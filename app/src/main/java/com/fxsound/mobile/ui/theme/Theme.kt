package com.fxsound.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = FxAccent,
    secondary = FxTextSecondary,
    background = FxBgDark,
    surface = FxCardSurface,
    surfaceVariant = FxCardElevated,
    onPrimary = FxOnAccent,
    onSecondary = FxOnAccent,
    onBackground = FxTextPrimary,
    onSurface = FxTextPrimary,
    outline = FxBorder
)

private val LightColorScheme = lightColorScheme(
    primary = FxAccentLight,
    secondary = FxLightTextSecondary,
    background = FxLightBg,
    surface = FxLightCardSurface,
    surfaceVariant = FxLightCardElevated,
    onPrimary = FxOnAccentLight,
    onSecondary = FxOnAccentLight,
    onBackground = FxLightTextPrimary,
    onSurface = FxLightTextPrimary,
    outline = FxLightBorder
)

private val FxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FxMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = FxShapes,
        content = content
    )
}
