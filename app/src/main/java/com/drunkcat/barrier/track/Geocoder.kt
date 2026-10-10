package com.drunkcat.barrier.track

import android.content.Context
import android.location.Address
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 逆地理编码：坐标 -> 可读地点名。
 *
 * 优先使用系统内置 Geocoder（Android 自带，无需 API Key）。
 * 若系统 Geocoder 不可用（部分国产 ROM 移除），降级为坐标显示。
 *
 * 隐私：仅上传坐标，不上传任何其他信息；结果仅本地缓存。
 */
class PlaceResolver(private val context: Context) {

    /**
     * 解析坐标对应的地点名。
     * @return 地点名，失败返回 null
     */
    suspend fun resolve(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= 33) {
                    // Android 13+ 同步接口已废弃，用阻塞式等待回调
                    val latch = java.util.concurrent.CountDownLatch(1)
                    var result: List<Address>? = null
                    geocoder.getFromLocation(latitude, longitude, 1) { list ->
                        result = list
                        latch.countDown()
                    }
                    latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    result
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                val addr = addresses?.firstOrNull() ?: return@withContext null
                buildName(addr)
            } catch (e: Exception) {
                null
            }
        }

    private fun buildName(addr: Address): String? {
        val parts = mutableListOf<String>()
        // 优先取兴趣点名（如「星巴克」）
        addr.featureName?.let { if (it.isNotBlank() && !it.matches(Regex("\\d+"))) parts.add(it) }
        // 街道
        addr.thoroughfare?.let { parts.add(it) }
        // 区域
        addr.subLocality?.let { parts.add(it) }
        addr.locality?.let { parts.add(it) }

        return if (parts.isEmpty()) null else parts.distinct().joinToString(" · ")
    }
}
