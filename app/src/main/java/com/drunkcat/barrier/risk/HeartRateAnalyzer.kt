package com.drunkcat.barrier.risk

/**
 * 心率异常分析（纯逻辑，可单元测试）。
 *
 * 判断逻辑：
 * 1. 用前 N 次读数建立静息基线
 * 2. 当最近 5 次读数均高于基线 30% 以上，判定为持续异常
 *
 * 为什么用「持续」而非「瞬时」：
 * 运动、紧张、喝咖啡都会瞬时拉高心率，
 * 只有持续偏高才可能是酒精代谢导致。
 *
 * 注意：心率不能直接判断醉酒，仅作为风险评分的弱信号。
 */
class HeartRateAnalyzer(
    private val baselineSampleSize: Int = 10,
    private val consecutiveThreshold: Int = 5,
    private val elevationRatio: Double = 1.3
) {

    private val recent = ArrayDeque<Int>()
    private var baseline: Int = 0

    /** 记录一次心率读数，返回是否应触发异常信号 */
    fun addReading(hr: Int, now: Long = System.currentTimeMillis()): Boolean {
        if (hr !in 30..220) return false

        recent.addLast(hr)
        if (recent.size > 20) recent.removeFirst()

        // 建立基线
        if (baseline == 0 && recent.size >= baselineSampleSize) {
            baseline = recent.take(baselineSampleSize).average().toInt()
            return false
        }

        if (baseline <= 0) return false
        if (recent.size < consecutiveThreshold) return false

        val tail = recent.toList().takeLast(consecutiveThreshold)
        return tail.all { it > baseline * elevationRatio }
    }

    /** 当前基线值（0 表示尚未建立） */
    fun currentBaseline(): Int = baseline

    /** 重置（停止监测时调用） */
    fun reset() {
        recent.clear()
        baseline = 0
    }
}
