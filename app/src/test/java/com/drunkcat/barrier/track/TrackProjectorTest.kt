package com.drunkcat.barrier.track

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackProjectorTest {

    private fun p(lat: Double, lon: Double) = TrackPoint(
        timestamp = 0L,
        latitude = lat,
        longitude = lon,
        accuracy = 0f,
        speed = 0f,
        provider = "test"
    )

    @Test
    fun `empty input returns empty`() {
        assertTrue(TrackProjector.normalize(emptyList()).isEmpty())
    }

    @Test
    fun `single point maps to center`() {
        val n = TrackProjector.normalize(listOf(p(39.9, 116.4)))
        assertEquals(0.5f, n[0].x, 0.001f)
        assertEquals(0.5f, n[0].y, 0.001f)
    }

    @Test
    fun `all points within 0-1 range`() {
        val pts = listOf(
            p(39.90, 116.40),
            p(39.95, 116.45),
            p(39.88, 116.38)
        )
        TrackProjector.normalize(pts).forEach { n ->
            assertTrue(n.x in 0f..1f)
            assertTrue(n.y in 0f..1f)
        }
    }

    @Test
    fun `north latitude maps to smaller y (top)`() {
        val south = p(39.0, 116.0)
        val north = p(40.0, 116.0)
        val n = TrackProjector.normalize(listOf(south, north))
        // 南边 y 大（画布下方），北边 y 小（画布上方）
        assertTrue("north should be above south", n[1].y < n[0].y)
    }

    @Test
    fun `east longitude maps to larger x`() {
        val west = p(39.0, 116.0)
        val east = p(39.0, 117.0)
        val n = TrackProjector.normalize(listOf(west, east))
        assertTrue("east should be right of west", n[1].x > n[0].x)
    }

    @Test
    fun `extreme points map to corners`() {
        val pts = listOf(
            p(39.0, 116.0),  // 西南 -> (0, 1)
            p(40.0, 117.0)   // 东北 -> (1, 0)
        )
        val n = TrackProjector.normalize(pts)
        assertEquals(0f, n[0].x, 0.001f)
        assertEquals(1f, n[0].y, 0.001f)
        assertEquals(1f, n[1].x, 0.001f)
        assertEquals(0f, n[1].y, 0.001f)
    }
}
