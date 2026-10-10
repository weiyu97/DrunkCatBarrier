package com.drunkcat.barrier.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.data.ContractMode

/**
 * 清醒契约仪式。
 *
 * 快速模式：勾选 + 输入"我确认"三字
 * 完整模式：逐字手写全文 + 前置拍照（Phase 1 暂不含手写/拍照，先用输入代替）
 */
@Composable
fun ContractScreen(
    mode: ContractMode,
    endTimeLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val fullText = "我确认自己现在清醒，并自愿锁定所选 App 直到 $endTimeLabel。我理解在此期间无法随意使用这些 App。"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("清醒契约", style = MaterialTheme.typography.headlineSmall)

        if (mode == ContractMode.QUICK) {
            Text(
                "我确认现在清醒，自愿锁定所选 App 直到 $endTimeLabel。",
                fontSize = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Text("我已阅读并同意", fontSize = 14.sp)
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("输入「我确认」三个字") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text("请逐字输入以下文本：", fontSize = 14.sp)
            Text(fullText, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("逐字输入契约全文") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("取消")
            }
            Button(
                onClick = {
                    val ok = if (mode == ContractMode.QUICK) {
                        checked && text.trim() == "我确认"
                    } else {
                        text.trim() == fullText
                    }
                    if (ok) onConfirm() else error = "确认文本不匹配"
                },
                modifier = Modifier.weight(1f)
            ) { Text("确认开启") }
        }
    }
}
