package com.drunkcat.barrier.data

/**
 * 永远允许的应用（安全底线）。
 *
 * 这些应用在保护期间始终可用，确保醉酒后仍能：
 * 打电话、叫车、导航、联系他人。
 */
object PermanentWhitelist {

    /**
     * 精确包名（常见系统应用）。
     */
    val PACKAGES = setOf(
        // 电话/短信
        "com.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.dialer",
        "com.android.mms",
        "com.google.android.apps.messaging",
        // 地图
        "com.google.android.apps.maps",
        "com.autonavi.minimap",
        "com.baidu.BaiduMap",
        // 打车/代驾
        "com.sdu.didi.psnger",
        "com.didi.passenger",
        // 相机
        "com.android.camera",
        "com.android.camera2",
        "com.google.android.GoogleCamera",
        // 系统 UI
        "com.android.systemui"
    )

    /**
     * 包名分段关键词。
     *
     * 采用「分段精确匹配」而非子串匹配，避免误判：
     * - \`com.android.phone\` 的段 "phone" 命中 ✓
     * - \`com.eg.android.AlipayGphone\` 的段是 "AlipayGphone"，不等于 "phone" ✗
     */
    private val SEGMENT_KEYWORDS = setOf(
        "dialer", "phone", "telecom", "contacts", "mms", "messaging",
        "maps", "baidumap", "gaode", "minimap",
        "taxi", "didi", "daijia",
        "camera", "systemui"
    )

    /**
     * 判断某包名是否属于永远允许。
     */
    fun isAllowed(packageName: String): Boolean {
        if (packageName in PACKAGES) return true
        // 按 "." 拆分段，任一段精确等于关键词即命中
        val segments = packageName.lowercase().split(".")
        return segments.any { seg -> seg in SEGMENT_KEYWORDS }
    }
}
