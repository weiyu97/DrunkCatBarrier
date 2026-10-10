package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 一键体检。
 *
 * 集中展示所有影响保护生效的配置项，未完成的提供一键跳转修复。
 */
data class CheckupItem(
    val title: String,
    val desc: String,
    val ok: Boolean,
    val critical: Boolean,
    val action: () -> Unit
)

@Composable
fun GlassCheckupScreen(
    items: List<CheckupItem>,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05030C), Color(0xFF0C0618), Color(0xFF080412))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassHeader(statusText = "结界体检")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 13.dp)
            ) {
                val criticalFails = items.count { !it.ok && it.critical }
                val warns = items.count { !it.ok && !it.critical }

                // 总览
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (criticalFails > 0) Color(0x99FF4678)
                    else Color(0x806DFFB0)
                ) {
                    CardTitle("结界状态")

                    val summary = when {
                        criticalFails > 0 -> "有 $criticalFails 项关键配置缺失，保护无法生效"
                        warns > 0 -> "核心功能正常，有 $warns 项体验配置未完成"
                        else -> "一切就绪，结界完整"
                    }

                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            criticalFails > 0 -> Glass.Primary
                            warns > 0 -> Glass.Tertiary
                            else -> Color(0xFF6DFFB0)
                        },
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 明细
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("配置明细")
                    items.forEach { item ->
                        CheckupRow(item)
                    }
                }

                Spacer(Modifier.height(12.dp))

                GlassSecondaryButton(text = "返回", onClick = onBack)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CheckupRow(item: CheckupItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .then(
                if (!item.ok) Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { item.action() }
                    .padding(horizontal = 4.dp)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UiIcon(
            type = when {
                item.ok -> UiIconType.CHECK
                item.critical -> UiIconType.WARNING
                else -> UiIconType.WARNING
            },
            size = 16.dp,
            tint = when {
                item.ok -> Color(0xFF6DFFB0)
                item.critical -> Glass.Primary
                else -> Glass.Tertiary
            }
        )

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                color = Glass.OnBg,
                fontFamily = FontFamily.Serif,
                fontWeight = if (!item.ok) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = item.desc,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Glass.OnSurfaceVariant,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (!item.ok) {
            Text(
                text = "去修复 >",
                fontSize = 11.5.sp,
                color = Glass.Primary,
                fontFamily = FontFamily.Serif
            )
        }
    }
}
