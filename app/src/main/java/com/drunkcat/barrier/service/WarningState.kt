package com.drunkcat.barrier.service

/**
 * 预警状态管理。
 *
 * 检测到风险时，不立即锁定，而是进入 30 秒预警期：
 * - 用户点「我没醉」-> 取消预警，清空风险信号
 * - 30 秒内无操作 -> 自动开启保护
 *
 * 用内存状态即可，进程被杀后预警自然失效（下次检测到风险会重新预警）。
 */
object WarningState {

    /** 预警时长 */
    const val WARNING_DURATION_MS = 30_000L

    @Volatile var active: Boolean = false
        private set

    @Volatile var startedAt: Long = 0L
        private set

    @Volatile var countdownEndsAt: Long = 0L
        private set

    /** 预警期间用户可点击取消的回调 */
    @Volatile var onCancel: (() -> Unit)? = null

    /** 预警到期自动锁定的回调 */
    @Volatile var onTimeout: (() -> Unit)? = null

    /** 用户本轮 session 里是否点过「我没醉」（供首页横条显示） */
    @Volatile var cancelledThisSession: Boolean = false
        private set

    fun start(now: Long = System.currentTimeMillis()) {
        active = true
        startedAt = now
        countdownEndsAt = now + WARNING_DURATION_MS
    }

    fun cancel() {
        active = false
        cancelledThisSession = true
        onCancel?.invoke()
        onCancel = null
        onTimeout = null
    }

    fun complete() {
        active = false
        onTimeout?.invoke()
        onCancel = null
        onTimeout = null
    }

    /** 剩余毫秒数 */
    fun remainingMs(now: Long = System.currentTimeMillis()): Long =
        (countdownEndsAt - now).coerceAtLeast(0)
}
