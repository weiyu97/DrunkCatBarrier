package com.drunkcat.barrier.verify

import kotlin.random.Random

/**
 * 认知验证器：负责生成挑战、校验作答、给出冷却时间。
 *
 * 支持的挑战类型：
 * - 两位数加减乘除选择题（Arithmetic）
 * - 成语填空选择题（Idiom）
 * - 点阵记忆（DotMemory）
 *
 * 每次验证随机抽 2 种组合（由上层决定顺序）。
 */
class CognitiveVerifier(private val random: Random = Random.Default) {

    fun newArithmeticChallenge(problemCount: Int = 5): Challenge.Arithmetic =
        ArithmeticGenerator.generate(problemCount, random)

    fun newIdiomChallenge(problemCount: Int = 5): Challenge.Idiom =
        IdiomGenerator.generate(problemCount, random)

    fun newDotMemoryChallenge(): Challenge.DotMemory =
        DotMemoryGenerator.generate(random)

    // ---------- 判定 ----------

    /**
     * 选择题通用判定：答错 2 题立即失败，答对 >= 总题数-1 通过。
     */
    fun verifyChoice(
        answers: List<Int>,
        correctIndices: List<Int>
    ): ChoiceOutcome {
        if (answers.size != correctIndices.size) {
            return ChoiceOutcome(false, "作答数量不匹配")
        }
        var wrong = 0
        var correct = 0
        answers.forEachIndexed { i, a ->
            if (a == correctIndices[i]) correct++ else {
                wrong++
                if (wrong >= 2) return ChoiceOutcome(false, "答错 2 题")
            }
        }
        val need = correctIndices.size - 1
        val passed = correct >= need
        return ChoiceOutcome(passed, if (passed) "" else "答对不足 $need 题")
    }

    /**
     * 点阵验证：只比对点击的格子集合，不要求顺序。
     * 只要点对全部目标格子即可通过。
     */
    fun verifyDotMemory(challenge: Challenge.DotMemory, tapped: List<Int>): Boolean =
        tapped.toSet() == challenge.pattern.toSet() && tapped.size == challenge.pattern.size

    fun cooldownFor(failureCount: Int): Long = CooldownPolicy.cooldownFor(failureCount)
}

data class ChoiceOutcome(
    val passed: Boolean,
    val reason: String
)
