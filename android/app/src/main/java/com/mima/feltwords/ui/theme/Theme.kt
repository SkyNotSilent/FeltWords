package com.mima.feltwords.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 毛毡扩展色板：Material 的 ColorScheme 之外，App 还要用到 cream/mint/sky/pink 等语义色，
 * 通过 CompositionLocal 暴露，等价 iOS 的 FeltTheme 枚举。
 */
data class FeltColors(
    val yellow: Color,
    val orange: Color,
    val cream: Color,
    val mint: Color,
    val sky: Color,
    val pink: Color,
    val ink: Color,
    val secondary: Color,
    val surface: Color,
    val isDark: Boolean,
)

private val LightFelt = FeltColors(
    yellow = FeltYellowLight, orange = FeltOrangeLight, cream = FeltCreamLight,
    mint = FeltMintLight, sky = FeltSkyLight, pink = FeltPinkLight,
    ink = FeltInkLight, secondary = FeltSecondaryLight, surface = FeltSurfaceLight,
    isDark = false,
)

private val DarkFelt = FeltColors(
    yellow = FeltYellowDark, orange = FeltOrangeDark, cream = FeltCreamDark,
    mint = FeltMintDark, sky = FeltSkyDark, pink = FeltPinkDark,
    ink = FeltInkDark, secondary = FeltSecondaryDark, surface = FeltSurfaceDark,
    isDark = true,
)

val LocalFeltColors = staticCompositionLocalOf { LightFelt }

/** 便捷访问：FeltTheme.colors.orange 等。 */
object FeltTheme {
    val colors: FeltColors
        @Composable get() = LocalFeltColors.current
}

private fun materialLight(felt: FeltColors) = lightColorScheme(
    primary = felt.orange,
    onPrimary = felt.ink,
    background = felt.yellow,
    onBackground = felt.ink,
    surface = felt.surface,
    onSurface = felt.ink,
    secondary = felt.mint,
    tertiary = felt.pink,
)

private fun materialDark(felt: FeltColors) = darkColorScheme(
    primary = felt.orange,
    onPrimary = felt.ink,
    background = felt.yellow,
    onBackground = felt.ink,
    surface = felt.surface,
    onSurface = felt.ink,
    secondary = felt.mint,
    tertiary = felt.pink,
)

/**
 * @param darkOverride 由天气服务的昼夜/手动模式驱动；null 时跟随系统。
 */
@Composable
fun FeltWordsTheme(
    darkOverride: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = darkOverride ?: isSystemInDarkTheme()
    val felt = if (dark) DarkFelt else LightFelt
    CompositionLocalProvider(LocalFeltColors provides felt) {
        MaterialTheme(
            colorScheme = if (dark) materialDark(felt) else materialLight(felt),
            typography = FeltTypography,
            content = content,
        )
    }
}
