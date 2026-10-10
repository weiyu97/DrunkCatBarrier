package com.drunkcat.barrier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drunkcat.barrier.R
import com.drunkcat.barrier.ui.WarningActivity

/**
 * 风险预警通知。
 *
 * 点击通知进入 WarningActivity，显示 30 秒倒计时，
 * 用户可点「我没醉」取消，或等待自动锁定。
 */
object RiskWarningNotifier {

    private const val CHANNEL_ID = "drunckat_warning"
    private const val NOTIFY_ID = 2002

    fun notify(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "清醒预警",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "检测到醉酒风险时的预警提醒"
            }
            nm.createNotificationChannel(ch)
        }

        val pi = PendingIntent.getActivity(
            context,
            0,
            Intent(context, WarningActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("检测到你可能喝多了")
            .setContentText("30 秒后自动开启保护。如果你还清醒，点击取消。")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(NOTIFY_ID, n)
    }
}
