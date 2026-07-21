package com.lifeos.personal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeOsColors = darkColorScheme(
    primary = Color(0xFF70D6A3),
    onPrimary = Color(0xFF082016),
    secondary = Color(0xFFF6C85F),
    error = Color(0xFFFF7B7B),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF202833),
    outline = Color(0xFF2D3744),
    onBackground = Color(0xFFEEF2F7),
    onSurface = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF9AA7B6),
)

@Composable
fun LifeOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LifeOsColors, content = content)
}
