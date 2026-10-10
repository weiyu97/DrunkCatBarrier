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
}
