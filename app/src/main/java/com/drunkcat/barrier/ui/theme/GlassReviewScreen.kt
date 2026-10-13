package com.drunkcat.barrier.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drunkcat.barrier.report.InterceptLog
import com.drunkcat.barrier.risk.RiskMonitor
import com.drunkcat.barrier.risk.RiskSignal
import com.drunkcat.barrier.service.AppWatchService
import com.drunkcat.barrier.track.TrackStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 复盘报告页。
 *
 * 展示：
 * - 统计卡片：拦截次数、通过次数、失败次数、高风险时段
 * - 生命信号：心率手环数据与风险关联说明
 * - 拦截时间线
 * - 轨迹（坐标 + 时间）
 *
 * @param onBack 返回上一页。**无默认值**，强制调用方必须显式传入，
 *              避免再次出现「二级页忘记传回调 → 页面变死胡同」。
 */
@Composable
fun GlassReviewScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val logStore = remember { InterceptLog(context) }
    val trackStore = remember { TrackStore(context) }
    val prefs = remember { com.drunkcat.barrier.data.Prefs(context) }

    var entries by remember { mutableStateOf<List<InterceptLog.Entry>>(emptyList()) }
    var trackPoints by remember { mutableStateOf<List<com.drunkcat.barrier.track.TrackPoint>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var mapEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        // 读取今天和昨天的记录
        val today = logStore.readDay(now)
        val yesterday = logStore.readDay(now - 24 * 3600 * 1000L)
        entries = (yesterday + today).sortedByDescending { it.timestamp }

        val tToday = trackStore.readDay(now)
        val tYesterday = trackStore.readDay(now - 24 * 3600 * 1000L)
        trackPoints = (tYesterday + tToday).sortedBy { it.timestamp }

        // 读取地图开关
        mapEnabled = try {
            prefs.mapEnabledFlow.first()
        } catch (e: Exception) { false }

        loaded = true
    }

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeFmtFull = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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

                        // —— 24 小时拦截热力图 ——
                        val hourBuckets = remember(entries) {
                            IntArray(24).also { buckets ->
                                entries.forEach { e ->
                                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = e.timestamp }
                                    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                                    buckets[h]++
                                }
                            }
                        }
                        val maxBucket = hourBuckets.maxOrNull() ?: 0
                        var selectedHour by remember { mutableIntStateOf(-1) }

                        Spacer(Modifier.height(8.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pointerInput(entries) {
                                    detectTapGestures { offset ->
                                        val barWidth = size.width / 24f
                                        val h = (offset.x / barWidth)
                                            .toInt()
                                            .coerceIn(0, 23)
                                        selectedHour = h
                                    }
                                }
                        ) {
                            val barWidth = size.width / 24f
                            for (h in 0..23) {
                                val count = hourBuckets[h]
                                val ratio = if (maxBucket > 0) count.toFloat() / maxBucket else 0f
                                val color = when {
                                    count == 0 -> Color(0x11FFFFFF)
                                    ratio <= 0.25f -> Color(0x44FF4678)
                                    ratio <= 0.6f -> Color(0x88FF4678)
                                    else -> Glass.Primary
                                }
                                val left = h * barWidth
                                val right = (h + 1) * barWidth - 1f
                                drawRect(
                                    color = color,
                                    topLeft = Offset(left, 0f),
                                    size = Size(
                                        width = right - left,
                                        height = size.height
                                    )
                                )
                                // 选中高亮边框
                                if (selectedHour == h) {
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.7f),
                                        topLeft = Offset(left, 0f),
                                        size = Size(
                                            width = right - left,
                                            height = size.height
                                        ),
                                        style = Stroke(width = 2f)
                                    )
                                }
                            }
                        }

                        // 选中小时详情行（只在选中且有事件时显示）
                        if (selectedHour >= 0 && hourBuckets[selectedHour] > 0) {
                            Text(
                                text = "%02d:00 - %d 次拦截".format(selectedHour, hourBuckets[selectedHour]),
                                fontSize = 11.5.sp,
                                color = Glass.Primary,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // 底部时间标注
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("0", "6", "12", "18", "24").forEach { label ->
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = Glass.OnSurfaceVariant,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

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
                        // 轨迹可视化：按开关选择底图
                        Spacer(Modifier.height(10.dp))
                        if (mapEnabled) {
                            OsmTrackMap(points = trackPoints)
                        } else {
                            TrackCanvas(points = trackPoints)
                        }
                        Spacer(Modifier.height(8.dp))
                        GlassSecondaryButton(
                            text = if (mapEnabled) "切换为简化路径（不联网）"
                            else "加载地图底图（需联网）",
                            onClick = {
                                val next = !mapEnabled
                                mapEnabled = next
                                scope.launch { prefs.setMapEnabled(next) }
                            }
                        )
                        if (mapEnabled) {
                            Spacer(Modifier.height(6.dp))
                            GlassSecondaryButton(
                                text = "切换瓦片源",
                                onClick = {
                                    val p = context.getSharedPreferences(
                                        "map_prefs", android.content.Context.MODE_PRIVATE
                                    )
                                    val cur = p.getString("tile_source", "amap")
                                    val options = tileSourceOptions()
                                    val curIdx = options.indexOfFirst { it.first == cur }
                                    val nextOpt = options[(curIdx + 1).coerceAtLeast(0) % options.size]
                                    p.edit().putString("tile_source", nextOpt.first).apply()
                                    toast = "已切换为 ${nextOpt.second}（重进复盘页生效）"
                                }
                            )
                        }
                        Spacer(Modifier.height(12.dp))

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

                Spacer(Modifier.height(12.dp))

                // ========== 数据管理 ==========
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    CardTitle("数据管理")

                    Text(
                        text = "轨迹与日志仅存于本机，不会自动删除。\n你可随时手动清理。",
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (confirmDelete) {
                        Text(
                            text = "确认删除全部轨迹与日志？此操作不可恢复。",
                            fontSize = 12.5.sp,
                            color = Glass.Primary,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                GlassSecondaryButton(text = "取消", onClick = { confirmDelete = false })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                GlassPrimaryButton(
                                    text = "确认删除",
                                    onClick = {
                                        confirmDelete = false
                                        scope.launch {
                                            trackStore.deleteAll()
                                            logStore.deleteAll()
                                            entries = emptyList()
                                            trackPoints = emptyList()
                                            toast = "已删除全部数据"
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        GlassGhostButton(
                            text = "清除全部轨迹与日志",
                            onClick = { confirmDelete = true }
                        )
                    }

                    toast?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = Color(0xFF6DFFB0),
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ========== 返回 ==========
                GlassSecondaryButton(text = "返回结界", onClick = onBack)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 心率脉冲装饰（小红色心跳点，跟随真实 BPM 呼吸节奏）。
 */
@Composable
private fun HeartRatePulse(
    bpm: Int,
    modifier: Modifier = Modifier
) {
    // 根据 BPM 计算脉冲周期（毫秒），最低 500ms 防止过快
    val pulseMs = (60_000L / bpm.coerceAtLeast(30)).coerceAtLeast(500L)
    val transition = rememberInfiniteTransition(label = "hrPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (pulseMs * 0.35).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hrScale"
    )
    Canvas(
        modifier = modifier
            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
            .height(40.dp)
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = (size.minDimension / 4) * scale
        drawCircle(
            color = Glass.Primary.copy(alpha = 0.9f),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Glass.Primary.copy(alpha = 0.3f),
            radius = r * 1.6f,
            center = Offset(cx, cy)
        )
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
