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
}
