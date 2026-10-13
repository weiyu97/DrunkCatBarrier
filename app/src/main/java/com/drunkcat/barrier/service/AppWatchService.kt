package com.drunkcat.barrier.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.drunkcat.barrier.data.Prefs
import com.drunkcat.barrier.risk.RiskMonitor
import com.drunkcat.barrier.risk.ShakeDetector
import com.drunkcat.barrier.risk.TypingAnalyzer
import com.drunkcat.barrier.ui.LockOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 前台应用监听服务。
 *
 * 两条职责：
 * 1. 自动监测：无论保护是否开启，持续上报前台应用切换与时间信号，
 *    当风险分超阈值时自动开启保护（无需用户操作）。
 * 2. 拦截：保护开启后，打开被封印的应用时弹出清醒试炼。
 *
 * 隐私：不读取任何输入内容，不联网，仅检测前台包名与系统时间。
 */
class AppWatchService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var prefs: Prefs

    private var shakeDetector: ShakeDetector? = null
    private val typingAnalyzer = TypingAnalyzer()

    private var lockedPackages: Set<String> = emptySet()
    private var whitelistPackages: Set<String> = emptySet()
    private var protectionActive = false
    private var autoProtectEnabled = true
    @Volatile private var cooldownUntilCache: Long = 0L
    @Volatile private var skipAutoUntil: Long = 0L
    @Volatile private var protectionUntilCache: Long = 0L

    // 已验证通过、处于临时窗口期的应用
    @Volatile private var tempAllowedPackage: String? = null
    @Volatile private var tempAllowUntil: Long = 0L

    // 全局放行：一次验证通过后，短时间内所有被锁应用免验证
    @Volatile private var globalAllowUntil: Long = 0L

    // 紧急呼叫后的全局临时放行窗口（避免锁屏反复拦截拨号界面）
    @Volatile private var emergencyUntil: Long = 0L

    // 防抖：同一应用短时间内不重复弹窗
    @Volatile private var lastBlockedPackage: String? = null
    @Volatile private var lastBlockedAt: Long = 0L

    // 上次触发自动保护的时间，防止重复触发
    @Volatile private var lastAutoTriggerAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("DrunkCat", "onServiceConnected: bound OK")
        prefs = Prefs(this)
        scope.launch {
            prefs.lockedFlow().collect { lockedPackages = it.toSet() }
        }
        scope.launch {
            prefs.whitelistFlow().collect { whitelistPackages = it.toSet() }
        }
        scope.launch {
            prefs.protectionFlow.collect { protectionActive = it }
        }
        scope.launch {
            prefs.protectionUntilFlow.collect { protectionUntilCache = it }
        }
        scope.launch {
            prefs.autoProtectFlow.collect { autoProtectEnabled = it }
        }
        // 定期刷新冷却时间与暂停状态
        scope.launch {
            while (true) {
                cooldownUntilCache = prefs.getCooldownUntil()
                skipAutoUntil = prefs.getSkipAutoUntil()
                kotlinx.coroutines.delay(5_000)
            }
        }
        // 启动步态检测
        shakeDetector = ShakeDetector(this).also { it.start() }
        // 定期刷新时间信号并检查风险
        scope.launch {
            while (true) {
                delay(60_000)
                RiskMonitor.refreshTimeSignal()
                maybeAutoTrigger()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // ---- 打字行为分析（仅统计，不读内容）----
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            // removedCount / addedCount 是系统提供的计数，不涉及字符内容
            val removed = event.removedCount
            val added = event.addedCount
            if (removed > 0) {
                repeat(removed) { typingAnalyzer.onInput(true) }
            }
            if (added > 0) {
                repeat(added) { typingAnalyzer.onInput(false) }
            }
            // 达到阈值则上报
            if (typingAnalyzer.isAnomalous()) {
                val ratio = typingAnalyzer.deletionRatio()
                RiskMonitor.onTypingAnomaly(
                    "删除率 ${(ratio * 100).toInt()}%"
                )
                typingAnalyzer.reset()  // 上报后清零，避免重复
            }
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "com.android.systemui") return

        // ---- 1. 持续上报信号（无论保护是否开启）----
        RiskMonitor.onAppSwitched(pkg)
        maybeAutoTrigger()

        android.util.Log.d(
            "DrunkCat",
            "watch: pkg=$pkg active=$protectionActive locked=${lockedPackages.size} wl=${whitelistPackages.size}"
        )

        // ---- 2. 拦截逻辑（仅保护开启时）----
        // 结界守护：保护开启后，只要前台不是本应用（且非白名单/临时放行），
        // 就把软锁屏拉回前台，把用户锁在结界内。
        if (!protectionActive) {
            // 保护已关闭，确保残留的悬浮锁屏层被撤掉
            if (LockOverlay.isShowing()) LockOverlay.remove(this)
            return
        }
        // 白名单应用允许自由使用
        if (pkg in whitelistPackages) return

        val now = System.currentTimeMillis()
        // 全局放行期内直接通过
        if (now < globalAllowUntil) return
        if (pkg == tempAllowedPackage && now < tempAllowUntil) return
        // 紧急呼叫后的临时放行窗口
        if (now < emergencyUntil) return
        // 防抖：同一包名短时间内不重复拉起锁屏
        if (pkg == lastBlockedPackage && now - lastBlockedAt < 1500) return

        lastBlockedPackage = pkg
        lastBlockedAt = now

        // 拉起悬浮锁屏层（结界封锁中），把用户锁回结界。
        // 用 TYPE_ACCESSIBILITY_OVERLAY 绘制，不受 MIUI 后台弹窗限制，
        // 且无法被返回手势关闭。
        android.util.Log.d("DrunkCat", "watch: LOCKSCREEN for $pkg")
        LockOverlay.show(this, pkg, protectionUntilCache)
    }

    /**
     * 检查风险分，超阈值则进入 30 秒预警。
     *
     * 预警期间用户可点「我没醉」取消；
     * 30 秒无操作则自动开启保护。
     *
     * 阈值由 ProtectionLevel 三档决定（LITE=99, STANDARD=60, STRICT=40）。
     */
    private fun maybeAutoTrigger() {
        if (!autoProtectEnabled) return
        if (protectionActive) return
        if (WarningState.active) return

        val now = System.currentTimeMillis()
        // 「今晚不喝」暂停期内不触发
        if (skipAutoUntil > 0L && now < skipAutoUntil) return
        // 5 分钟内不重复触发
        if (now - lastAutoTriggerAt < 5 * 60 * 1000L) return

        // 读取保护级别，决定触发阈值
        val levelName = runBlocking { runCatching { prefs.getProtectionLevel() }.getOrDefault("STANDARD") }
        val threshold = runCatching {
            com.drunkcat.barrier.data.ProtectionLevel.valueOf(levelName).threshold
        }.getOrDefault(60)

        if (RiskMonitor.shouldTrigger(now, threshold)) {
            lastAutoTriggerAt = now

            // 进入预警状态
            WarningState.start(now)
            WarningState.onCancel = {
                // 用户表示清醒：清空风险信号
                RiskMonitor.reset()
            }
            WarningState.onTimeout = {
                scope.launch {
                    // 若用户从未配置锁定名单，套用默认清单，
                    // 避免「保护中却拦不住任何东西」
                    var defaultAppliedCount = 0
                    val current = prefs.getLockedPackages()
                    if (current.isEmpty()) {
                        val installed = queryInstalledPackages()
                        val defaults = com.drunkcat.barrier.data.DefaultLockList
                            .pickDefault(installed)
                        if (defaults.isNotEmpty()) {
                            prefs.setLockedPackages(defaults)
                            defaultAppliedCount = defaults.size
                        }
                    }
                    prefs.setProtectionActive(true)
                    // 默认锁到次日 8:00
                    prefs.setProtectionUntil(
                        com.drunkcat.barrier.data.ContractTime.nextMorning8()
                    )
                    AutoProtectionNotifier.notify(this@AppWatchService, defaultAppliedCount)
                }
            }

            // 弹预警通知
            RiskWarningNotifier.notify(this@AppWatchService)
        }
    }

    /**
     * 查询所有已安装应用的包名（用于自动套用默认锁定清单）。
     */
    private fun queryInstalledPackages(): List<String> {
        return try {
            packageManager.getInstalledApplications(0).map { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector?.stop()
        scope.cancel()
    }

    companion object {

        /**
         * 验证通过后给予临时使用权限。
         *
         * @param pkg 目标应用
         * @param durationMs 该应用的临时窗口
         * @param globalMs 全局放行时长（期间所有被锁应用免验证）
         */
        fun grantTempAccess(pkg: String, durationMs: Long, globalMs: Long = 0L) {
            instance?.let {
                val now = System.currentTimeMillis()
                it.tempAllowedPackage = pkg
                it.tempAllowUntil = now + durationMs
                if (globalMs > 0L) {
                    it.globalAllowUntil = now + globalMs
                }
            }
        }

        /**
         * 紧急呼叫时调用：开启一段全局临时放行，
         * 避免软锁屏反复拦截拨号界面导致无法拨出。
         */
        fun grantEmergencyAccess(durationMs: Long = 60_000L) {
            instance?.let { it.emergencyUntil = System.currentTimeMillis() + durationMs }
        }

        /** 关闭悬浮锁屏层（保护停止 / 验证通过时调用），并作废挂起状态，之后不再恢复 */
        fun dismissOverlay() {
            instance?.let { LockOverlay.remove(it) }
            LockOverlay.clearPending()
        }

        /**
         * 恢复悬浮锁屏层。验证 Activity 关闭但并未解锁时调用，
         * 需要服务实例提供无障碍上下文。
         */
        fun restoreOverlay() {
            val s = instance ?: return
            if (!s.protectionActive) return
            LockOverlay.restore(s)
        }

        @Volatile private var instance: AppWatchService? = null

        fun register(s: AppWatchService) { instance = s }
        fun unregister() { instance = null }
    }

    init {
        register(this)
    }
}
