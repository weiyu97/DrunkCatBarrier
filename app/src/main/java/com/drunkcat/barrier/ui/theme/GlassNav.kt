package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 底部导航项。
 */
data class NavItem(
    val id: String,
    val icon: UiIconType,
    val label: String
)

val DefaultNavItems = listOf(
    NavItem("altar", UiIconType.CANDLE, "祭坛"),
    NavItem("contract", UiIconType.SCROLL, "契约"),
    NavItem("trial", UiIconType.CRYSTAL, "试炼"),
    NavItem("review", UiIconType.DAGGER, "复盘")
)

@Composable
fun GlassNavBar(
    items: List<NavItem>,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFA06030E))
                )
            )
            .padding(top = 11.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .clickable { onSelect(item.id) }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UiIcon(
                        type = item.icon,
                        size = 20.dp,
                        tint = if (item.id == current) Glass.Primary
                        else Glass.OnSurfaceVariant
                    )
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = if (item.id == current) FontWeight.SemiBold
                        else FontWeight.Normal,
                        color = if (item.id == current) Glass.Primary
                        else Glass.OnSurfaceVariant,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
