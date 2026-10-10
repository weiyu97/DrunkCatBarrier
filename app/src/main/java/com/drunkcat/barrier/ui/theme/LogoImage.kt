package com.drunkcat.barrier.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drunkcat.barrier.R

/**
 * 品牌 Logo。
 *
 * 使用「Logo 主标 单色版裁切」（黑线白底），预先在 CPU 侧做像素反相，
 * 生成「深底亮线」的位图，再用 Image 显示。
 *
 * 之所以不用 BlendMode.Difference：该混合模式需要 Android 10 (API 29)，
 * 旧设备上会渲染成纯色块。位图反相在所有版本上表现一致。
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val context = LocalContext.current
    val inverted = remember(size) {
        val resId = R.drawable.logo_cat
        val src = BitmapFactory.decodeResource(context.resources, resId)
            ?: return@remember null
        invertBitmap(src).asImageBitmap()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A1560), Color(0xFF1A0830))
                )
            )
            .border(1.5.dp, Color(0x8CFF4678), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (inverted != null) {
            Image(
                bitmap = inverted,
                contentDescription = "醉猫结界",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo_cat),
                contentDescription = "醉猫结界",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * 逐像素反相：白底 -> 黑（融入深紫），黑线 -> 白（浮现）。
 */
private fun invertBitmap(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    for (i in px.indices) {
        val c = px[i]
        val a = (c ushr 24) and 0xFF
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        // 反相 RGB，保留 alpha
        val nr = 255 - r
        val ng = 255 - g
        val nb = 255 - b
        px[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
    }
    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
}
