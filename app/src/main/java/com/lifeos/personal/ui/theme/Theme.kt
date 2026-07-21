package com.lifeos.personal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeOsColors = lightColorScheme(
    primary = Color(0xFF3659D9),
    secondary = Color(0xFF52699A),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFF7F8FC),
)

@Composable
fun LifeOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LifeOsColors, content = content)
}
