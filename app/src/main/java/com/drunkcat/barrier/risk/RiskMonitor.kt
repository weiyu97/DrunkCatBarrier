package com.drunkcat.barrier.risk

import java.util.Calendar

/**
 * 运行时风险监测状态。
 *
 * 累计所有已触发的信号，供 RiskScorer 计算分数。
 * 线程安全：所有写操作在锁内完成。
 */
object RiskMonitor {

    private val lock = Any()
    private val hits = mutableListOf<SignalHit>()

    /** 最近的若干次前台包切换时间戳（用于判定频繁切换） */
    private val recentSwitches = ArrayDeque<Long>()

    /** 频繁切换的时间窗口 */
    private const val SWITCH_WINDOW_MS = 10 * 60 * 1000L  // 10 分钟

    /** 频繁切换的判定阈值 */
    private const val SWITCH_COUNT_THRESHOLD = 15

    /**
     * 记录一次前台应用切换。
     */
    fun onAppSwitched(packageName: String, now: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            recentSwitches.addLast(now)
            // 清理窗口外的记录
            while (recentSwitches.isNotEmpty() && now - recentSwitches.first() > SWITCH_WINDOW_MS) {
                recentSwitches.removeFirst()
            }
            // 频繁切换
            if (recentSwitches.size >= SWITCH_COUNT_THRESHOLD) {
                addHit(RiskSignal.FREQUENT_APP_SWITCH, now, "10 分钟内切换 ${recentSwitches.size} 次")
            }
            // 高风险应用
            if (RiskScorer.isHighRiskPackage(packageName)) {
                addHit(RiskSignal.HIGH_RISK_APP, now, packageName)
            }
        }
    }

    /**
     * 定期调用，刷新时间信号。
     */
    fun refreshTimeSignal(now: Long = System.currentTimeMillis()) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (RiskScorer.isLateNight(hour)) {
            synchronized(lock) {
                addHit(RiskSignal.LATE_NIGHT, now, "$hour 时")
            }
        }
    }

    /**
     * 记录一次「手机持续晃动」信号。
     */
    fun onDeviceShaking(now: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            addHit(RiskSignal.DEVICE_SHAKING, now, "持续晃动")
        }
    }

    /**
     * 记录一次打字异常信号。
     */
    fun onTypingAnomaly(detail: String, now: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            addHit(RiskSignal.TYPING_ANOMALY, now, detail)
        }
    }

    /**
     * 获取当前风险分。
     */
    fun currentScore(now: Long = System.currentTimeMillis()): Int {
        synchronized(lock) {
            return RiskScorer.score(hits.toList(), now)
        }
    }

    /**
     * 是否应触发保护。
     *
     * @param threshold 触发阈值（分）。默认 60；可通过 ProtectionLevel 三档配置。
     */
    fun shouldTrigger(now: Long = System.currentTimeMillis(), threshold: Int = RiskScorer.THRESHOLD): Boolean {
        synchronized(lock) {
            val score = RiskScorer.score(hits.toList(), now)
            return score >= threshold
        }
    }

    /**
     * 获取当前所有活跃的信号（供复盘页展示）。
     * 返回快照，不随内部状态变化。
     */
    fun snapshot(): List<SignalHit> {
        synchronized(lock) {
            return hits.toList()
        }
    }

    /**
     * 获取某类信号的最新一条（供复盘页按类型查询）。
     */
    fun latest(signal: RiskSignal): SignalHit? {
        synchronized(lock) {
            return hits.firstOrNull { it.signal == signal }
        }
    }

    /**
     * 清空所有信号。用户明确表示清醒、或保护已解除时调用。
     */
    fun reset() {
        synchronized(lock) {
            hits.clear()
            recentSwitches.clear()
        }
    }

    private fun addHit(signal: RiskSignal, now: Long, detail: String) {
        // 同一信号只保留最新一条，避免堆积
        hits.removeAll { it.signal == signal }
        hits.add(SignalHit(signal, now, detail))
        // 清理过期
        hits.removeAll { now - it.timestamp > RiskScorer.SIGNAL_TTL_MS }
    }
}
