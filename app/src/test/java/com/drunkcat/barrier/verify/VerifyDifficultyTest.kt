package com.drunkcat.barrier.verify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyDifficultyTest {

    @Test
    fun `low risk is easy`() {
        assertEquals(VerifyDifficulty.EASY, VerifyDifficulty.fromRisk(0))
        assertEquals(VerifyDifficulty.EASY, VerifyDifficulty.fromRisk(29))
    }

    @Test
    fun `medium risk is normal`() {
        assertEquals(VerifyDifficulty.NORMAL, VerifyDifficulty.fromRisk(30))
        assertEquals(VerifyDifficulty.NORMAL, VerifyDifficulty.fromRisk(59))
    }

    @Test
    fun `high risk is hard`() {
        assertEquals(VerifyDifficulty.HARD, VerifyDifficulty.fromRisk(60))
        assertEquals(VerifyDifficulty.HARD, VerifyDifficulty.fromRisk(100))
    }

    @Test
    fun `difficulty escalates with risk`() {
        assertTrue(VerifyDifficulty.EASY.typeCount <= VerifyDifficulty.NORMAL.typeCount)
        assertTrue(VerifyDifficulty.NORMAL.problemCount <= VerifyDifficulty.HARD.problemCount)
    }

    @Test
    fun `easy only needs one challenge`() {
        assertEquals(1, VerifyDifficulty.EASY.typeCount)
    }

    @Test
    fun `hard has more problems than easy`() {
        assertTrue(VerifyDifficulty.HARD.problemCount > VerifyDifficulty.EASY.problemCount)
    }
}
