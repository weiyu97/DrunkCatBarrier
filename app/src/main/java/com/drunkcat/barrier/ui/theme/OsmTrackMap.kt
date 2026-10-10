package com.drunkcat.barrier.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.drunkcat.barrier.track.TrackPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * 基于 OSM 的轨迹地图（可选底图）。
 *
 * 使用 osmdroid + OpenStreetMap 瓦片，开源、无需 API Key。
 *
 * **隐私说明**：
 * - 仅在用户主动开启后使用
 * - 联网只为下载地图瓦片，不上传任何用户数据
 * - 瓦片缓存于本地，再次查看同一区域无需重新下载
 * - 默认关闭，保持纯本地模式
 */
@Composable
fun OsmTrackMap(
    points: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22000000))
    ) {
        if (points.size < 2) {
            Text(
                text = "轨迹点不足，无法绘制",
                fontSize = 12.sp,
                color = Glass.OnSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        // 一次性初始化 osmdroid 配置
        DisposableEffect(Unit) {
            Configuration.getInstance().apply {
                userAgentValue = context.packageName
                // 瓦片缓存目录（默认已是应用私有目录）
            }
            onDispose { }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    overlays.add(CopyrightOverlay(ctx))

                    val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

                    // 轨迹线
                    val line = Polyline().apply {
                        setPoints(geoPoints)
                        outlinePaint.color = AndroidColor.parseColor("#FF3D6E")
                        outlinePaint.strokeWidth = 8f
                    }
                    overlays.add(line)

                    // 起点标记
                    geoPoints.firstOrNull()?.let { start ->
                        overlays.add(Marker(this).apply {
                            position = start
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "起点"
                        })
                    }
                    // 终点标记
                    geoPoints.lastOrNull()?.let { end ->
                        overlays.add(Marker(this).apply {
                            position = end
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "终点"
                        })
                    }

                    // 视野自动缩放到轨迹范围
                    controller.setZoom(15.0)
                    val mid = GeoPoint(
                        geoPoints.map { it.latitude }.average(),
                        geoPoints.map { it.longitude }.average()
                    )
                    controller.setCenter(mid)

                    // 让轨迹完整可见
                    post {
                        try {
                            zoomToBoundingBox(
                                BoundingBox.fromGeoPoints(geoPoints),
                                true,
                                80
                            )
                        } catch (e: Exception) { }
                    }
                }
            },
            update = { mapView ->
                mapView.invalidate()
            }
        )

        // 版权标注（OSM 要求）
        Text(
            text = "© OpenStreetMap contributors",
            fontSize = 9.sp,
            color = Color(0x99FFFFFF),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color(0x66000000))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
