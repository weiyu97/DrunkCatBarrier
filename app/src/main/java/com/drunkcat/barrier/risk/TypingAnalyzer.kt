package com.drunkcat.barrier.risk

/**
 * 打字行为分析（纯逻辑，可单元测试）。
 *
 * 酒精会降低手部精细控制，表现为：
 * - 删除率升高（打错重打）
 * - 打字速度异常（忽快忽慢）
 *
 * 隐私：只统计**数量和频率**，不读取任何输入内容、
 * 不记录字符、不分析语义。
 *
 * 判定逻辑：
 * - 在滑动窗口内统计删除事件占比
 * - 删除率 > 40% 且样本足够 -> 判定为异常
 */
class TypingAnalyzer(
    private val windowMs: Long = 60_000L,
    private val minSamples: Int = 20,
    private val deletionRatioThreshold: Double = 0.4
) {

    private data class Event(val isDeletion: Boolean, val timestamp: Long)

    private val events = ArrayDeque<Event>()

    /**
     * 记录一次输入事件。
     * @param isDeletion 是否为删除/退格
     */
    fun onInput(isDeletion: Boolean, now: Long = System.currentTimeMillis()) {
        events.addLast(Event(isDeletion, now))
        // 清理窗口外
        while (events.isNotEmpty() && now - events.first().timestamp > windowMs) {
            events.removeFirst()
        }
    }

    /**
     * 当前删除率（0.0 - 1.0）。样本不足返回 -1。
     */
    fun deletionRatio(now: Long = System.currentTimeMillis()): Double {
        prune(now)
        if (events.size < minSamples) return -1.0
        val deletions = events.count { it.isDeletion }
        return deletions.toDouble() / events.size
    }

    /**
     * 是否判定为打字异常。
     */
    fun isAnomalous(now: Long = System.currentTimeMillis()): Boolean {
        val ratio = deletionRatio(now)
        return ratio >= 0 && ratio > deletionRatioThreshold
    }

    fun reset() {
        events.clear()
    }

    private fun prune(now: Long) {
        while (events.isNotEmpty() && now - events.first().timestamp > windowMs) {
            events.removeFirst()
        }
    }
}
