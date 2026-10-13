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
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * 瓦片源定义。
 *
 * 国内可用源优先：高德中文（国内速度最快、中文标注），
 * 海外用户可切到 OSM 官方或 OpenTopoMap。
 */
private object ChinaTileSources {

    /**
     * 高德中文路网瓦片（国内最稳定、中文标注）。
     * 四个子域名轮询，支持并发下载。
     * 不需要 API Key，免费可用（个人使用）。
     */
    val AMAP_ZH: OnlineTileSourceBase = XYTileSource(
        "高德中文",
        3, 20, 256, ".png",
        arrayOf(
            "https://webrd01.is.autonavi.com/appmaptile",
            "https://webrd02.is.autonavi.com/appmaptile",
            "https://webrd03.is.autonavi.com/appmaptile",
            "https://webrd04.is.autonavi.com/appmaptile"
        )
    ).apply {
        // 高德瓦片的 URL 参数在 tile path 之外
        // osmdroid 默认拼接 {z}/{x}/{y}.png，但高德用 query string
        // 我们需要重写 getTileURLString
    }

    /**
     * OSM 官方瓦片（海外稳定，国内部分地区被 DNS 污染）。
     */
    val OSM_OFFICIAL = TileSourceFactory.MAPNIK

    /**
     * OpenTopoMap 地形图（全球可用，但国内较慢）。
     */
    val TOPO = TileSourceFactory.OpenTopo
}

/**
 * 高德中文瓦片源（重写 URL 拼接，使用 query string 格式）。
 *
 * osmdroid 的 OnlineTileSourceBase 构造参数顺序：
 * name, zoomMin, zoomMax, tileSize, fileExtension, baseUrls...
 */
private class AmapTileSource : OnlineTileSourceBase(
    "高德中文",
    3, 20, 256, ".png",
    arrayOf(
        "https://webrd01.is.autonavi.com/appmaptile",
        "https://webrd02.is.autonavi.com/appmaptile",
        "https://webrd03.is.autonavi.com/appmaptile",
        "https://webrd04.is.autonavi.com/appmaptile"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
        val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
        val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
        return "?lang=zh_cn&size=1&scale=1&style=8&x=$x&y=$y&z=$zoom"
    }
}

private val AMAP_SOURCE = AmapTileSource()

/**
 * 选择瓦片源。默认高德中文（国内最友好）。
 *
 * @param choice 存储键：amap（默认）/ osm / topo
 */
private fun resolveTileSource(context: android.content.Context): org.osmdroid.tileprovider.tilesource.ITileSource {
    val pref = context.getSharedPreferences("map_prefs", android.content.Context.MODE_PRIVATE)
    val choice = pref.getString("tile_source", "amap") ?: "amap"
    return when (choice) {
        "osm" -> TileSourceFactory.MAPNIK
        "topo" -> TileSourceFactory.OpenTopo
        else -> AMAP_SOURCE  // 默认高德中文
    }
}

/**
 * 瓦片源列表（供 UI 切换按钮使用）。
 * 返回 (存储键, 显示名, 版权说明)。
 */
fun tileSourceOptions(): List<Triple<String, String, String>> = listOf(
    Triple("amap", "高德中文（国内推荐）", "© 高德地图"),
    Triple("osm", "OpenStreetMap", "© OpenStreetMap contributors"),
    Triple("topo", "OpenTopoMap 地形", "© OpenTopoMap")
)

/**
 * 基于 OSM 的轨迹地图（可选底图）。
 *
 * 使用 osmdroid + 可切换瓦片源，开源、无需 API Key。
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
    modifier: Modifier = Modifier,
    copyrightOverride: String = ""
) {
    val context = LocalContext.current

    // ⚠️ 不用 return@Box 提前返回（同 TrackCanvas：Composable lambda 内
    // 提前返回会破坏 Composer 组栈，重组时组栈下溢闪退）。
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
        } else {
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
                        setTileSource(resolveTileSource(context))
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

            // 版权标注（法律要求）
            val copyright = copyrightOverride.ifBlank {
                val pref = context.getSharedPreferences("map_prefs", android.content.Context.MODE_PRIVATE)
                when (pref.getString("tile_source", "amap")) {
                    "osm" -> "© OpenStreetMap contributors"
                    "topo" -> "© OpenTopoMap"
                    else -> "© 高德地图"
                }
            }
            Text(
                text = copyright,
                fontSize = 9.sp,
                color = Color(0x99FFFFFF),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color(0x66000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
