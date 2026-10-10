package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 六芒星魔法阵。完全居中对称，由 Canvas 绘制。
 *
 * 结构（由外到内）：
 * 1. 外圈金环
 * 2. 内圈紫环
 * 3. 8 个符文节点 + 短刻线
 * 4. 正三角（金）+ 倒三角（红）组成六芒星
 * 5. 内层小六芒星
 * 6. 中心双环 + 核点
 */
@Composable
fun MagicCircle(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f
        val center = Offset(cx, cy)

        drawMagicCircle(center, r)
    }
}

private fun DrawScope.drawMagicCircle(center: Offset, r: Float) {
    val cx = center.x
    val cy = center.y

    // ---- 外圈双环 ----
    drawCircle(
        color = Color(0x52FFC864),
        radius = r * 0.96f,
        center = center,
        style = Stroke(width = r * 0.012f)
    )
    drawCircle(
        color = Color(0x38A064FF),
        radius = r * 0.89f,
        center = center,
        style = Stroke(width = r * 0.008f)
    )

    // ---- 8 个符文节点（含短刻线）----
    val gold = Color(0x8CFFC864)
    val goldStroke = Color(0x59FFC864)
    for (i in 0 until 8) {
        val ang = (i * 45.0 - 90.0) * PI / 180.0
        val cosA = cos(ang).toFloat()
        val sinA = sin(ang).toFloat()
        // 节点位置：外圈上
        val nx = cx + (r * 0.96f) * cosA
        val ny = cy + (r * 0.96f) * sinA
        drawCircle(color = gold, radius = r * 0.018f, center = Offset(nx, ny))

        // 短刻线：从 0.90r 到 0.85r
        val inner1 = r * 0.90f
        val inner2 = r * 0.85f
        drawLine(
            color = goldStroke,
            start = Offset(cx + inner1 * cosA, cy + inner1 * sinA),
            end = Offset(cx + inner2 * cosA, cy + inner2 * sinA),
            strokeWidth = r * 0.009f
        )
    }

    // ---- 六芒星：正三角（金）+ 倒三角（红）----
    val star = r * 0.74f
    val triUp = Path().apply {
        moveTo(cx, cy - star)              // 顶点（上）
        lineTo(cx + star * 0.866f, cy + star * 0.5f)  // 右下
        lineTo(cx - star * 0.866f, cy + star * 0.5f)  // 左下
        close()
    }
    drawPath(triUp, color = Color(0xD1FFD264), style = Stroke(width = r * 0.016f))

    val triDown = Path().apply {
        moveTo(cx, cy + star)              // 顶点（下）
        lineTo(cx - star * 0.866f, cy - star * 0.5f)  // 左上
        lineTo(cx + star * 0.866f, cy - star * 0.5f)  // 右上
        close()
    }
    drawPath(triDown, color = Color(0xC7FF4678), style = Stroke(width = r * 0.016f))

    // ---- 内层小六芒星 ----
    val innerStar = r * 0.42f
    val innerUp = Path().apply {
        moveTo(cx, cy - innerStar)
        lineTo(cx + innerStar * 0.866f, cy + innerStar * 0.5f)
        lineTo(cx - innerStar * 0.866f, cy + innerStar * 0.5f)
        close()
    }
    drawPath(innerUp, color = Color(0x66FFD264), style = Stroke(width = r * 0.009f))

    val innerDown = Path().apply {
        moveTo(cx, cy + innerStar)
        lineTo(cx - innerStar * 0.866f, cy - innerStar * 0.5f)
        lineTo(cx + innerStar * 0.866f, cy - innerStar * 0.5f)
        close()
    }
    drawPath(innerDown, color = Color(0x61FF5082), style = Stroke(width = r * 0.009f))

    // ---- 中心双环 ----
    drawCircle(
        color = Color(0x4CFFC864),
        radius = r * 0.22f,
        center = center,
        style = Stroke(width = r * 0.010f)
    )
    drawCircle(
        color = Color(0x47A064FF),
        radius = r * 0.13f,
        center = center,
        style = Stroke(width = r * 0.008f)
    )

    // ---- 中心核点 ----
    drawCircle(
        color = Color(0xA6FF5A8C),
        radius = r * 0.030f,
        center = center
    )
}

/**
 * 琉璃风卡片：深紫渐变 + 顶部红光线 + 圆角。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentLine: Boolean = true,
    borderColor: Color = Color(0x38AA82FF),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xF71E1037), Color(0xF7100820))
                )
            )
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (accentLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.76f)
                        .height(1.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x99FF5078),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            content()
        }
    }
}
