package com.drunkcat.barrier.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingAnalyzerTest {

    private val t0 = 1_700_000_000_000L

    @Test
    fun `insufficient samples returns -1 ratio`() {
        val a = TypingAnalyzer()
        repeat(10) { a.onInput(it % 2 == 0, t0 + it) }
        assertEquals(-1.0, a.deletionRatio(t0 + 10), 0.001)
    }

    @Test
    fun `normal typing is not anomalous`() {
        val a = TypingAnalyzer()
        // 20 次输入，只有 2 次删除（10%）
        repeat(18) { a.onInput(false, t0 + it) }
        repeat(2) { a.onInput(true, t0 + 20 + it) }
        assertFalse(a.isAnomalous(t0 + 30))
    }

    @Test
    fun `high deletion ratio is anomalous`() {
        val a = TypingAnalyzer()
        // 20 次输入，10 次删除（50%）
        repeat(10) { a.onInput(false, t0 + it) }
        repeat(10) { a.onInput(true, t0 + 10 + it) }
        assertTrue(a.isAnomalous(t0 + 30))
    }

    @Test
    fun `ratio just below threshold is not anomalous`() {
        val a = TypingAnalyzer()
        // 20 次，7 次删除（35% < 40%）
        repeat(13) { a.onInput(false, t0 + it) }
        repeat(7) { a.onInput(true, t0 + 13 + it) }
        assertFalse(a.isAnomalous(t0 + 30))
    }

    @Test
    fun `old events are pruned`() {
        val a = TypingAnalyzer(windowMs = 1000L)
        repeat(20) { a.onInput(true, t0 + it) }
        // 时间跳到窗口外
        val later = t0 + 2000
        assertEquals(-1.0, a.deletionRatio(later), 0.001)
    }

    @Test
    fun `reset clears state`() {
        val a = TypingAnalyzer()
        repeat(20) { a.onInput(true, t0 + it) }
        a.reset()
        assertEquals(-1.0, a.deletionRatio(t0 + 30), 0.001)
    }
}
