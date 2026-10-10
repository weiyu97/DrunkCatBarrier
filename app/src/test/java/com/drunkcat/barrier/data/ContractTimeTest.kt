package com.drunkcat.barrier.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ContractTimeTest {

    private fun ts(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `before 8am returns same day 8am`() {
        val now = ts(2026, 3, 15, 6, 30)
        val until = ContractTime.nextMorning8(now)
        val cal = Calendar.getInstance().apply { timeInMillis = until }
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `after 8am returns next day 8am`() {
        val now = ts(2026, 3, 15, 22, 0)
        val until = ContractTime.nextMorning8(now)
        val cal = Calendar.getInstance().apply { timeInMillis = until }
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `exactly 8am returns next day`() {
        val now = ts(2026, 3, 15, 8, 0)
        val until = ContractTime.nextMorning8(now)
        val cal = Calendar.getInstance().apply { timeInMillis = until }
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `until is always in the future`() {
        val now = System.currentTimeMillis()
        val until = ContractTime.nextMorning8(now)
        assertTrue(until > now)
    }

    @Test
    fun `label contains hour and period`() {
        val now = ts(2026, 3, 15, 22, 0)
        val until = ContractTime.nextMorning8(now)
        val label = ContractTime.label(until, now)
        assertTrue(label.contains("8"))
        assertTrue(label.contains("早"))
    }

    // ---------- 自定义小时 ----------

    @Test
    fun `custom hour 6 before 6am is today`() {
        val now = ts(2026, 3, 15, 4, 0)
        val until = ContractTime.nextAt(6, now)
        val cal = Calendar.getInstance().apply { timeInMillis = until }
        assertEquals(6, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `custom hour 12 after 12pm is tomorrow`() {
        val now = ts(2026, 3, 15, 14, 0)
        val until = ContractTime.nextAt(12, now)
        val cal = Calendar.getInstance().apply { timeInMillis = until }
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `hour coerced to valid range`() {
        val now = ts(2026, 3, 15, 10, 0)
        // 输入 -5 -> 0 点，30 -> 23 点
        val until0 = ContractTime.nextAt(-5, now)
        val cal0 = Calendar.getInstance().apply { timeInMillis = until0 }
        assertEquals(0, cal0.get(Calendar.HOUR_OF_DAY))

        val until23 = ContractTime.nextAt(30, now)
        val cal23 = Calendar.getInstance().apply { timeInMillis = until23 }
        assertEquals(23, cal23.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `all hour options produce future time`() {
        val now = System.currentTimeMillis()
        ContractTime.HOUR_OPTIONS.forEach { h ->
            assertTrue("hour $h should be future", ContractTime.nextAt(h, now) > now)
        }
    }

    @Test
    fun `remaining formats hours and minutes`() {
        val now = 1_000_000_000_000L
        // 3 小时 20 分钟
        val until = now + (3 * 60 + 20) * 60_000L
        val r = ContractTime.remaining(until, now)
        assertTrue(r.contains("3"))
        assertTrue(r.contains("20"))
    }

    @Test
    fun `remaining shows expired`() {
        val now = 1_000_000_000_000L
        assertEquals("已到期", ContractTime.remaining(now - 1000, now))
    }
}
