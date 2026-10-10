package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 首次启动引导。
 *
 * 三步：
 * 1. 欢迎
 * 2. 开启无障碍（核心，否则自动检测无效）
 * 3. 设置 PIN
 */
@Composable
fun GlassOnboarding(
    step: Int,
    accessibilityOn: Boolean,
    hasPin: Boolean,
    onOpenAccessibility: () -> Unit,
    onSetPin: () -> Unit,
    onFinish: () -> Unit
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
            GlassHeader(statusText = "结界初启 · ${step + 1} / 3")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 13.dp)
            ) {
                when (step) {
                    0 -> WelcomeStep(onNext = onFinish)
                    1 -> AccessibilityStep(
                        enabled = accessibilityOn,
                        onOpen = onOpenAccessibility,
                        onNext = onFinish
                    )
                    else -> PinStep(
                        hasPin = hasPin,
                        onSetPin = onSetPin,
                        onFinish = onFinish
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = Color(0x59FF4678)) {
        CardTitle("欢迎来到醉猫结界")

        Text(
            text = "这个应用做一件事：\n在你喝多的时候，帮你拦住那些第二天会后悔的操作。",
            fontSize = 14.sp,
            lineHeight = 24.sp,
            color = Glass.OnBg,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text(
            text = "它不需要你手动开启——后台会自动检测。\n一旦发现风险，先给你 30 秒预警，\n你若没反应，它自动上锁。",
            fontSize = 13.sp,
            lineHeight = 22.sp,
            color = Glass.OnSurfaceVariant,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        GlassPrimaryButton(text = "开始配置", onClick = onNext)
    }
}

@Composable
private fun AccessibilityStep(
    enabled: Boolean,
    onOpen: () -> Unit,
    onNext: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        CardTitle("开启无障碍检测")

        Text(
            text = if (enabled) "✅ 已开启，可以继续。"
            else "⚠️ 必须开启，否则无法检测前台应用，自动保护将失效。",
            fontSize = 13.sp,
            lineHeight = 22.sp,
            color = if (enabled) Color(0xFF6DFFB0) else Glass.Primary,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Text(
            text = "本服务仅检测前台应用包名，\n不读取任何输入内容，不联网。",
            fontSize = 12.sp,
            lineHeight = 20.sp,
            color = Glass.OnSurfaceVariant,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        if (!enabled) {
            GlassPrimaryButton(text = "打开无障碍设置", onClick = onOpen)
            Spacer(Modifier.height(9.dp))
        }
        GlassSecondaryButton(
            text = if (enabled) "下一步" else "跳过（不推荐）",
            onClick = onNext
        )
    }
}

@Composable
private fun PinStep(
    hasPin: Boolean,
    onSetPin: () -> Unit,
    onFinish: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        CardTitle("设置清醒试炼 PIN")

        Text(
            text = if (hasPin) "✅ 已设置。"
            else "用于高保护级别解锁，以及修改受保护设置。",
            fontSize = 13.sp,
            lineHeight = 22.sp,
            color = if (hasPin) Color(0xFF6DFFB0) else Glass.OnSurfaceVariant,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        if (!hasPin) {
            GlassPrimaryButton(text = "设置 PIN", onClick = onSetPin)
            Spacer(Modifier.height(9.dp))
            GlassSecondaryButton(text = "稍后设置", onClick = onFinish)
        } else {
            GlassPrimaryButton(text = "完成", onClick = onFinish)
        }
    }
}
