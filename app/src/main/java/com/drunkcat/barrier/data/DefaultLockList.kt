package com.drunkcat.barrier.data

/**
 * 默认锁定名单。
 *
 * 当自动保护触发、但用户从未配置过锁定名单时，
 * 套用这份清单，避免「保护中却拦不住任何东西」。
 *
 * 清单覆盖最容易造成社死/破财的应用类型：
 * 社交、通讯、支付、购物。
 */
object DefaultLockList {

    /**
     * 关键词匹配：这些类型的应用默认被锁。
     * 与 PermanentWhitelist 互斥（白名单优先）。
     */
    private val RISKY_KEYWORDS = listOf(
        // 社交
        "wechat", "tencent.mm",
        "mobileqq", "tencent.mobileqq",
        "weibo",
        "douyin", "tiktok", "aweme",
        "kuaishou",
        "xiaohongshu",
        "telegram", "whatsapp", "messenger",
        "instagram", "facebook", "twitter",
        // 通讯（通话/短信类，非拨号器）
        "messaging", "mms",
        // 支付/银行
        "alipay", "wallet", "unionpay",
        "icbc", "ccb", "abchina", "boc", "cmb",
        // 购物/外卖
        "taobao", "tmall", "jd", "pinduoduo",
        "meituan", "eleme", "dianping"
    )

    /**
     * 从已安装应用列表中挑选默认锁定项。
     * 排除永久白名单。
     */
    fun pickDefault(
        installedPackages: List<String>
    ): List<String> {
        return installedPackages.filter { pkg ->
            if (PermanentWhitelist.isAllowed(pkg)) return@filter false
            val lower = pkg.lowercase()
            RISKY_KEYWORDS.any { lower.contains(it) }
        }
    }
}
