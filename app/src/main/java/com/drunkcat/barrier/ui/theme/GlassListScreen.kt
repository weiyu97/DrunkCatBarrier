package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.drunkcat.barrier.data.InstalledApps

/**
 * 琉璃风应用列表选择页（用于锁定名单 / 白名单）。
 */
@Composable
fun GlassAppListScreen(
    title: String,
    subtitle: String,
    apps: List<InstalledApps.AppInfo>,
    initialSelected: Set<String>,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf(initialSelected) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF05030C), Color(0xFF0C0618), Color(0xFF080412))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassHeader(statusText = title)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 13.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle(title)
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "已选 ${selected.size} 个",
                        fontSize = 12.sp,
                        color = Glass.Primary,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(apps) { app ->
                        val checked = selected.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (checked) Color(0x1AFF3264) else Color(0x0AFFFFFF),
                                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selected = if (checked) selected - app.packageName
                                    else selected + app.packageName
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = app.label,
                                    fontSize = 14.sp,
                                    color = Glass.OnBg,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 10.sp,
                                    color = Glass.OnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassSecondaryButton(text = "取消", onClick = onBack)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassPrimaryButton(text = "保存", onClick = { onSave(selected) })
                    }
                }
            }
        }
    }
}
