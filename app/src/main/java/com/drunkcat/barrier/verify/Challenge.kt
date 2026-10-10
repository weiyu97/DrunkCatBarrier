package com.drunkcat.barrier.verify

/**
 * 认知验证挑战模型。
 */
sealed class Challenge {
    abstract val id: String

    /** 两位数加减乘除选择题。 */
    data class Arithmetic(
        override val id: String,
        val problems: List<ArithmeticProblem>
    ) : Challenge()

    /** 成语填空选择题。 */
    data class Idiom(
        override val id: String,
        val problems: List<IdiomProblem>
    ) : Challenge()

    /** 点阵记忆：3x3 中点亮若干位置，用户按顺序还原。 */
    data class DotMemory(
        override val id: String,
        val pattern: List<Int>,
        val displayDurationMs: Long = 1500L
    ) : Challenge()
}
