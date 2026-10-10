package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 卡片标题：金色 + 前置发光红点。
 */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(Glass.Primary, CircleShape)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            letterSpacing = 2.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Glass.Tertiary,
            fontFamily = FontFamily.Serif
        )
    }
}

/**
 * 胶囊状态标签。
 */
@Composable
fun PillTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Glass.Primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            letterSpacing = 1.5.sp,
            color = color,
            fontFamily = FontFamily.Serif
        )
    }
}

/**
 * 主按钮：红粉渐变 + 阴影。
 */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) Brush.linearGradient(
                    listOf(Color(0xFFFF3D6E), Color(0xFFB01048))
                ) else Brush.linearGradient(
                    listOf(Color(0x55FF3D6E), Color(0x55B01048))
                )
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif
        )
    }
}

/**
 * 次按钮：紫透底 + 描边。
 */
@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1A8C5ADC))
            .border(1.dp, Color(0x38AA82FF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Glass.OnBg,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif
        )
    }
}

/**
 * 幽灵按钮：透明底 + 红描边。
 */
@Composable
fun GlassGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x66FF4678), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Glass.Primary,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif
        )
    }
}

/**
 * 检测项行：左标签 右状态。
 */
@Composable
fun CheckItem(
    label: String,
    status: String,
    ok: Boolean,
    modifier: Modifier = Modifier,
    icon: UiIconType? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                UiIcon(
                    type = icon,
                    size = 16.dp,
                    tint = Glass.Secondary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                fontSize = 12.5.sp,
                color = Glass.OnBg,
                fontFamily = FontFamily.Serif
            )
        }
        Text(
            text = status,
            fontSize = 11.5.sp,
            color = if (ok) Color(0xFF6DFFB0) else Glass.Primary,
            fontFamily = FontFamily.Serif
        )
    }
}
