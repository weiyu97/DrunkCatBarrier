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
        val batteryOptimized: Boolean,
        val bluetoothOk: Boolean = false
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
            batteryOptimized = checkBattery(context),
            bluetoothOk = checkBluetooth(context)
        )
    }

    /** 蓝牙权限（可选，用于心率手环） */
    private fun checkBluetooth(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scan = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            val conn = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return scan && conn
        }
        return true  // 旧版本无需运行时权限
    }

    /** 通知权限（API 33+ 需运行时授权） */
    private fun checkNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 无障碍服务是否开启（双方法，任一命中即算） */
    private fun checkAccessibility(context: Context): Boolean {
        val byManager = try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as android.view.accessibility.AccessibilityManager
            am.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }
        } catch (e: Exception) { false }

        val bySettings = try {
            val raw = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            raw != null && raw != "null" && raw.split(":").any {
                it.contains(context.packageName) && it.contains("AppWatchService")
            }
        } catch (e: Exception) { false }

        return byManager || bySettings
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
