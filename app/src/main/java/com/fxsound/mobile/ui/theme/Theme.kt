package com.fxsound.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = FxAccentCoral,
    secondary = FxCyan,
    background = FxBgDark,
    surface = FxCardSurface,
    surfaceVariant = FxCardElevated,
    onPrimary = FxTextPrimary,
    onSecondary = FxTextPrimary,
    onBackground = FxTextPrimary,
    onSurface = FxTextPrimary,
    outline = FxBorder
)

@Composable
fun FxMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
