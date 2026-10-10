package com.drunkcat.barrier.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * PIN 验证：错误 3 次触发失败回调（由上层处理冷却）。
 */
@Composable
fun PinVerifyScreen(
    expected: String,
    onSuccess: () -> Unit,
    onFailure: (attempts: Int) -> Unit,
    onCancel: () -> Unit
) {
    var attempts by remember { mutableStateOf(0) }

    PinPad(
        title = "输入 PIN 解锁（错误 ${attempts} / 3）",
        onSubmit = { input ->
            if (input == expected) {
                onSuccess()
            } else {
                attempts++
                if (attempts >= 3) {
                    onFailure(attempts)
                }
            }
        },
        onCancel = onCancel
    )
}
