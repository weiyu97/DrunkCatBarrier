package com.drunkcat.barrier.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
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
import androidx.compose.runtime.mutableStateOf
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
import com.drunkcat.barrier.report.InterceptLog
import com.drunkcat.barrier.service.AppWatchService
import com.drunkcat.barrier.ui.theme.CardTitle
import com.drunkcat.barrier.ui.theme.DrunkCatTheme
import com.drunkcat.barrier.ui.theme.Glass
import com.drunkcat.barrier.ui.theme.GlassCard
import com.drunkcat.barrier.ui.theme.GlassPrimaryButton
import com.drunkcat.barrier.ui.theme.GlassSecondaryButton
import com.drunkcat.barrier.ui.theme.MagicCircle
import com.drunkcat.barrier.ui.theme.ThemeMode
import com.drunkcat.barrier.ui.theme.GlassFailureScreen
import com.drunkcat.barrier.ui.theme.GlassVerificationFlow
import com.drunkcat.barrier.verify.CognitiveVerifier
import com.drunkcat.barrier.verify.VerifyDifficulty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /** 是否已通过验证；未通过就关闭时必须把结界遮罩还回来 */
    private var unlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 必须声明可在系统锁屏之上显示，否则从结界遮罩点击进入时，
        // Activity 会因 canShowWhenLocked=false 被 keyguard 挡在身后不渲染（表现为"点了没反应"）。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val attempted = intent.getStringExtra(EXTRA_ATTEMPTED_PACKAGE) ?: ""
        // 拦截系统返回手势（Android 13+）：返回手势走 OnBackInvokedCallback，
        // 旧的 onBackPressed / onKeyDown(BACK) 已无法拦截，会导致锁屏被一滑即关。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                // 消费返回手势，必须通过验证或紧急呼叫才能离开
            }
        }
        setContent {
            DrunkCatTheme(mode = ThemeMode.GLASS) {
                LockScreenContent(
                    attemptedPackage = attempted,
                    onUnlock = {
                        // 验证通过：取消预警（避免 30 秒后误开启整晚保护），
                        // 撤掉悬浮锁屏层，再关闭 Activity
                        unlocked = true
                        AppWatchService.cancelWarning()
                        AppWatchService.dismissOverlay()
                        finish()
                    },
                    onEmergency = {
                        // 先开临时放行窗，避免锁屏反复拦截拨号界面
                        AppWatchService.grantEmergencyAccess(60_000L)
                        try {
                            startActivity(
                                android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:110")
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (e: Exception) { }
                    },
                    onEmergencyBypass = {
                        CoroutineScope(Dispatchers.IO).launch {
                            InterceptLog.logEmergencyBypass(this@LockScreenActivity)
                        }
                        AppWatchService.grantTempAccess(
                            attempted,
                            5 * 60 * 1000L,
                            2 * 60 * 1000L
                        )
                        unlocked = true
                        AppWatchService.dismissOverlay()
                        finish()
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

    override fun onDestroy() {
        super.onDestroy()
        // 未解锁就被关掉（被系统回收、手势关掉等）→ 把结界遮罩还回来，避免结界被绕过
        if (!unlocked) AppWatchService.restoreOverlay()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 拦截返回键（部分设备走此路径）
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_ATTEMPTED_PACKAGE = "attempted_package"
    }
}

@Composable
internal fun LockScreenContent(
    attemptedPackage: String,
    onUnlock: () -> Unit,
    onEmergency: () -> Unit,
    onEmergencyBypass: () -> Unit
) {
    var mode by remember { mutableStateOf("lock") }
    var failReason by remember { mutableStateOf("") }

    when (mode) {
        "verify" -> {
            GlassVerificationFlow(
                verifier = CognitiveVerifier(),
                difficulty = VerifyDifficulty.NORMAL,
                onResult = { passed, reason ->
                    if (passed) {
                        // 给予被尝试应用临时使用窗口，再解除锁屏
                        AppWatchService.grantTempAccess(
                            attemptedPackage,
                            5 * 60 * 1000L,
                            2 * 60 * 1000L
                        )
                        onUnlock()
                    } else {
                        failReason = reason
                        mode = "fail"
                    }
                }
            )
        }
        "fail" -> {
            GlassFailureScreen(
                reason = failReason,
                failureCount = 0,
                cooldownMs = 0,
                onDismiss = { mode = "lock" },
                onEmergencyBypass = onEmergencyBypass
            )
        }
        else -> {
            LockScreenBody(
                onVerify = { mode = "verify" },
                onEmergency = onEmergency
            )
        }
    }
}

@Composable
private fun LockScreenBody(
    onVerify: () -> Unit,
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
                    onClick = onVerify
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
