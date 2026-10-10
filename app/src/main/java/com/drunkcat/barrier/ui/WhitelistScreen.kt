package com.drunkcat.barrier.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.data.InstalledApps

/**
 * 白名单管理：
 * - 顶部显示"永远允许"的固定项（只读）
 * - 下方为可选应用列表，勾选加入自定义白名单
 */
@Composable
fun WhitelistScreen(
    apps: List<InstalledApps.AppInfo>,
    initialWhitelist: Set<String>,
    permanentPackages: Set<String>,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf(initialWhitelist - permanentPackages) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("白名单管理", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(8.dp))

        Text("永远允许（不可修改）", style = MaterialTheme.typography.titleSmall)
        Text(
            "电话、紧急呼叫、地图、打车、代驾",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Text("自定义白名单", style = MaterialTheme.typography.titleSmall)
        Text(
            "已选 ${selected.size} 个",
            fontSize = 13.sp
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(apps) { app ->
                val checked = selected.contains(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (checked) {
                                selected - app.packageName
                            } else {
                                selected + app.packageName
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(app.label, fontSize = 15.sp)
                        Text(
                            app.packageName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) { Text("取消") }
            Button(
                onClick = { onSave(selected) },
                modifier = Modifier.weight(1f)
            ) { Text("保存") }
        }
    }
}
