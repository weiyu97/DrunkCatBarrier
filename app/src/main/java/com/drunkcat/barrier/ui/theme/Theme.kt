package com.drunkcat.barrier.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ThemeMode { GLASS, MINIMAL }

private val GlassColorScheme = darkColorScheme(
    primary = Glass.Primary,
    onPrimary = Glass.OnBg,
    secondary = Glass.Secondary,
    onSecondary = Glass.OnBg,
    tertiary = Glass.Tertiary,
    background = Glass.BgDeep,
    onBackground = Glass.OnBg,
    surface = Glass.Surface,
    onSurface = Glass.OnSurface,
    surfaceVariant = Glass.SurfaceVariant,
    onSurfaceVariant = Glass.OnSurfaceVariant,
    outline = Glass.Outline,
    error = Glass.Error
)

private val MinimalColorScheme = lightColorScheme(
    primary = Minimal.Primary,
    onPrimary = Minimal.Surface,
    secondary = Minimal.Secondary,
    tertiary = Minimal.Tertiary,
    background = Minimal.Bg,
    onBackground = Minimal.OnBg,
    surface = Minimal.Surface,
    onSurface = Minimal.OnSurface,
    surfaceVariant = Minimal.SurfaceVariant,
    onSurfaceVariant = Minimal.OnSurfaceVariant,
    outline = Minimal.Outline,
    error = Minimal.Error
)

@Composable
fun DrunkCatTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit
) {
    val colorScheme = when (mode) {
        ThemeMode.GLASS -> GlassColorScheme
        ThemeMode.MINIMAL -> MinimalColorScheme
    }
    val typography = when (mode) {
        ThemeMode.GLASS -> GlassTypography
        ThemeMode.MINIMAL -> MinimalTypography
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

/** 辅助：取得当前主题的代码块背景色 */
object ThemeExtras {
    fun codeBg(mode: ThemeMode) = when (mode) {
        ThemeMode.GLASS -> Glass.CodeBg
        ThemeMode.MINIMAL -> Minimal.CodeBg
    }
    fun success(mode: ThemeMode) = when (mode) {
        ThemeMode.GLASS -> Glass.Success
        ThemeMode.MINIMAL -> Minimal.Success
    }
}
