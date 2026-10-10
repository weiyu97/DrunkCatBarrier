package com.drunkcat.barrier.data

import java.util.Calendar

/**
 * 契约到期时间计算与格式化。
 */
object ContractTime {

    /** 默认结束小时（次日 8:00） */
    const val DEFAULT_HOUR = 8

    /** 可选的结束小时范围 */
    val HOUR_OPTIONS = listOf(6, 7, 8, 9, 10, 11, 12)

    /**
     * 计算「下一个指定小时:00」的时间戳。
     * 若当前时间早于今天该时刻，则为今天；否则为明天。
     *
     * @param hour 24 小时制小时（0-23）
     */
    fun nextAt(
        hour: Int = DEFAULT_HOUR,
        now: Long = System.currentTimeMillis()
    ): Long {
        val h = hour.coerceIn(0, 23)
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    /** 兼容旧调用：下一个早上 8:00 */
    fun nextMorning8(now: Long = System.currentTimeMillis()): Long =
        nextAt(DEFAULT_HOUR, now)

    /**
     * 格式化为可读文案，如「明早 8:00」「今晚 23:00」。
     */
    fun label(until: Long, now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = until }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val isTomorrow = cal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR)
        val period = when (hour) {
            in 0..5 -> "凌晨"
            in 6..11 -> "早上"
            in 12..17 -> "下午"
            else -> "晚上"
        }
        val dayPrefix = if (isTomorrow) "明" else "今"
        val minute = cal.get(Calendar.MINUTE)
        val timeStr = if (minute == 0) "${hour}:00" else "${hour}:${"%02d".format(minute)}"
        return "$dayPrefix$period $timeStr"
    }

    /**
     * 剩余时间文案，如「还有 3 小时 20 分钟」。
     */
    fun remaining(until: Long, now: Long = System.currentTimeMillis()): String {
        val diff = until - now
        if (diff <= 0) return "已到期"
        val totalMin = diff / 60_000
        val hours = totalMin / 60
        val mins = totalMin % 60
        return when {
            hours > 0 && mins > 0 -> "还有 $hours 小时 $mins 分钟"
            hours > 0 -> "还有 $hours 小时"
            else -> "还有 $mins 分钟"
        }
    }
}
