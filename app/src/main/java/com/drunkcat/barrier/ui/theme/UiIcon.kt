package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drunkcat.barrier.R

/**
 * 琉璃风统一图标集。
 * 全部为矢量 drawable，支持 tint 与自定义尺寸。
 */
enum class UiIconType(val res: Int) {
    SHIELD(R.drawable.ic_shield),
    KEY(R.drawable.ic_key),
    PACKAGE(R.drawable.ic_package),
    BELL(R.drawable.ic_bell),
    WARNING(R.drawable.ic_warning),
    CHECK(R.drawable.ic_check),
    CROSS(R.drawable.ic_cross),
    CANDLE(R.drawable.ic_candle),
    SCROLL(R.drawable.ic_scroll),
    CRYSTAL(R.drawable.ic_crystal),
    DAGGER(R.drawable.ic_dagger)
}

@Composable
fun UiIcon(
    type: UiIconType,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = LocalContentColor.current
) {
    Image(
        painter = painterResource(id = type.res),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}
