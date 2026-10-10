package com.drunkcat.barrier.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PIN 设置：两次输入一致才生效。
 */
@Composable
fun PinSetScreen(
    onSet: (String) -> Unit,
    onCancel: () -> Unit
) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    if (first == null) {
        PinPad(
            title = "设置 6 位 PIN（第一次）",
            onSubmit = { first = it; error = null },
            onCancel = onCancel
        )
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
        }
        PinPad(
            title = "再次输入确认",
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
