package com.drunkcat.barrier.track

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 轨迹记录器。
 *
 * 仅在保护开启期间记录位置，使用系统 LocationManager，不依赖 Google Play 服务。
 * 采样策略：
 * - 移动超过 50 米记录一次
 * - 或间隔超过 3 分钟记录一次
 *
 * 隐私：坐标仅本地存储，不联网（逆地理编码为可选后续功能）。
 */
class TrackRecorder(
    private val context: Context,
    private val store: TrackStore
) {

    private var locationManager: LocationManager? = null
    private var listener: LocationListener? = null
    private var lastPoint: TrackPoint? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val resolver = PlaceResolver(context)

    /** 采样间隔阈值 */
    private val minDistanceMeters = 50f
    private val minIntervalMs = 3 * 60 * 1000L

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) return
        if (listener != null) return  // 已在运行

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return
        locationManager = lm

        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                handleLocation(location)
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        listener = l

        // 优先 GPS，退化到网络定位
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60_000L, 20f, l)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 120_000L, 30f, l)
            }
        } catch (e: Exception) {
            // 忽略：权限或系统限制
        }
    }

    fun stop() {
        listener?.let { l ->
            try {
                locationManager?.removeUpdates(l)
            } catch (e: Exception) {}
        }
        listener = null
    }

    private fun handleLocation(location: Location) {
        val now = System.currentTimeMillis()
        val point = TrackPoint(
            timestamp = now,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            speed = location.speed,
            provider = location.provider ?: "unknown"
        )

        val last = lastPoint
        val shouldRecord = if (last == null) {
            true
        } else {
            val dist = distanceBetween(last, point)
            val elapsed = now - last.timestamp
            dist >= minDistanceMeters || elapsed >= minIntervalMs
        }

        if (shouldRecord) {
            lastPoint = point
            scope.launch {
                // 尝试解析地点名，失败则存原始坐标
                val name = try {
                    resolver.resolve(point.latitude, point.longitude)
                } catch (e: Exception) { null }
                store.append(point.copy(placeName = name))
            }
        }
    }

    private fun distanceBetween(a: TrackPoint, b: TrackPoint): Float {
        val r = FloatArray(1)
        Location.distanceBetween(
            a.latitude, a.longitude,
            b.latitude, b.longitude,
            r
        )
        return r[0]
    }
}
