package com.drunkcat.barrier.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScorerTest {

    private val now = 1_700_000_000_000L

    private fun hit(signal: RiskSignal, ageMs: Long = 0) =
        SignalHit(signal, now - ageMs)

    @Test
    fun `no signals scores zero`() {
        assertEquals(0, RiskScorer.score(emptyList(), now))
    }

    @Test
    fun `late night alone does not trigger`() {
        // 深夜 30 分 < 阈值 60
        val hits = listOf(hit(RiskSignal.LATE_NIGHT))
        assertEquals(30, RiskScorer.score(hits, now))
        assertFalse(RiskScorer.shouldTrigger(hits, now))
    }

    @Test
    fun `late night plus high risk app triggers`() {
        // 30 + 20 = 50 < 60，还不触发
        val hits = listOf(
            hit(RiskSignal.LATE_NIGHT),
            hit(RiskSignal.HIGH_RISK_APP)
        )
        assertEquals(50, RiskScorer.score(hits, now))
        assertFalse(RiskScorer.shouldTrigger(hits, now))
    }

    @Test
    fun `late night plus frequent switch triggers`() {
        // 30 + 25 = 55 < 60
        val hits = listOf(
            hit(RiskSignal.LATE_NIGHT),
            hit(RiskSignal.FREQUENT_APP_SWITCH)
        )
        assertEquals(55, RiskScorer.score(hits, now))
        assertFalse(RiskScorer.shouldTrigger(hits, now))
    }

    @Test
    fun `three signals trigger`() {
        // 30 + 25 + 20 = 75 >= 60
        val hits = listOf(
            hit(RiskSignal.LATE_NIGHT),
            hit(RiskSignal.FREQUENT_APP_SWITCH),
            hit(RiskSignal.HIGH_RISK_APP)
        )
        assertEquals(75, RiskScorer.score(hits, now))
        assertTrue(RiskScorer.shouldTrigger(hits, now))
    }

    @Test
    fun `duplicate signal counted once`() {
        // 同类型信号出现多次，只算一次权重
        val hits = listOf(
            hit(RiskSignal.LATE_NIGHT),
            hit(RiskSignal.LATE_NIGHT, ageMs = 1000),
            hit(RiskSignal.LATE_NIGHT, ageMs = 2000)
        )
        assertEquals(30, RiskScorer.score(hits, now))
    }

    @Test
    fun `expired signals ignored`() {
        // 超过 TTL 的信号不计入
        val hits = listOf(
            hit(RiskSignal.LATE_NIGHT, ageMs = RiskScorer.SIGNAL_TTL_MS + 1),
            hit(RiskSignal.FREQUENT_APP_SWITCH, ageMs = RiskScorer.SIGNAL_TTL_MS + 1)
        )
        assertEquals(0, RiskScorer.score(hits, now))
    }

    @Test
    fun `score capped at 100`() {
        val hits = RiskSignal.values().map { hit(it) }
        assertEquals(100, RiskScorer.score(hits, now))
    }

    @Test
    fun `late night detection`() {
        assertTrue(RiskScorer.isLateNight(22))
        assertTrue(RiskScorer.isLateNight(23))
        assertTrue(RiskScorer.isLateNight(0))
        assertTrue(RiskScorer.isLateNight(5))
        assertFalse(RiskScorer.isLateNight(6))
        assertFalse(RiskScorer.isLateNight(12))
        assertFalse(RiskScorer.isLateNight(21))
    }

    @Test
    fun `high risk package detection`() {
        assertTrue(RiskScorer.isHighRiskPackage("com.tencent.mm"))
        assertTrue(RiskScorer.isHighRiskPackage("com.android.contacts"))
        assertTrue(RiskScorer.isHighRiskPackage("com.eg.android.AlipayGphone"))
        assertTrue(RiskScorer.isHighRiskPackage("org.telegram.messenger"))
        assertFalse(RiskScorer.isHighRiskPackage("com.android.chrome"))
        assertFalse(RiskScorer.isHighRiskPackage("com.google.android.apps.maps"))
    }

    @Test
    fun `short video package detection`() {
        assertTrue(RiskScorer.isShortVideoPackage("com.ss.android.ugc.aweme"))   // 抖音
        assertTrue(RiskScorer.isShortVideoPackage("com.ss.android.ugc.aweme.lite"))
        assertTrue(RiskScorer.isShortVideoPackage("com.kuaishou.nebula"))         // 快手
        assertTrue(RiskScorer.isShortVideoPackage("tv.danmaku.bili"))            // B站
        assertFalse(RiskScorer.isShortVideoPackage("com.tencent.mm"))            // 微信主包不是短视频包
        assertFalse(RiskScorer.isShortVideoPackage("com.android.chrome"))
    }

    @Test
    fun `short video night alone triggers threshold`() {
        // 深夜刷短视频单条信号权重=60，应直接达阈值触发
        RiskMonitor.reset()
        RiskMonitor.onShortVideoLateNight(now)
        assertTrue(RiskScorer.shouldTrigger(RiskMonitor.snapshot(), now))
    }
}
