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

    /**
     * 发送自动保护开启通知。
     *
     * @param context Android Context
     * @param defaultLockedCount 若 > 0，通知中会提及"已套用默认拦截清单（X 个应用）"
     */
    fun notify(context: Context, defaultLockedCount: Int = 0) {
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

        val (title, text) = if (defaultLockedCount > 0) {
            "醉猫结界已自动开启 · 已套用默认封锁清单" to
                "检测到你可能喝多了，已开启保护，并套用 $defaultLockedCount 个高风险应用拦截。打开 App 可随时修改。"
        } else {
            "醉猫结界已自动开启" to
                "检测到你可能喝多了，已为你开启保护。如需解除，请打开 App 通过清醒验证。"
        }

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFY_ID, n)
    }
}
