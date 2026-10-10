package com.drunkcat.barrier.data

import java.util.Calendar

/**
 * 契约到期时间计算。
 */
object ContractTime {

    /**
     * 计算「下一个早上 8:00」的时间戳。
     * 若当前时间早于今天 8:00，则为今天 8:00；否则为明天 8:00。
     */
    fun nextMorning8(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    /**
     * 格式化为可读文案，如「明早 8:00」。
     */
    fun label(until: Long, now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = until }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val isTomorrow = cal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR)
        val period = when {
            hour in 0..5 -> "凌晨"
            hour in 6..11 -> "早上"
            hour in 12..17 -> "下午"
            else -> "晚上"
        }
        val dayPrefix = if (isTomorrow) "明" else "今"
        return "$dayPrefix$period ${hour}:00"
    }
}
