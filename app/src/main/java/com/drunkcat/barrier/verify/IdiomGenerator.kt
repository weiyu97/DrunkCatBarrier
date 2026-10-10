package com.drunkcat.barrier.verify

import kotlin.random.Random

/**
 * 成语填空选择题生成器。
 *
 * 从 IdiomBank 随机抽成语，挖去一个字，选项为 4 个候选字。
 */
object IdiomGenerator {

    fun generate(
        problemCount: Int = 5,
        random: Random = Random.Default
    ): Challenge.Idiom {
        val pool = IdiomBank.ALL.shuffled(random).take(problemCount)
        val problems = pool.map { generateOne(it, random) }
        return Challenge.Idiom(
            id = "idiom-${System.currentTimeMillis()}",
            problems = problems
        )
    }

    private fun generateOne(idiom: String, random: Random): IdiomProblem {
        val blankIndex = random.nextInt(idiom.length)
        val answer = idiom[blankIndex]
        val question = idiom.mapIndexed { i, c ->
            if (i == blankIndex) "○" else c.toString()
        }.joinToString("")

        // 干扰字：从其他成语中取不重复的字
        val distractors = linkedSetOf<Char>()
        var guard = 0
        while (distractors.size < 3 && guard < 100) {
            guard++
            val other = IdiomBank.ALL.random(random)
            val c = other.random(random)
            if (c != answer) distractors.add(c)
        }
        // 兜底
        val fallback = "天地人和金木水火土风云雨雪"
        var idx = 0
        while (distractors.size < 3 && idx < fallback.length) {
            val c = fallback[idx]
            if (c != answer) distractors.add(c)
            idx++
        }

        val options = (listOf(answer) + distractors).shuffled(random)
        return IdiomProblem(
            question = question,
            options = options,
            correctIndex = options.indexOf(answer),
            original = idiom
        )
    }
}

data class IdiomProblem(
    val question: String,
    val options: List<Char>,
    val correctIndex: Int,
    val original: String,
    val timeLimitMs: Long = 15_000L
)
