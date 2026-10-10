package com.drunkcat.barrier.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdiomGeneratorTest {

    @Test
    fun `each problem has exactly 4 unique options`() {
        repeat(50) { seed ->
            val c = IdiomGenerator.generate(5, Random(seed))
            c.problems.forEach { p ->
                assertEquals(4, p.options.size)
                assertEquals(4, p.options.toSet().size)
            }
        }
    }

    @Test
    fun `question contains blank marker`() {
        val c = IdiomGenerator.generate(5, Random(1))
        c.problems.forEach { p ->
            assertTrue(p.question.contains("○"))
        }
    }

    @Test
    fun `answer matches the original idiom character`() {
        repeat(100) { seed ->
            val c = IdiomGenerator.generate(5, Random(seed))
            c.problems.forEach { p ->
                val answer = p.options[p.correctIndex]
                assertTrue(p.original.contains(answer))
            }
        }
    }

    @Test
    fun `original idiom length preserved in question`() {
        val c = IdiomGenerator.generate(5, Random(2))
        c.problems.forEach { p ->
            assertEquals(p.original.length, p.question.length)
        }
    }
}
