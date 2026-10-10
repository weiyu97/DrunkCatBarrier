package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.report.InterceptLog
import com.drunkcat.barrier.track.TrackStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 复盘报告页。
 *
 * 展示：
 * - 统计卡片：拦截次数、通过次数、失败次数、高风险时段
 * - 拦截时间线
 * - 轨迹（坐标 + 时间）
 */
@Composable
fun GlassReviewScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val logStore = remember { InterceptLog(context) }
    val trackStore = remember { TrackStore(context) }

    var entries by remember { mutableStateOf<List<InterceptLog.Entry>>(emptyList()) }
    var trackPoints by remember { mutableStateOf<List<com.drunkcat.barrier.track.TrackPoint>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        // 读取今天和昨天的记录
        val today = logStore.readDay(now)
        val yesterday = logStore.readDay(now - 24 * 3600 * 1000L)
        entries = (yesterday + today).sortedByDescending { it.timestamp }

        val tToday = trackStore.readDay(now)
        val tYesterday = trackStore.readDay(now - 24 * 3600 * 1000L)
        trackPoints = (tYesterday + tToday).sortedBy { it.timestamp }

        loaded = true
    }

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

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
            GlassHeader(statusText = "暗夜复盘")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 13.dp)
            ) {
                // ---- 统计 ----
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("昨夜结界记录")

                    val total = entries.size
                    val passed = entries.count { it.passed }
                    val failed = entries.count { !it.passed }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox("拦截", total.toString(), Modifier.weight(1f))
                        StatBox("通过", passed.toString(), Modifier.weight(1f), ok = true)
                        StatBox("失败", failed.toString(), Modifier.weight(1f))
                    }

                    if (total == 0) {
                        Text(
                            text = "昨夜没有触发拦截。要么你没喝，要么你克制住了。",
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp,
                            color = Glass.OnSurfaceVariant,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ---- 拦截时间线 ----
                if (entries.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        CardTitle("拦截时间线")
                        entries.take(20).forEach { e ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeFmt.format(Date(e.timestamp)),
                                    fontSize = 12.sp,
                                    color = Glass.OnSurfaceVariant,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = shortName(e.packageName),
                                    fontSize = 12.5.sp,
                                    color = Glass.OnBg,
                                    fontFamily = FontFamily.Serif,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                                )
                                Text(
                                    text = if (e.passed) "通过" else "拦截",
                                    fontSize = 11.5.sp,
                                    color = if (e.passed) Color(0xFF6DFFB0) else Glass.Primary,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ---- 轨迹 ----
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("暗夜行迹")
                    if (trackPoints.isEmpty()) {
                        Text(
                            text = "昨夜没有轨迹记录。\n（保护未开启，或未授予定位权限）",
                            fontSize = 12.5.sp,
                            lineHeight = 20.sp,
                            color = Glass.OnSurfaceVariant,
                            fontFamily = FontFamily.Serif
                        )
                    } else {
                        // 展示停留点（简化：按时间列出首尾若干点）
                        val shown = trackPoints.let {
                            if (it.size <= 10) it
                            else it.take(5) + it.takeLast(5)
                        }
                        shown.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = timeFmt.format(Date(p.timestamp)),
                                    fontSize = 12.sp,
                                    color = Glass.OnSurfaceVariant,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = p.placeName ?: "%.4f, %.4f".format(p.latitude, p.longitude),
                                    fontSize = 12.sp,
                                    color = Glass.OnBg,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                        if (trackPoints.size > 10) {
                            Text(
                                text = "（共 ${trackPoints.size} 个轨迹点）",
                                fontSize = 11.sp,
                                color = Glass.OnSurfaceVariant,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    ok: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (ok) Color(0xFF6DFFB0) else Glass.Primary,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Glass.OnSurfaceVariant,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** 从包名提取简短名称 */
private fun shortName(pkg: String): String {
    val parts = pkg.split(".")
    return parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
}
