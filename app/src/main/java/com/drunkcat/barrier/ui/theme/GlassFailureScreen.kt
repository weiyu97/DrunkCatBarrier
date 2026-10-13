package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 验证失败的反馈界面。
 *
 * 告知用户失败原因与冷却时间，避免「被弹回桌面但不知道发生了什么」。
 * 同时提供紧急跳过出口，让用户能绕过验证直接进入应用（会被记录进复盘）。
 */
@Composable
fun GlassFailureScreen(
    reason: String,
    failureCount: Int,
    cooldownMs: Long,
    onDismiss: () -> Unit,
    onEmergencyBypass: (() -> Unit)? = null
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

            if (onEmergencyBypass != null) {
                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFFFF4444), RoundedCornerShape(12.dp))
                        .clickable { onEmergencyBypass() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚠ 紧急情况直接进入",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4444),
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "（将被记录进复盘）",
                            fontSize = 10.5.sp,
                            color = Color(0xFFFF8888),
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }
    }
}
