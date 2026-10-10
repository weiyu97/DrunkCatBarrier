package com.drunkcat.barrier.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.service.WarningState
import com.drunkcat.barrier.ui.theme.Glass
import com.drunkcat.barrier.ui.theme.GlassCard
import com.drunkcat.barrier.ui.theme.GlassPrimaryButton
import com.drunkcat.barrier.ui.theme.GlassSecondaryButton
import com.drunkcat.barrier.ui.theme.CardTitle
import com.drunkcat.barrier.ui.theme.DrunkCatTheme
import com.drunkcat.barrier.ui.theme.ThemeMode
import kotlinx.coroutines.delay

/**
 * 风险预警界面。
 *
 * 检测到醉酒风险时弹出，显示 30 秒倒计时：
 * - 点「我没醉」-> 取消预警，清空风险信号
 * - 倒计时结束 -> 自动开启保护
 */
class WarningActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 兜底：若预警状态未初始化（如通过通知或 adb 直接打开），
        // 则以当前时刻为起点启动一次预警倒计时。
        if (!WarningState.active) {
            WarningState.start()
        }

        // 震动提醒，确保用户感知
        vibrate()

        setContent {
            DrunkCatTheme(mode = ThemeMode.GLASS) {
                WarningScreen(
                    onCancel = {
                        WarningState.cancel()
                        finish()
                    },
                    onTimeout = {
                        WarningState.complete()
                        finish()
                    }
                )
            }
        }
    }

    /** 震动提醒，确保用户感知预警 */
    private fun vibrate() {
        try {
            val vb = getSystemService(android.content.Context.VIBRATOR_SERVICE)
                as? android.os.Vibrator ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vb.vibrate(
                    android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 400, 200, 400),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vb.vibrate(longArrayOf(0, 400, 200, 400), -1)
            }
        } catch (e: Exception) { }
    }
}

@Composable
private fun WarningScreen(
    onCancel: () -> Unit,
    onTimeout: () -> Unit
) {
    var remaining by remember { mutableLongStateOf(WarningState.remainingMs()) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(200)
            remaining = WarningState.remainingMs()
        }
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05030C), Color(0xFF0C0618), Color(0xFF080412))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = Color(0x99FF4678)) {
                CardTitle("清醒预警")

                Text(
                    text = "检测到你可能喝多了。",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Glass.OnBg,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "${(remaining / 1000.0).let { (it * 10).toInt() / 10.0 }} 秒后自动开启保护",
                    fontSize = 14.sp,
                    color = Glass.Primary,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "如果你还清醒，点击下方按钮取消。\n如果确实喝多了，什么都不做即可。",
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = Glass.OnSurfaceVariant,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                GlassPrimaryButton(
                    text = "我没醉，取消预警",
                    onClick = onCancel
                )
                Spacer(Modifier.height(9.dp))
                GlassSecondaryButton(
                    text = "确认喝多了，立即锁定",
                    onClick = onTimeout
                )
            }
        }
    }
}
