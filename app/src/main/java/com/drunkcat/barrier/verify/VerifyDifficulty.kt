package com.drunkcat.barrier.verify

/**
 * 验证难度分级。
 *
 * 根据当前风险分动态调整，避免「白天随手开个 App 也要做完整试炼」的摩擦。
 *
 * - 低风险（白天、分数低）：1 种题型，门槛低
 * - 标准：2 种题型，平衡
 * - 高风险（深夜 + 高分）：2 种题型 + 更多题，提高通过门槛
 */
enum class VerifyDifficulty(
    val typeCount: Int,
    val problemCount: Int,
    val label: String
) {
    EASY(1, 4, "轻度验证"),
    NORMAL(2, 5, "标准验证"),
    HARD(2, 6, "严格验证");

    companion object {
        /**
         * 根据风险分选择难度。
         *
         * @param riskScore 0-100
         */
        fun fromRisk(riskScore: Int): VerifyDifficulty = when {
            riskScore < 30 -> EASY
            riskScore < 60 -> NORMAL
            else -> HARD
        }
    }
}
