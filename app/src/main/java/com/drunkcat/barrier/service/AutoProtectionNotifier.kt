package com.drunkcat.barrier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drunkcat.barrier.R

/**
 * 自动保护触发时的通知。
 * 告知用户"检测到你可能喝多了，已自动开启保护"，并说明如何解除。
 */
object AutoProtectionNotifier {

    private const val CHANNEL_ID = "drunckat_auto"
    private const val NOTIFY_ID = 2001

    fun notify(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "自动保护通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "检测到风险时自动开启保护的提醒"
            }
            nm.createNotificationChannel(ch)
        }

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("醉猫结界已自动开启")
            .setContentText("检测到你可能喝多了，已为你开启保护。如需解除，请打开 App 通过清醒验证。")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFY_ID, n)
    }
}
