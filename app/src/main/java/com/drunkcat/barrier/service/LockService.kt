package com.drunkcat.barrier.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.drunkcat.barrier.MainActivity
import com.drunkcat.barrier.R
import com.drunkcat.barrier.admin.DeviceOwnerManager
import com.drunkcat.barrier.data.Prefs
import com.drunkcat.barrier.track.TrackRecorder
import com.drunkcat.barrier.track.TrackStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 前台服务：喝酒模式运行期间保持常驻。
 *
 * 职责：
 * 1. 显示常驻通知，让用户知道保护在进行
 * 2. 每 15 分钟检查一次保护状态（电池优化、通知权限等）
 * 3. 状态失效时通过通知提醒（Phase 1 用通知代替全屏警告）
 */
class LockService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var manager: DeviceOwnerManager
    private lateinit var prefs: Prefs
    private lateinit var recorder: TrackRecorder

    override fun onCreate() {
        super.onCreate()
        manager = DeviceOwnerManager(this)
        prefs = Prefs(this)
        recorder = TrackRecorder(this, TrackStore(this))
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(now = System.currentTimeMillis()))
        // 保护开启期间记录轨迹
        recorder.start()
        scope.launch {
            while (true) {
                delay(REFRESH_NOTIFICATION_MS)
                checkStatus()
                val expired = checkExpiry()
                if (expired) break
                // 刷新常驻通知（剩余时间 / 风险分）
                refreshNotification()
            }
        }
        return START_STICKY
    }

    /**
     * 检查契约是否到期。
     * @return true 表示已到期、服务即将停止
     */
    private suspend fun checkExpiry(): Boolean {
        val until = prefs.getProtectionUntil()
        if (until <= 0L) return false
        if (System.currentTimeMillis() < until) return false

        // 到期：解除保护
        prefs.setProtectionActive(false)
        prefs.setProtectionUntil(0L)
        prefs.setContractSealed(false)
        // 清空风险信号
        com.drunkcat.barrier.risk.RiskMonitor.reset()
        // 停止服务
        stopSelf()
        return true
    }

    private suspend fun checkStatus() {
        val locked = prefs.getLockedPackages()
        val isOwner = manager.isDeviceOwner()
        val hidden = locked.any { manager.isAppHidden(it) }

        val problems = mutableListOf<String>()
        if (!isOwner) problems.add("Device Owner 已失效")
        if (locked.isNotEmpty() && !hidden) problems.add("锁定未生效")

        if (problems.isNotEmpty()) {
            notifyWarning(problems.joinToString("; "))
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "醉猫结界",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "喝酒模式运行状态"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    /** 刷新常驻通知：剩余时间 + 当前风险分 */
    private fun refreshNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        val n = buildNotification(now = System.currentTimeMillis())
        nm.notify(NOTIFICATION_ID, n)
    }

    private fun buildNotification(now: Long): Notification {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val until = runBlocking { prefs.getProtectionUntil() }
        val riskScore = com.drunkcat.barrier.risk.RiskMonitor.currentScore(now)
        val riskColor = when {
            riskScore >= 60 -> "🔴"
            riskScore >= 40 -> "🟡"
            riskScore > 0 -> "🟢"
            else -> "⚪"
        }

        val whenText = if (until > 0L) {
            val remainMs = (until - now).coerceAtLeast(0L)
            val h = remainMs / 3600_000L
            val m = (remainMs % 3600_000L) / 60_000L
            "剩余 ${h}h ${m}m 自动解除"
        } else {
            "手动解除"
        }

        val content = buildString {
            append(riskColor).append(" 风险分 ").append(riskScore).append(" / 100")
            append("\n").append(whenText)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("结界守护中")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun notifyWarning(message: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("保护可能已失效")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(WARNING_ID, n)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        recorder.stop()
        // 清除常驻通知（防止服务被系统杀后通知残留）
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_ID)
        scope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "drunckat_lock"
        private const val NOTIFICATION_ID = 1001
        private const val WARNING_ID = 1002
        private const val REFRESH_NOTIFICATION_MS = 30 * 1000L  // 30 秒刷一次

        fun start(context: Context) {
            val i = Intent(context, LockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockService::class.java))
        }
    }
}
