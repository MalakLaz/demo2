package com.mallar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MallARColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealSurface,
    onPrimaryContainer = TealDark,
    secondary = TealLight,
    onSecondary = Color.White,
    background = Color(0xFFF5FAFA),
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = TealSurface,
    onSurfaceVariant = TextSecondary,
    error = Color(0xFFE53935),
    onError = Color.White
)

@Composable
fun MallARTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MallARColorScheme,
        typography = MallARTypography,
        content = content
    )
}
