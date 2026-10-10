package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.drunkcat.barrier.track.TrackPoint
import com.drunkcat.barrier.track.TrackProjector

/**
 * 轨迹可视化（纯 Canvas，不依赖地图 SDK）。
 *
 * 使用 TrackProjector 将经纬度归一化到画布坐标，
 * 绘制连线与起点/终点/中间节点。
 */
@Composable
fun TrackCanvas(
    points: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33000000))
    ) {
        if (points.size < 2) {
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pad = 24f

            val normalized = TrackProjector.normalize(points)

            fun project(idx: Int): Offset {
                val n = normalized[idx]
                return Offset(
                    x = pad + n.x * (w - pad * 2),
                    y = pad + n.y * (h - pad * 2)
                )
            }

            // 连线
            val path = Path().apply {
                val first = project(0)
                moveTo(first.x, first.y)
                for (i in 1 until points.size) {
                    val o = project(i)
                    lineTo(o.x, o.y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFFFF3D6E),
                style = Stroke(width = 3f)
            )

            // 节点：起点绿、终点红、中间紫
            points.indices.forEach { idx ->
                val o = project(idx)
                val color = when (idx) {
                    0 -> Color(0xFF6DFFB0)
                    points.lastIndex -> Color(0xFFFF3D6E)
                    else -> Color(0xFFB08CFF)
                }
                val radius = if (idx == 0 || idx == points.lastIndex) 8f else 5f
                drawCircle(color = color, radius = radius, center = o)
            }
        }
    }
}
