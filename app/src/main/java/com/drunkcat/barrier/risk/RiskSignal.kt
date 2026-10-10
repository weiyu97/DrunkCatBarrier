package com.drunkcat.barrier.risk

/**
 * 风险信号类型与权重。
 *
 * 设计原则：宁可误锁，不可漏判。
 * 误锁清醒用户 -> 过一次验证即可解除（损失 30 秒）
 * 漏判醉酒用户 -> 社死/破财（无法挽回）
 * 因此权重设置偏向敏感。
 */
enum class RiskSignal(val weight: Int, val label: String) {
    /** 深夜时段 22:00 - 06:00 */
    LATE_NIGHT(30, "深夜时段"),

    /** 短时间内频繁切换应用 */
    FREQUENT_APP_SWITCH(25, "频繁切换应用"),

    /** 打开高风险应用（通讯录/短信/社交/支付） */
    HIGH_RISK_APP(20, "打开高风险应用"),

    /** 加速度计检测到持续晃动（步态不稳） */
    DEVICE_SHAKING(15, "手机持续晃动"),

    /** 长时间无操作后突然活跃（可能的断片恢复） */
    SUDDEN_ACTIVITY(10, "突然活跃")
}

/**
 * 单条信号的一次观测记录。
 */
data class SignalHit(
    val signal: RiskSignal,
    val timestamp: Long,
    val detail: String = ""
)
