package com.drunkcat.barrier.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 6 位数字 PIN 输入组件。
 */
@Composable
fun PinPad(
    title: String,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)

        Text(
            text = if (input.isEmpty()) "- - - - - -" else input.map { "●" }.joinToString(" "),
            fontFamily = FontFamily.Monospace,
            fontSize = 28.sp,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("清空", "0", "删除")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = {
                            when (key) {
                                "清空" -> input = ""
                                "删除" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                else -> if (input.length < 6) input += key
                            }
                        },
                        modifier = Modifier.weight(1f).padding(2.dp)
                    ) { Text(key, fontSize = 18.sp) }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("取消")
            }
            Button(
                onClick = { if (input.length == 6) onSubmit(input) },
                enabled = input.length == 6,
                modifier = Modifier.weight(1f)
            ) { Text("确认") }
        }
    }
}
