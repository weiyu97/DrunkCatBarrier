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
    SUDDEN_ACTIVITY(10, "突然活跃"),

    /** 打字行为异常（删除率高，弱信号，仅统计不读内容） */
    TYPING_ANOMALY(10, "打字异常"),

    /**
     * 深夜刷短视频（抖音/快手/视频号/TikTok 等）。
     * 权重直接等于阈值：深夜打开即触发预警，是最易漏的「支架刷视频+语音」场景。
     */
    SHORT_VIDEO_NIGHT(60, "深夜刷短视频"),

    /**
     * 手机静置 + 长时间同一 App 前台（支架看视频特征）。
     * 弱信号，仅辅助：深夜叠加其他信号即可触发保护。
     */
    STATIC_FOREGROUND(15, "手机静置长时间前台")
}

/**
 * 单条信号的一次观测记录。
 */
data class SignalHit(
    val signal: RiskSignal,
    val timestamp: Long,
    val detail: String = ""
)
