package com.drunkcat.barrier.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 保护健康检查。
 *
 * 保护依赖多个系统权限，任一缺失都会导致保护静默失效。
 * 本类集中检测，供 UI 提示用户修复。
 */
object HealthCheck {

    data class Status(
        val notificationOk: Boolean,
        val accessibilityOk: Boolean,
        val locationOk: Boolean,
        val batteryOptimized: Boolean
    ) {
        /** 是否存在会导致保护失效的严重问题 */
        val hasCriticalIssue: Boolean
            get() = !accessibilityOk

        /** 是否存在影响体验的问题 */
        val hasWarning: Boolean
            get() = !notificationOk
    }

    fun check(context: Context): Status {
        return Status(
            notificationOk = checkNotification(context),
            accessibilityOk = checkAccessibility(context),
            locationOk = checkLocation(context),
            batteryOptimized = checkBattery(context)
        )
    }

    /** 通知权限（API 33+ 需运行时授权） */
    private fun checkNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 无障碍服务是否开启 */
    private fun checkAccessibility(context: Context): Boolean {
        val enabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(":").any {
            it.contains(context.packageName) && it.contains("AppWatchService")
        }
    }

    /** 定位权限（影响轨迹记录，非关键） */
    private fun checkLocation(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 是否在电池优化白名单中 */
    private fun checkBattery(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE)
                as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) {
            true
        }
    }
}
