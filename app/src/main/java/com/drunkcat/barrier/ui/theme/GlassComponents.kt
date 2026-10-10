package com.drunkcat.barrier.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 六芒星魔法阵。动态特效版。
 *
 * 动画层：
 * 1. 外圈金环 + 紫环（静态基准）
 * 2. 外圈流光：两道弧光沿外圈反向扫描
 * 3. 8 个符文节点：随外圈旋转，逐个闪烁
 * 4. 主六芒星：金三角 + 红三角，整体缓慢旋转
 * 5. 内层小六芒星：反向旋转
 * 6. 中心双环 + 核点：脉冲呼吸
 *
 * @param rotation 外部传入的整体旋转（来自 Header 的 60 秒旋转）
 * @param scale 外部传入的整体缩放（来自 Header 的脉冲）
 */
@Composable
fun MagicCircle(
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    scale: Float = 1f
) {
    val t = rememberInfiniteTransition(label = "magic")

    // 外圈符文层：顺时针 24 秒
    val outerSpin by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "outerSpin"
    )
    // 内层六芒星：逆时针 18 秒
    val innerSpin by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "innerSpin"
    )
    // 流光扫描：4 秒一圈
    val sweep by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "sweep"
    )
    // 符文呼吸：2.5 秒
    val shimmer by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "shimmer"
    )
    // 中心脉冲：1.8 秒往返
    val corePulse by t.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation
            scaleX = scale
            scaleY = scale
        }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f
        drawMagicCircle(Offset(cx, cy), r, outerSpin, innerSpin, sweep, shimmer, corePulse)
    }
}

private fun DrawScope.drawMagicCircle(
    center: Offset,
    r: Float,
    outerSpin: Float,
    innerSpin: Float,
    sweep: Float,
    shimmer: Float,
    corePulse: Float
) {
    val cx = center.x
    val cy = center.y

    // ========== 静态双环 ==========
    drawCircle(
        color = Color(0x88FFC864),
        radius = r * 0.96f, center = center,
        style = Stroke(width = r * 0.014f)
    )
    drawCircle(
        color = Color(0x59A064FF),
        radius = r * 0.89f, center = center,
        style = Stroke(width = r * 0.010f)
    )

    // ========== 外圈流光：两道弧光反向扫描 ==========
    val ringSize = Size(r * 0.96f * 2, r * 0.96f * 2)
    val ringTopLeft = Offset(cx - r * 0.96f, cy - r * 0.96f)

    // 金色流光：外层光晕 + 主光 + 拖尾
    drawArc(
        color = Color(0x33FFD264),
        startAngle = sweep * 360f - 50f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = ringTopLeft,
        size = ringSize,
        style = Stroke(width = r * 0.048f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0x66FFD264),
        startAngle = sweep * 360f - 35f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = ringTopLeft,
        size = ringSize,
        style = Stroke(width = r * 0.032f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFFFFD264),
        startAngle = sweep * 360f,
        sweepAngle = 48f,
        useCenter = false,
        topLeft = ringTopLeft,
        size = ringSize,
        style = Stroke(width = r * 0.026f, cap = StrokeCap.Round)
    )
    // 扫描头亮点
    val headAngle = Math.toRadians((sweep * 360f).toDouble())
    val hx = cx + r * 0.96f * cos(headAngle).toFloat()
    val hy = cy + r * 0.96f * sin(headAngle).toFloat()
    drawCircle(color = Color(0x66FFD264), radius = r * 0.030f, center = Offset(hx, hy))
    drawCircle(color = Color(0xFFFFF0C0), radius = r * 0.014f, center = Offset(hx, hy))
    // 洋红流光：主光 + 拖尾（反向）
    drawArc(
        color = Color(0x44FF4678),
        startAngle = -sweep * 360f + 200f,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = ringTopLeft,
        size = ringSize,
        style = Stroke(width = r * 0.026f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFFFF4678),
        startAngle = -sweep * 360f + 180f,
        sweepAngle = 40f,
        useCenter = false,
        topLeft = ringTopLeft,
        size = ringSize,
        style = Stroke(width = r * 0.022f, cap = StrokeCap.Round)
    )

    // ========== 8 个符文节点：随外圈旋转 + 逐个闪烁 + 光晕 ==========
    for (i in 0 until 8) {
        val ang = (i * 45.0 + outerSpin - 90.0) * PI / 180.0
        val cosA = cos(ang).toFloat()
        val sinA = sin(ang).toFloat()

        // 相位错开 1/8，形成"逐个点亮"效果
        val phase = (shimmer + i / 8f) % 1f
        val brightness = 0.25f + 0.75f * abs(sin(phase * PI).toFloat())

        val nx = cx + r * 0.96f * cosA
        val ny = cy + r * 0.96f * sinA

        // 外层光晕（更淡、更小）
        drawCircle(
            color = Color(0xFFFFC864).copy(alpha = (0.14f * brightness).coerceIn(0f, 1f)),
            radius = r * 0.030f * brightness,
            center = Offset(nx, ny)
        )
        // 节点本体（缩小，贴近设计稿的精致感）
        drawCircle(
            color = Color(0xFFFFE09C).copy(alpha = brightness.coerceIn(0f, 1f)),
            radius = r * 0.011f * (0.8f + 0.4f * brightness),
            center = Offset(nx, ny)
        )

        // 短刻线
        val i1 = r * 0.90f
        val i2 = r * 0.84f
        drawLine(
            color = Color(0xFFFFC864).copy(alpha = (0.55f * brightness).coerceIn(0f, 1f)),
            start = Offset(cx + i1 * cosA, cy + i1 * sinA),
            end = Offset(cx + i2 * cosA, cy + i2 * sinA),
            strokeWidth = r * 0.012f,
            cap = StrokeCap.Round
        )
    }

    // ========== 星尘粒子：随外圈旋转的细小光点 ==========
    for (i in 0 until 16) {
        val baseAng = i * 22.5 + outerSpin * 1.6
        val a = Math.toRadians(baseAng)
        // 半径在 0.65~0.85 之间抖动
        val radiusFactor = 0.65f + 0.20f * ((i * 7 % 10) / 10f)
        val px = cx + r * radiusFactor * cos(a).toFloat()
        val py = cy + r * radiusFactor * sin(a).toFloat()

        // 粒子闪烁
        val pPhase = (shimmer * 2 + i / 16f) % 1f
        val pAlpha = 0.15f + 0.55f * abs(sin(pPhase * PI).toFloat())

        drawCircle(
            color = if (i % 3 == 0) Color(0xFFFFD264).copy(alpha = pAlpha * 0.7f)
            else Color(0xFFB08CFF).copy(alpha = pAlpha * 0.5f),
            radius = r * 0.004f,
            center = Offset(px, py)
        )
    }

    // ========== 主六芒星：顺时针缓慢旋转 ==========
    val starAngle = Math.toRadians(outerSpin.toDouble())
    val star = r * 0.74f

    // 正三角（金）：外发光 + 实线
    val triUp = Path().apply {
        for (k in 0 until 3) {
            val a = starAngle - PI / 2 + k * 2 * PI / 3
            val px = cx + star * cos(a).toFloat()
            val py = cy + star * sin(a).toFloat()
            if (k == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(triUp, color = Color(0x33FFD264), style = Stroke(width = r * 0.045f))
    drawPath(triUp, color = Color(0x66FFD264), style = Stroke(width = r * 0.028f))
    drawPath(triUp, color = Color(0xE6FFD264), style = Stroke(width = r * 0.016f))

    // 倒三角（红）：外发光 + 实线
    val triDown = Path().apply {
        for (k in 0 until 3) {
            val a = starAngle + PI / 2 + k * 2 * PI / 3
            val px = cx + star * cos(a).toFloat()
            val py = cy + star * sin(a).toFloat()
            if (k == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(triDown, color = Color(0x33FF4678), style = Stroke(width = r * 0.045f))
    drawPath(triDown, color = Color(0x66FF4678), style = Stroke(width = r * 0.028f))
    drawPath(triDown, color = Color(0xD9FF4678), style = Stroke(width = r * 0.016f))

    // ========== 内层小六芒星：反向旋转 ==========
    val innerStar = r * 0.42f
    val innerAngle = -Math.toRadians(innerSpin.toDouble())

    val innerUp = Path().apply {
        for (k in 0 until 3) {
            val a = innerAngle - PI / 2 + k * 2 * PI / 3
            val px = cx + innerStar * cos(a).toFloat()
            val py = cy + innerStar * sin(a).toFloat()
            if (k == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(innerUp, color = Color(0x22FFD264), style = Stroke(width = r * 0.020f))
    drawPath(innerUp, color = Color(0x88FFD264), style = Stroke(width = r * 0.009f))

    val innerDown = Path().apply {
        for (k in 0 until 3) {
            val a = innerAngle + PI / 2 + k * 2 * PI / 3
            val px = cx + innerStar * cos(a).toFloat()
            val py = cy + innerStar * sin(a).toFloat()
            if (k == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(innerDown, color = Color(0x22FF5082), style = Stroke(width = r * 0.020f))
    drawPath(innerDown, color = Color(0x80FF5082), style = Stroke(width = r * 0.009f))

    // ========== 中心双环：脉冲呼吸 ==========
    drawCircle(
        color = Color(0xFFFFC864).copy(alpha = (0.45f * corePulse).coerceIn(0f, 1f)),
        radius = r * 0.22f * corePulse,
        center = center,
        style = Stroke(width = r * 0.010f)
    )
    drawCircle(
        color = Color(0xFFA064FF).copy(alpha = (0.45f * corePulse).coerceIn(0f, 1f)),
        radius = r * 0.13f * corePulse,
        center = center,
        style = Stroke(width = r * 0.008f)
    )

    // ========== 中心核点：呼吸 + 外光晕 ==========
    drawCircle(
        color = Color(0x33FF5A8C).copy(alpha = (0.5f * corePulse).coerceIn(0f, 1f)),
        radius = r * 0.075f * corePulse,
        center = center
    )
    drawCircle(
        color = Color(0xFFFF8CB4).copy(alpha = (0.9f * corePulse).coerceIn(0f, 1f)),
        radius = r * 0.030f * corePulse,
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
