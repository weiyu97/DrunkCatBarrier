package com.drunkcat.barrier.ui

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.data.Prefs
import com.drunkcat.barrier.report.InterceptLog
import com.drunkcat.barrier.service.AppWatchService
import com.drunkcat.barrier.ui.theme.CardTitle
import com.drunkcat.barrier.ui.theme.DrunkCatTheme
import com.drunkcat.barrier.ui.theme.Glass
import com.drunkcat.barrier.ui.theme.GlassCard
import com.drunkcat.barrier.ui.theme.GlassPrimaryButton
import com.drunkcat.barrier.ui.theme.GlassVerificationFlow
import com.drunkcat.barrier.ui.theme.ThemeMode
import com.drunkcat.barrier.verify.CognitiveVerifier
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 清醒试炼界面。当被封印的应用被打开时，由 AppWatchService 唤起。
 *
 * 验证通过后：
 * - 通知 AppWatchService 给予临时使用窗口
 * - 关闭本界面，回到目标应用
 */
class VerifyActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var log: InterceptLog

    /** 拦截返回键：试炼未完成不允许退出，防止返回后直接回到被锁应用 */
    @Deprecated("Deprecated in API 33")
    override fun onBackPressed() {
        // 不响应返回。出口只有两个：验证通过，或失败页的「回到桌面」
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 拦截返回键（部分设备走此路径）
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        log = InterceptLog(this)

        val targetPkg = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.GLASS) }
            LaunchedEffect(Unit) {
                themeMode = try {
                    ThemeMode.valueOf(prefs.getThemeMode())
                } catch (e: Exception) {
                    ThemeMode.GLASS
                }
            }
            DrunkCatTheme(mode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GlassVerificationFlow(
                        verifier = CognitiveVerifier(),
                        targetAppName = appLabel(targetPkg),
                        difficulty = com.drunkcat.barrier.verify.VerifyDifficulty.fromRisk(
                            com.drunkcat.barrier.risk.RiskMonitor.currentScore()
                        ),
                        onResult = { passed, reason ->
                            if (passed) {
                                kotlinx.coroutines.MainScope().launch {
                                    log.append(
                                        InterceptLog.Entry(
                                            timestamp = System.currentTimeMillis(),
                                            packageName = targetPkg,
                                            passed = true,
                                            reason = ""
                                        )
                                    )
                                    prefs.resetFailureState()
                                }
                                // 5 分钟该应用窗口 + 2 分钟全局放行
                                // （避免短时间内反复验证多个被锁应用）
                                AppWatchService.grantTempAccess(
                                    targetPkg,
                                    5 * 60 * 1000L,
                                    2 * 60 * 1000L
                                )
                                Toast.makeText(this, "试炼通过", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                // 记录失败 + 递增计数 + 设置冷却
                                kotlinx.coroutines.MainScope().launch {
                                    log.append(
                                        InterceptLog.Entry(
                                            timestamp = System.currentTimeMillis(),
                                            packageName = targetPkg,
                                            passed = false,
                                            reason = reason
                                        )
                                    )
                                    val count = prefs.getFailureCount() + 1
                                    prefs.setFailureCount(count)
                                    val cooldown = com.drunkcat.barrier.verify.CooldownPolicy
                                        .cooldownFor(count)
                                    if (cooldown > 0) {
                                        prefs.setCooldownUntil(
                                            System.currentTimeMillis() + cooldown
                                        )
                                    }
                                    // 切换到失败反馈页
                                    setContent {
                                        DrunkCatTheme(mode = ThemeMode.GLASS) {
                                            Surface(modifier = Modifier.fillMaxSize()) {
                                                com.drunkcat.barrier.ui.theme.GlassFailureScreen(
                                                    reason = reason,
                                                    failureCount = count,
                                                    cooldownMs = cooldown,
                                                    onDismiss = {
                                                        val home = android.content.Intent(
                                                            android.content.Intent.ACTION_MAIN
                                                        ).apply {
                                                            addCategory(android.content.Intent.CATEGORY_HOME)
                                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        startActivity(home)
                                                        finish()
                                                    },
                                                    onEmergencyBypass = {
                                                        // 记录紧急绕过事件
                                                        MainScope().launch {
                                                            InterceptLog.logEmergencyBypass(this@VerifyActivity)
                                                        }
                                                        // 放行目标应用 + 短暂全局放行
                                                        AppWatchService.grantTempAccess(
                                                            targetPkg,
                                                            5 * 60 * 1000L,
                                                            2 * 60 * 1000L
                                                        )
                                                        Toast.makeText(
                                                            this@VerifyActivity,
                                                            "紧急放行，已记入复盘",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        finish()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    /** 取应用显示名 */
    private fun appLabel(pkg: String): String {
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg.split(".").lastOrNull() ?: pkg
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }
}
