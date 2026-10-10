package com.drunkcat.barrier.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset

/**
 * 琉璃风顶部品牌区。
 *
 * 规则：
 * 1. 猫头（Logo）圆心 = 魔法阵圆心。两者放在同一共享容器内，Logo 居中于容器。
 * 2. 仅魔法阵与辉光围绕容器中心循环脉冲缩放 + 旋转，Logo 全程固定不缩放。
 * 3. 缩放围绕容器中心（= Logo 圆心），因此圆心不位移。
 * 4. 魔法阵可溢出屏幕被裁切，不做尺寸限制。
 * 5. 背景、文字、UI 元素位置固定。
 */

// 共享容器尺寸：魔法阵填满该容器，Logo 居中
private val STAGE_SIZE = 460.dp
// 容器在 Header 内的垂直偏移（决定圆心位置）
private val STAGE_OFFSET_Y = (-30).dp
// Header 可见背景高度
private val HEADER_HEIGHT = 260.dp
// Logo 尺寸（略缩，让魔法阵结构更清晰）
private val LOGO_SIZE = 84.dp

@Composable
fun GlassHeader(
    modifier: Modifier = Modifier,
    statusText: String = "暗之契约生效中"
) {
    // ---- 魔法阵旋转：60 秒匀速一圈 ----
    val spin = rememberInfiniteTransition(label = "spin")
    val rotation by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // ---- 魔法阵脉冲：循环放大缩小（0.72 -> 1.28），允许溢出 ----
    val pulse = rememberInfiniteTransition(label = "pulse")
    val circleScale by pulse.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // ---- 辉光强度：与脉冲同步 ----
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E0A35), Glass.Bg)
                )
            )
    ) {
        // ========== 共享舞台：魔法阵 + Logo 共用同一圆心 ==========
        // 该 Box 的几何中心即为魔法阵圆心，也是 Logo 圆心。
        // 缩放围绕此中心进行，因此圆心永不位移。
        Box(
            modifier = Modifier
                .size(STAGE_SIZE)
                .align(Alignment.TopCenter)
                .offset(y = STAGE_OFFSET_Y)
        ) {
            // ---- 层 A：外圈辉光（循环明暗 + 随脉冲同步缩放） ----
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin.Center
                        scaleX = circleScale
                        scaleY = circleScale
                        alpha = glowAlpha
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x55FF4678),  // 洋红
                                Color(0x33FFD264),  // 金
                                Color(0x11A064FF),  // 紫
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // ---- 层 B：魔法阵（脉冲缩放 + 整体旋转，内部含多层动态特效） ----
            MagicCircle(
                modifier = Modifier.fillMaxSize(),
                rotation = rotation,
                scale = circleScale
            )

            // ---- 层 C：猫头 Logo（固定居中，不参与缩放） ----
            BrandLogo(
                modifier = Modifier.align(Alignment.Center),
                size = LOGO_SIZE
            )

            // ---- 层 D：品牌文字（固定在 Logo 下方，不参与缩放） ----
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = LOGO_SIZE / 2 + 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "醉猫结界",
                    fontSize = 25.sp,
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xCCFF4678),
                            offset = Offset.Zero,
                            blurRadius = 24f
                        )
                    )
                )
                Text(
                    text = "DRUNKCAT BARRIER",
                    fontSize = 10.sp,
                    letterSpacing = 5.sp,
                    color = Glass.OnSurfaceVariant,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // ========== 右上角状态文字（固定不动） ==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 22.dp, end = 22.dp)
        ) {
            Text(
                text = statusText,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = Glass.OnSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
