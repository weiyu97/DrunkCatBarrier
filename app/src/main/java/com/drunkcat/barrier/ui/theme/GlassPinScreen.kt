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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * 琉璃风 PIN 键盘页。
 *
 * @param title 标题，如「设置 6 位 PIN（第一次）」「输入 PIN 解锁」
 * @param subtitle 副标题/提示
 * @param onSubmit 输入满 6 位并点确认后回调
 * @param onCancel 取消回调
 */
@Composable
fun GlassPinScreen(
    title: String,
    subtitle: String,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("") }

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
            GlassHeader(statusText = "清醒试炼")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 13.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle(title)
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif
                    )

                    // 6 位圆点指示
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(6) { idx ->
                            val filled = idx < input.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 7.dp)
                                    .size(14.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        if (filled) Glass.Primary else Color(0x33FFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        if (filled) Glass.Primary else Color(0x38AA82FF),
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("清空", "0", "删除")
                    )
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                Box(modifier = Modifier.weight(1f)) {
                                    PinKey(key) {
                                        when (key) {
                                            "清空" -> input = ""
                                            "删除" -> if (input.isNotEmpty())
                                                input = input.dropLast(1)
                                            else -> if (input.length < 6)
                                                input += key
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(11.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            GlassSecondaryButton(text = "取消", onClick = onCancel)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            GlassPrimaryButton(
                                text = "确认",
                                enabled = input.length == 6,
                                onClick = { if (input.length == 6) onSubmit(input) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x38AA82FF), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 1) 14.sp else 20.sp,
            color = Glass.OnBg,
            fontFamily = FontFamily.Serif,
            fontWeight = if (label.length > 1) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}
