package com.drunkcat.barrier.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 琉璃风 PIN 验证：错误 3 次触发失败回调。
 */
@Composable
fun GlassPinVerifyScreen(
    expected: String,
    onSuccess: () -> Unit,
    onFailure: (attempts: Int) -> Unit,
    onCancel: () -> Unit
) {
    var attempts by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassPinScreen(
        title = "输入 PIN 解锁",
        subtitle = error ?: "已失败 $attempts / 3 次",
        onSubmit = { input ->
            if (input == expected) {
                onSuccess()
            } else {
                attempts++
                error = "PIN 错误（$attempts / 3）"
                if (attempts >= 3) {
                    onFailure(attempts)
                }
            }
        },
        onCancel = onCancel
    )
}
