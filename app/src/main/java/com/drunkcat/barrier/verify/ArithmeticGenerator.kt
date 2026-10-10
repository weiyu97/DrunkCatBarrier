package com.drunkcat.barrier.verify

import kotlin.random.Random

/**
 * 两位数加减乘除选择题生成器。
 *
 * - 加法：两位数 + 两位数
 * - 减法：两位数 - 一位数/两位数（保证非负）
 * - 乘法：两位数 × 一位数
 * - 除法：被除数 ÷ 除数（整除）
 * 每题 4 选 1，选项包含常见错误干扰项。
 */
object ArithmeticGenerator {

    fun generate(
        problemCount: Int = 5,
        random: Random = Random.Default
    ): Challenge.Arithmetic {
        val problems = (1..problemCount).map { generateOne(random) }
        return Challenge.Arithmetic(
            id = "ari-${System.currentTimeMillis()}",
            problems = problems
        )
    }

    private fun generateOne(random: Random): ArithmeticProblem {
        return when (random.nextInt(4)) {
            0 -> addition(random)
            1 -> subtraction(random)
            2 -> multiplication(random)
            else -> division(random)
        }
    }

    private fun addition(random: Random): ArithmeticProblem {
        val a = random.nextInt(10, 100)
        val b = random.nextInt(10, 100)
        val ans = a + b
        return build("$a + $b", ans, random)
    }

    private fun subtraction(random: Random): ArithmeticProblem {
        var a = random.nextInt(20, 100)
        var b = random.nextInt(10, 100)
        if (a < b) { val t = a; a = b; b = t }
        val ans = a - b
        return build("$a - $b", ans, random)
    }

    private fun multiplication(random: Random): ArithmeticProblem {
        val a = random.nextInt(10, 100)
        val b = random.nextInt(2, 10)
        val ans = a * b
        return build("$a × $b", ans, random)
    }

    private fun division(random: Random): ArithmeticProblem {
        val b = random.nextInt(2, 10)
        val ans = random.nextInt(2, 20)
        val a = b * ans
        return build("$a ÷ $b", ans, random)
    }

    private fun build(
        question: String,
        correct: Int,
        random: Random
    ): ArithmeticProblem {
        val opts = linkedSetOf(correct)
        var guard = 0
        while (opts.size < 4 && guard < 60) {
            guard++
            val delta = random.nextInt(1, 10)
            val cand = if (random.nextBoolean()) correct + delta else correct - delta
            if (cand >= 0 && cand != correct) opts.add(cand)
        }
        var d = 1
        while (opts.size < 4) {
            opts.add(correct + d)
            d++
        }
        val shuffled = opts.toList().shuffled(random)
        return ArithmeticProblem(
            question = question,
            options = shuffled,
            correctIndex = shuffled.indexOf(correct)
        )
    }
}

data class ArithmeticProblem(
    val question: String,
    val options: List<Int>,
    val correctIndex: Int,
    val timeLimitMs: Long = 15_000L
)
