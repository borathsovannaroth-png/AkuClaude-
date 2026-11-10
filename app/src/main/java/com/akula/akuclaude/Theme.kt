package com.akula.akuclaude

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NightBlue = Color(0xFF0A1B33)
val NightBlueDeep = Color(0xFF060F22)
val AccentBlue = Color(0xFF2F6FED)
val AccentBlueSoft = Color(0xFF6FA8FF)
val GlassWhite = Color(0x1AFFFFFF)
val GlassBorder = Color(0x33A9C7FF)
val RunGreen = Color(0xFF34D399)
val TextPrimary = Color(0xFFE7EEFC)
val TextSecondary = Color(0xFF8FA3C7)

private val scheme = darkColorScheme(
    background = NightBlueDeep,
    surface = NightBlue,
    primary = AccentBlue,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AkuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
