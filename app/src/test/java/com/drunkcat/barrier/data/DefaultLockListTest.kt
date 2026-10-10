package com.drunkcat.barrier.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLockListTest {

    private val sample = listOf(
        "com.tencent.mm",              // 微信 -> 锁
        "com.tencent.mobileqq",        // QQ -> 锁
        "com.eg.android.AlipayGphone", // 支付宝 -> 锁
        "com.taobao.taobao",           // 淘宝 -> 锁
        "com.ss.android.ugc.aweme",    // 抖音 -> 锁
        "com.android.chrome",          // 浏览器 -> 不锁
        "com.android.settings",        // 设置 -> 不锁
        "com.google.android.apps.maps",// 地图 -> 白名单，不锁
        "com.android.dialer",          // 拨号 -> 白名单，不锁
        "com.sdu.didi.psnger"          // 打车 -> 白名单，不锁
    )

    @Test
    fun `picks social apps`() {
        val picked = DefaultLockList.pickDefault(sample)
        assertTrue(picked.contains("com.tencent.mm"))
        assertTrue(picked.contains("com.tencent.mobileqq"))
        assertTrue(picked.contains("com.ss.android.ugc.aweme"))
    }

    @Test
    fun `picks payment apps`() {
        val picked = DefaultLockList.pickDefault(sample)
        assertTrue(picked.contains("com.eg.android.AlipayGphone"))
    }

    @Test
    fun `picks shopping apps`() {
        val picked = DefaultLockList.pickDefault(sample)
        assertTrue(picked.contains("com.taobao.taobao"))
    }

    @Test
    fun `excludes browser and settings`() {
        val picked = DefaultLockList.pickDefault(sample)
        assertFalse(picked.contains("com.android.chrome"))
        assertFalse(picked.contains("com.android.settings"))
    }

    @Test
    fun `excludes permanent whitelist`() {
        val picked = DefaultLockList.pickDefault(sample)
        assertFalse("maps should not be locked", picked.contains("com.google.android.apps.maps"))
        assertFalse("dialer should not be locked", picked.contains("com.android.dialer"))
        assertFalse("ride hailing should not be locked", picked.contains("com.sdu.didi.psnger"))
    }

    @Test
    fun `empty input returns empty`() {
        assertTrue(DefaultLockList.pickDefault(emptyList()).isEmpty())
    }
}
