package com.drunkcat.barrier.verify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 点阵验证：确认「只看集合、不看顺序」。
 */
class DotMemoryVerifyTest {

    private val verifier = CognitiveVerifier()

    private fun challenge(pattern: List<Int>) = Challenge.DotMemory(
        id = "test",
        pattern = pattern
    )

    @Test
    fun `exact order passes`() {
        val c = challenge(listOf(0, 1, 2, 3))
        assertTrue(verifier.verifyDotMemory(c, listOf(0, 1, 2, 3)))
    }

    @Test
    fun `reversed order passes`() {
        val c = challenge(listOf(0, 1, 2, 3))
        assertTrue(verifier.verifyDotMemory(c, listOf(3, 2, 1, 0)))
    }

    @Test
    fun `arbitrary order passes`() {
        val c = challenge(listOf(0, 4, 6, 8))
        assertTrue(verifier.verifyDotMemory(c, listOf(6, 0, 8, 4)))
    }

    @Test
    fun `wrong cell fails`() {
        val c = challenge(listOf(0, 1, 2, 3))
        assertFalse(verifier.verifyDotMemory(c, listOf(0, 1, 2, 4)))
    }

    @Test
    fun `missing cell fails`() {
        val c = challenge(listOf(0, 1, 2, 3))
        assertFalse(verifier.verifyDotMemory(c, listOf(0, 1, 2)))
    }

    @Test
    fun `extra cell fails`() {
        val c = challenge(listOf(0, 1, 2, 3))
        assertFalse(verifier.verifyDotMemory(c, listOf(0, 1, 2, 3, 4)))
    }
}
