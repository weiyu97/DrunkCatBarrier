package com.drunkcat.barrier.risk

/**
 * 风险评分器。
 *
 * 将所有激活的信号按权重求和，得到 0-100 的风险分。
 * 超过阈值即触发自动保护。
 *
 * 纯逻辑，无 Android 依赖，可单元测试。
 */
object RiskScorer {

    /** 触发自动上锁的阈值 */
    const val THRESHOLD = 60

    /** 信号有效期：超过该时间的信号不再计入 */
    const val SIGNAL_TTL_MS = 30 * 60 * 1000L  // 30 分钟

    /**
     * 计算风险分。
     *
     * @param hits 当前所有已触发的信号（可能包含过期的）
     * @param now 当前时间戳
     * @return 0-100 的风险分
     */
    fun score(hits: List<SignalHit>, now: Long): Int {
        val fresh = hits.filter { now - it.timestamp <= SIGNAL_TTL_MS }
        if (fresh.isEmpty()) return 0

        // 同一信号只计一次（取最新的那条的权重）
        val uniqueSignals = fresh.map { it.signal }.distinct()
        val total = uniqueSignals.sumOf { it.weight }
        return total.coerceIn(0, 100)
    }

    /**
     * 是否应该触发保护。
     */
    fun shouldTrigger(hits: List<SignalHit>, now: Long): Boolean =
        score(hits, now) >= THRESHOLD

    /**
     * 判断当前是否处于深夜时段。
     */
    fun isLateNight(hourOfDay: Int): Boolean =
        hourOfDay >= 22 || hourOfDay < 6

    /**
     * 判断包名是否为高风险应用（通讯录/短信/社交/支付）。
     *
     * 采用关键词匹配，避免硬编码具体包名。
     */
    fun isHighRiskPackage(packageName: String): Boolean {
        val keywords = listOf(
            "contact", "contacts",      // 通讯录
            "mms", "sms", "messaging",  // 短信
            "wechat", "tencent.mm",     // 微信
            "qq", "telegram", "whatsapp", "messenger",  // 社交
            "weibo", "douyin", "tiktok", "kuaishou",    // 微博/抖音/快手
            "alipay", "wallet", "pay", "bank",           // 支付/银行
            "taobao", "jd", "pinduoduo", "meituan"       // 购物/外卖
        )
        val lower = packageName.lowercase()
        return keywords.any { lower.contains(it) }
    }

    /**
     * 判断包名是否为短视频应用（抖音/快手/视频号/TikTok 等）。
     *
     * 短视频是「支架刷视频 + 语音」漏判场景的核心：用户长时间静置观看、
     * 不切屏也不打字，传统信号几乎为零。深夜打开时视为强风险。
     */
    fun isShortVideoPackage(packageName: String): Boolean {
        val keywords = listOf(
            "aweme",                    // 抖音（包名 com.ss.android.ugc.aweme）
            "douyin", "tiktok",         // 抖音国际版 / TikTok
            "kuaishou", "kwai",         // 快手
            "weishi",                   // 微视
            "bili",                    // B站（tv.danmaku.bili）
            "v.qq", "qqlive", "qiyi",   // 腾讯视频 / 爱奇艺等
            "ixigua", "huoshan"         // 西瓜视频 / 火山小视频
        )
        val lower = packageName.lowercase()
        return keywords.any { lower.contains(it) }
    }
}
