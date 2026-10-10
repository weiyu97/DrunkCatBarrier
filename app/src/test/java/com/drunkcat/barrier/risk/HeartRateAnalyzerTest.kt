package com.drunkcat.barrier.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateAnalyzerTest {

    @Test
    fun `no baseline before enough samples`() {
        val a = HeartRateAnalyzer()
        repeat(9) { a.addReading(70) }
        assertEquals(0, a.currentBaseline())
    }

    @Test
    fun `baseline established after sample size`() {
        val a = HeartRateAnalyzer()
        repeat(10) { a.addReading(70) }
        assertEquals(70, a.currentBaseline())
    }

    @Test
    fun `stable readings do not trigger`() {
        val a = HeartRateAnalyzer()
        repeat(20) { assertFalse(a.addReading(70)) }
    }

    @Test
    fun `transient spike does not trigger`() {
        val a = HeartRateAnalyzer()
        repeat(10) { a.addReading(70) }
        // 单次飙高
        assertFalse(a.addReading(120))
    }

    @Test
    fun `sustained elevation triggers`() {
        val a = HeartRateAnalyzer()
        repeat(10) { a.addReading(70) }
        // 连续 5 次超过基线 30%（70 * 1.3 = 91）
        var triggered = false
        repeat(5) {
            if (a.addReading(100)) triggered = true
        }
        assertTrue("持续偏高应触发", triggered)
    }

    @Test
    fun `elevation just below threshold does not trigger`() {
        val a = HeartRateAnalyzer()
        repeat(10) { a.addReading(70) }
        // 90 < 91，未达 30% 阈值
        repeat(6) { assertFalse(a.addReading(90)) }
    }

    @Test
    fun `out of range readings ignored`() {
        val a = HeartRateAnalyzer()
        repeat(10) { a.addReading(70) }
        assertFalse(a.addReading(300))
        assertFalse(a.addReading(10))
        assertEquals(70, a.currentBaseline())
    }

    @Test
    fun `reset clears state`() {
        val a = HeartRateAnalyzer()
        repeat(10) { a.addReading(70) }
        a.reset()
        assertEquals(0, a.currentBaseline())
    }

    @Test
    fun `higher baseline reduces false positives`() {
        // 基线 100 的运动员，心率 120 不应触发（未超 30%）
        val a = HeartRateAnalyzer()
        repeat(10) { a.addReading(100) }
        repeat(6) { assertFalse(a.addReading(120)) }
    }
}
