package com.drunkcat.barrier.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ArithmeticGeneratorTest {

    @Test
    fun `each problem has exactly 4 unique options`() {
        repeat(50) { seed ->
            val c = ArithmeticGenerator.generate(5, Random(seed))
            c.problems.forEach { p ->
                assertEquals(4, p.options.size)
                assertEquals(4, p.options.toSet().size)
            }
        }
    }

    @Test
    fun `correct index points to a valid option`() {
        repeat(50) { seed ->
            val c = ArithmeticGenerator.generate(5, Random(seed))
            c.problems.forEach { p ->
                assertTrue(p.correctIndex in 0..3)
                assertTrue(p.options[p.correctIndex] >= 0)
            }
        }
    }

    @Test
    fun `addition answer is correct`() {
        // 遍历足够多的题，遇到加法就校验
        repeat(200) { seed ->
            val c = ArithmeticGenerator.generate(10, Random(seed))
            c.problems.forEach { p ->
                if (p.question.contains("+")) {
                    val parts = p.question.split(" + ")
                    val expected = parts[0].trim().toInt() + parts[1].trim().toInt()
                    assertEquals(expected, p.options[p.correctIndex])
                }
            }
        }
    }

    @Test
    fun `subtraction result is non negative`() {
        repeat(200) { seed ->
            val c = ArithmeticGenerator.generate(10, Random(seed))
            c.problems.forEach { p ->
                assertTrue(p.options[p.correctIndex] >= 0)
            }
        }
    }

    @Test
    fun `generates requested count`() {
        val c = ArithmeticGenerator.generate(5)
        assertEquals(5, c.problems.size)
    }

    @Test
    fun `all operands are single digit`() {
        // 加/减/乘的操作数都是个位数；
        // 除法被除数是两位数（如 81÷9），除数是个位数。
        repeat(200) { seed ->
            val c = ArithmeticGenerator.generate(20, Random(seed))
            c.problems.forEach { p ->
                val nums = p.question.split(Regex("[^0-9]+"))
                    .filter { it.isNotBlank() }
                    .map { it.toInt() }
                if (p.question.contains("÷")) {
                    // 除法：除数（第二个数）必须是 1-9
                    assertTrue(
                        "除数 ${nums[1]} 应为个位数（题目 ${p.question}）",
                        nums[1] in 1..9
                    )
                } else {
                    // 加减乘：操作数都是个位数
                    nums.forEach { n ->
                        assertTrue(
                            "操作数 $n 应为个位数（题目 ${p.question}）",
                            n in 0..9
                        )
                    }
                }
            }
        }
    }
}
