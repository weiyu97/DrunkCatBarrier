package com.drunkcat.barrier.data

/**
 * 保护级别三档。
 *
 * 对应 RiskScorer.THRESHOLD 不同的触发灵敏度：
 * - LITE：轻度，只拦深夜时段，不自动触发（THRESHOLD=99 相当于关掉自动触发）
 * - STANDARD：标准（默认），风险分 ≥ 60 触发预警
 * - STRICT：严格，风险分 ≥ 40 直接上锁
 */
enum class ProtectionLevel(val threshold: Int, val label: String, val description: String) {
    LITE(
        threshold = 99,
        label = "轻度",
        description = "只拦截深夜时段，不自动触发保护。小酌模式。"
    ),
    STANDARD(
        threshold = 60,
        label = "标准",
        description = "检测到多项异常信号时触发预警（默认）。"
    ),
    STRICT(
        threshold = 40,
        label = "严格",
        description = "信号稍多立即上锁，宁可误锁。"
    )
}
