package com.app.oshoaudio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Orange = Color(0xFFEA580C)
private val LightOrange = Color(0xFFFB923C)

private val LightScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Color(0xFF7C2D12),
    background = Color(0xFFFFF7ED),
    surface = Color(0xFFFFFFFF)
)

private val DarkScheme = darkColorScheme(
    primary = LightOrange,
    onPrimary = Color.Black,
    secondary = Color(0xFFFDBA74),
    background = Color(0xFF0C0A09),
    surface = Color(0xFF1C1917)
)

@Composable
fun OshoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content
    )
}
