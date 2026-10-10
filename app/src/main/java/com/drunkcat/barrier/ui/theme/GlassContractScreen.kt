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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.data.ContractMode

/**
 * 琉璃风契约仪式页。
 */
@Composable
fun GlassContractScreen(
    initialMode: ContractMode,
    initialEndHour: Int = com.drunkcat.barrier.data.ContractTime.DEFAULT_HOUR,
    onConfirm: (mode: ContractMode, endHour: Int) -> Unit,
    onCancel: () -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) }
    var checked by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var endHour by remember { mutableStateOf(initialEndHour) }

    // 根据选择的小时实时计算文案
    val endTimeLabel = remember(endHour) {
        com.drunkcat.barrier.data.ContractTime.label(
            com.drunkcat.barrier.data.ContractTime.nextAt(endHour)
        )
    }

    val fullText = "我确认自己现在清醒，并自愿锁定所选 App 直到 $endTimeLabel。我理解在此期间无法随意使用这些 App。"

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
            GlassHeader(statusText = "堕天契约仪式")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 13.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("堕天契约仪式")

                    ModeCard(
                        title = "快速契约 · L1-L2",
                        tag = "微醺",
                        desc = "简易誓约。适合只是喝一点的愚民。门槛低，但保护也弱。",
                        active = mode == ContractMode.QUICK,
                        onClick = { mode = ContractMode.QUICK }
                    )
                    Spacer(Modifier.height(9.dp))
                    ModeCard(
                        title = "完整堕天仪式 · L3",
                        tag = "高风险",
                        desc = "手写誓约 + 本地影像锚点。影像只用于次日复盘，绝不参与试炼判定。",
                        active = mode == ContractMode.FULL,
                        onClick = { mode = ContractMode.FULL }
                    )

                    Spacer(Modifier.height(11.dp))

                    // 结束时间选择
                    Text(
                        text = "锁定至",
                        fontSize = 13.sp,
                        color = Glass.Tertiary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.drunkcat.barrier.data.ContractTime.HOUR_OPTIONS.forEach { h ->
                            val active = h == endHour
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (active) Color(0x33FF3264) else Color(0x0AFFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        if (active) Color(0x8CFF4678) else Color(0x38AA82FF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { endHour = h }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${h}:00",
                                    fontSize = 12.sp,
                                    color = if (active) Glass.Primary else Glass.OnBg,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "将于 $endTimeLabel 自动解除",
                        fontSize = 11.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(Modifier.height(11.dp))

                    // 誓约输入区
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x52000000))
                            .border(1.dp, Color(0x38AA82FF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = if (mode == ContractMode.QUICK)
                                    "输入「我确认」三个字……"
                                else
                                    "逐字写下誓约……",
                                fontSize = 12.sp,
                                color = Color(0x59B4A0DC),
                                fontFamily = FontFamily.Serif
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = TextStyle(
                                color = Glass.OnBg,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif
                            ),
                            cursorBrush = SolidColor(Glass.Primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 同意勾选
                    Row(
                        modifier = Modifier.padding(top = 11.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { checked = it }
                        )
                        Text(
                            text = "吾确认缔结契约，并接受次日复盘的审判。",
                            fontSize = 11.5.sp,
                            color = Glass.OnSurfaceVariant,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(top = 14.dp, start = 4.dp)
                        )
                    }

                    error?.let {
                        Text(
                            text = it,
                            color = Glass.Primary,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(11.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            GlassSecondaryButton(text = "返回祭坛", onClick = onCancel)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            GlassPrimaryButton(
                                text = "缔结契约",
                                onClick = {
                                    val ok = if (mode == ContractMode.QUICK) {
                                        checked && text.trim() == "我确认"
                                    } else {
                                        checked && text.trim() == fullText
                                    }
                                    if (ok) {
                                        onConfirm(mode, endHour)
                                    } else {
                                        error = if (!checked) "请先勾选确认" else "誓约文本不匹配"
                                    }
                                }
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
private fun ModeCard(
    title: String,
    tag: String,
    desc: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) Brush.linearGradient(
                    listOf(Color(0x1AFF3264), Color(0x148C50DC))
                ) else Brush.linearGradient(
                    listOf(Color(0x08FFFFFF), Color(0x08FFFFFF))
                )
            )
            .border(
                width = 1.dp,
                color = if (active) Color(0x8CFF4678) else Color(0x38AA82FF),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(13.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Glass.OnBg,
                    fontFamily = FontFamily.Serif
                )
                PillTag(
                    text = tag,
                    color = if (active) Glass.Primary else Glass.Tertiary
                )
            }
            Text(
                text = desc,
                fontSize = 11.5.sp,
                lineHeight = 19.sp,
                color = Glass.OnSurfaceVariant,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}
