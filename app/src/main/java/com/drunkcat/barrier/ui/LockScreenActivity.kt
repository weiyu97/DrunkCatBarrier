package com.drunkcat.barrier.ui

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
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
import com.drunkcat.barrier.data.ContractTime
import com.drunkcat.barrier.data.Prefs
import com.drunkcat.barrier.service.AppWatchService
import com.drunkcat.barrier.ui.theme.CardTitle
import com.drunkcat.barrier.ui.theme.DrunkCatTheme
import com.drunkcat.barrier.ui.theme.Glass
import com.drunkcat.barrier.ui.theme.GlassCard
import com.drunkcat.barrier.ui.theme.GlassPrimaryButton
import com.drunkcat.barrier.ui.theme.GlassSecondaryButton
import com.drunkcat.barrier.ui.theme.MagicCircle
import com.drunkcat.barrier.ui.theme.ThemeMode
import com.drunkcat.barrier.verify.CognitiveVerifier
import kotlinx.coroutines.delay

/**
 * 酒后锁屏界面。
 *
 * 保护期间可主动展示此界面，作为「软锁屏」：
 * - 全屏不可返回（拦截返回键）
 * - 只有两条出口：通过清醒验证，或紧急呼叫
 * - 显示契约到期时间
 *
 * 定位：替代或补充无障碍拦截，减少对无障碍服务的依赖。
 * 注意：这不是系统锁屏，无法阻止用户强制停止应用或重启设备。
 */
class LockScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrunkCatTheme(mode = ThemeMode.GLASS) {
                LockScreenContent(
                    onUnlock = { finish() },
                    onEmergency = {
                        try {
                            startActivity(
                                android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:110")
                                }
                            )
                        } catch (e: Exception) { }
                    }
                )
            }
        }
    }

    /** 拦截返回键，强制走验证出口 */
    @Deprecated("Deprecated in API 33")
    override fun onBackPressed() {
        // 不响应返回，必须通过验证或紧急呼叫离开
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 拦截返回键（部分设备走此路径）
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun LockScreenContent(
    onUnlock: () -> Unit,
    onEmergency: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var remaining by remember { mutableLongStateOf(0L) }
    var untilLabel by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val prefs = Prefs(context)
        val until = prefs.getProtectionUntil()
        untilLabel = until
        while (true) {
            remaining = (until - System.currentTimeMillis()).coerceAtLeast(0)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05030C), Color(0xFF0C0618), Color(0xFF080412))
                )
            )
    ) {
        // 背景魔法阵
        MagicCircle(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "结界封锁中",
                fontSize = 28.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "DRUNKCAT BARRIER ACTIVE",
                fontSize = 10.sp,
                letterSpacing = 5.sp,
                color = Glass.OnSurfaceVariant,
                fontFamily = FontFamily.Serif
            )

            Spacer(Modifier.height(28.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = Color(0x99FF4678)) {
                if (untilLabel > 0) {
                    Text(
                        text = "将在 ${ContractTime.label(untilLabel)} 自动解除",
                        fontSize = 13.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = ContractTime.remaining(untilLabel),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Glass.Primary,
                        fontFamily = FontFamily.Serif
                    )
                } else {
                    Text(
                        text = "保护进行中",
                        fontSize = 14.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif
                    )
                }

                Spacer(Modifier.height(18.dp))

                GlassPrimaryButton(
                    text = "通过清醒验证解除",
                    onClick = {
                        // 转交给 VerifyActivity 处理，通过后关闭锁屏
                        try {
                            context.startActivity(
                                android.content.Intent(context, VerifyActivity::class.java).apply {
                                    putExtra(VerifyActivity.EXTRA_TARGET_PACKAGE, "")
                                }
                            )
                        } catch (e: Exception) { }
                        onUnlock()
                    }
                )

                Spacer(Modifier.height(9.dp))

                GlassSecondaryButton(
                    text = "紧急呼叫",
                    onClick = onEmergency
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "所有记忆仅留存于本机",
                fontSize = 11.sp,
                color = Glass.OnSurfaceVariant,
                fontFamily = FontFamily.Serif
            )
        }
    }
}
