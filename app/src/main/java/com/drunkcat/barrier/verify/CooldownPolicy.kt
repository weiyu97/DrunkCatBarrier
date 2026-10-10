package com.drunkcat.barrier.verify

/**
 * 失败冷却策略。
 *
 * 来自 PROJECT.md 6.6：
 * 1 次 -> 30s
 * 2 次 -> 2min
 * 3 次 -> 10min
 * 4 次 -> 30min
 * 5 次 -> 锁定至次日（由上层处理，此处返回哨兵值）
 *
 * 附加规则（L3 凌晨 2 点后）：
 * 累计失败 3 次且保护级别为 L3，触发次日锁定。
 * 该判定放在上层，本策略仅提供基础阶梯。
 */
object CooldownPolicy {

    /** 锁定至次日固定时间的哨兵值。 */
    const val LOCK_UNTIL_NEXT_DAY = -1L

    private val STEPS = longArrayOf(
        30_000L,
        2 * 60_000L,
        10 * 60_000L,
        30 * 60_000L
    )

    fun cooldownFor(failureCount: Int): Long {
        if (failureCount <= 0) return 0L
        if (failureCount > STEPS.size) return LOCK_UNTIL_NEXT_DAY
        return STEPS[failureCount - 1]
    }
}
