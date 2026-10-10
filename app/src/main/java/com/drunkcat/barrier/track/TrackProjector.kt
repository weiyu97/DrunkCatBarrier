package com.drunkcat.barrier.track

import kotlin.math.abs
import kotlin.math.max

/**
 * 轨迹投影：将经纬度归一化到 0..1 的画布坐标。
 *
 * 纯逻辑，可单元测试。不涉及任何 Android 依赖。
 */
object TrackProjector {

    data class Norm(val x: Float, val y: Float)

    /**
     * 将轨迹点归一化到 [0,1] × [0,1]。
     * x 对应经度（东为正），y 对应纬度（**北为上，故已翻转**）。
     *
     * 单点或范围为零时，返回中心点 (0.5, 0.5)。
     */
    fun normalize(points: List<TrackPoint>): List<Norm> {
        if (points.isEmpty()) return emptyList()

        val lats = points.map { it.latitude }
        val lons = points.map { it.longitude }
        val minLat = lats.min()
        val maxLat = lats.max()
        val minLon = lons.min()
        val maxLon = lons.max()

        val latRange = max(abs(maxLat - minLat), 1e-6)
        val lonRange = max(abs(maxLon - minLon), 1e-6)

        val single = latRange <= 1e-6 && lonRange <= 1e-6

        return points.map { p ->
            if (single) {
                Norm(0.5f, 0.5f)
            } else {
                val x = ((p.longitude - minLon) / lonRange).toFloat()
                // 纬度翻转：北（大值）在上
                val y = 1f - ((p.latitude - minLat) / latRange).toFloat()
                Norm(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
            }
        }
    }
}
