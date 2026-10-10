package com.drunkcat.barrier.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.drunkcat.barrier.data.Prefs
import com.drunkcat.barrier.report.InterceptLog
import com.drunkcat.barrier.service.AppWatchService
import com.drunkcat.barrier.ui.theme.DrunkCatTheme
import com.drunkcat.barrier.ui.theme.GlassVerificationFlow
import com.drunkcat.barrier.ui.theme.ThemeMode
import com.drunkcat.barrier.verify.CognitiveVerifier
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
                        onResult = { passed, reason ->
                            // 记录拦截日志
                            kotlinx.coroutines.MainScope().launch {
                                log.append(
                                    InterceptLog.Entry(
                                        timestamp = System.currentTimeMillis(),
                                        packageName = targetPkg,
                                        passed = passed,
                                        reason = reason
                                    )
                                )
                            }
                            if (passed) {
                                // 给予 5 分钟临时使用窗口
                                AppWatchService.grantTempAccess(targetPkg, 5 * 60 * 1000L)
                                Toast.makeText(this, "试炼通过", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this, "试炼失败：$reason", Toast.LENGTH_LONG).show()
                                // 失败后关闭被锁 App（回到桌面）
                                val home = android.content.Intent(
                                    android.content.Intent.ACTION_MAIN
                                ).apply {
                                    addCategory(android.content.Intent.CATEGORY_HOME)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(home)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }
}
