package com.drunkcat.barrier.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermanentWhitelistTest {

    @Test
    fun `dialer is allowed`() {
        assertTrue(PermanentWhitelist.isAllowed("com.android.dialer"))
        assertTrue(PermanentWhitelist.isAllowed("com.google.android.dialer"))
    }

    @Test
    fun `phone variants are allowed`() {
        assertTrue(PermanentWhitelist.isAllowed("com.android.phone"))
        assertTrue(PermanentWhitelist.isAllowed("com.android.server.telecom"))
    }

    @Test
    fun `maps are allowed`() {
        assertTrue(PermanentWhitelist.isAllowed("com.google.android.apps.maps"))
        assertTrue(PermanentWhitelist.isAllowed("com.baidu.BaiduMap"))
    }

    @Test
    fun `ride hailing is allowed`() {
        assertTrue(PermanentWhitelist.isAllowed("com.sdu.didi.psnger"))
    }

    @Test
    fun `camera is allowed`() {
        assertTrue(PermanentWhitelist.isAllowed("com.android.camera2"))
    }

    @Test
    fun `wechat is not allowed`() {
        assertFalse(PermanentWhitelist.isAllowed("com.tencent.mm"))
    }

    @Test
    fun `alipay is not allowed`() {
        assertFalse(PermanentWhitelist.isAllowed("com.eg.android.AlipayGphone"))
    }

    @Test
    fun `keyword fallback matches custom dialer`() {
        // 国产 ROM 的拨号器包名可能不同，关键词兜底应命中
        assertTrue(PermanentWhitelist.isAllowed("com.miui.phone"))
        assertTrue(PermanentWhitelist.isAllowed("com.coloros.contacts"))
    }
}
