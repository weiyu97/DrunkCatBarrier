package com.drunkcat.barrier.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 琉璃风 PIN 设置：两次输入一致才生效。
 */
@Composable
fun GlassPinSetScreen(
    onSet: (String) -> Unit,
    onCancel: () -> Unit
) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (first == null) {
        GlassPinScreen(
            title = "设置清醒试炼 PIN",
            subtitle = error ?: "两次输入一致后生效。此 PIN 用于高保护级别解锁。",
            onSubmit = { first = it; error = null },
            onCancel = onCancel
        )
    } else {
        GlassPinScreen(
            title = "再次输入确认",
            subtitle = "重复刚才的 6 位 PIN",
            onSubmit = { second ->
                if (second == first) {
                    onSet(second)
                } else {
                    error = "两次输入不一致，请重新设置"
                    first = null
                }
            },
            onCancel = onCancel
        )
    }
}
