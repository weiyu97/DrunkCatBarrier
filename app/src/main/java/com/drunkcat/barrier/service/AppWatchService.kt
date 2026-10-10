package com.drunkcat.barrier.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.drunkcat.barrier.data.Prefs
import com.drunkcat.barrier.risk.RiskMonitor
import com.drunkcat.barrier.risk.ShakeDetector
import com.drunkcat.barrier.ui.VerifyActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    private var lockedPackages: Set<String> = emptySet()
    private var whitelistPackages: Set<String> = emptySet()
    private var protectionActive = false
    private var autoProtectEnabled = true
    @Volatile private var cooldownUntilCache: Long = 0L

    // 已验证通过、处于临时窗口期的应用
    @Volatile private var tempAllowedPackage: String? = null
    @Volatile private var tempAllowUntil: Long = 0L

    // 防抖：同一应用短时间内不重复弹窗
    @Volatile private var lastBlockedPackage: String? = null
    @Volatile private var lastBlockedAt: Long = 0L

    // 上次触发自动保护的时间，防止重复触发
    @Volatile private var lastAutoTriggerAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
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
            prefs.autoProtectFlow.collect { autoProtectEnabled = it }
        }
        // 定期刷新冷却时间（冷却由 VerifyActivity 设置）
        scope.launch {
            while (true) {
                cooldownUntilCache = prefs.getCooldownUntil()
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
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "com.android.systemui") return

        // ---- 1. 持续上报信号（无论保护是否开启）----
        RiskMonitor.onAppSwitched(pkg)
        maybeAutoTrigger()

        // ---- 2. 拦截逻辑（仅保护开启时）----
        if (!protectionActive) return
        if (pkg in whitelistPackages) return
        if (pkg !in lockedPackages) return

        val now = System.currentTimeMillis()
        if (pkg == tempAllowedPackage && now < tempAllowUntil) return
        if (pkg == lastBlockedPackage && now - lastBlockedAt < 1500) return

        // 冷却期内直接拦截，不弹验证页
        val cooldownUntil = cooldownUntilCache
        if (now < cooldownUntil) {
            // 弹回桌面
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(home)
            return
        }

        lastBlockedPackage = pkg
        lastBlockedAt = now

        // 弹出验证界面
        val intent = Intent(this, VerifyActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(VerifyActivity.EXTRA_TARGET_PACKAGE, pkg)
        }
        startActivity(intent)
    }

    /**
     * 检查风险分，超阈值则进入 30 秒预警。
     *
     * 预警期间用户可点「我没醉」取消；
     * 30 秒无操作则自动开启保护。
     */
    private fun maybeAutoTrigger() {
        if (!autoProtectEnabled) return
        if (protectionActive) return
        if (WarningState.active) return

        val now = System.currentTimeMillis()
        // 5 分钟内不重复触发
        if (now - lastAutoTriggerAt < 5 * 60 * 1000L) return

        if (RiskMonitor.shouldTrigger(now)) {
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
                    val current = prefs.getLockedPackages()
                    if (current.isEmpty()) {
                        val installed = queryInstalledPackages()
                        val defaults = com.drunkcat.barrier.data.DefaultLockList
                            .pickDefault(installed)
                        if (defaults.isNotEmpty()) {
                            prefs.setLockedPackages(defaults)
                        }
                    }
                    prefs.setProtectionActive(true)
                    // 默认锁到次日 8:00
                    prefs.setProtectionUntil(
                        com.drunkcat.barrier.data.ContractTime.nextMorning8()
                    )
                    AutoProtectionNotifier.notify(this@AppWatchService)
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
        fun grantTempAccess(pkg: String, durationMs: Long) {
            instance?.let {
                it.tempAllowedPackage = pkg
                it.tempAllowUntil = System.currentTimeMillis() + durationMs
            }
        }

        @Volatile private var instance: AppWatchService? = null

        fun register(s: AppWatchService) { instance = s }
        fun unregister() { instance = null }
    }

    init {
        register(this)
    }
}
