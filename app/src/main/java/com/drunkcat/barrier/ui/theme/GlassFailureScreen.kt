package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 验证失败的反馈界面。
 *
 * 告知用户失败原因与冷却时间，避免「被弹回桌面但不知道发生了什么」。
 */
@Composable
fun GlassFailureScreen(
    reason: String,
    failureCount: Int,
    cooldownMs: Long,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05030C), Color(0xFF0C0618))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            borderColor = Color(0x99FF4678)
        ) {
            CardTitle("试炼未通过")

            Text(
                text = reason.ifEmpty { "未能通过清醒验证" },
                fontSize = 14.sp,
                color = Color(0xFFFF6B9D),
                fontFamily = FontFamily.Serif
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (cooldownMs > 0)
                    "失败 $failureCount 次，冷却 ${cooldownMs / 60_000} 分钟后可重试。"
                else "已失败 $failureCount 次。",
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = Glass.OnSurfaceVariant,
                fontFamily = FontFamily.Serif
            )

            Spacer(Modifier.height(16.dp))

            GlassPrimaryButton(text = "知道了", onClick = onDismiss)
        }
    }
}
