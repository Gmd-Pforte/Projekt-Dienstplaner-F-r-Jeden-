package de.psaimusic.dienstplaner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C6E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEDE5),
    onPrimaryContainer = Color(0xFF163D36),
    secondary = Color(0xFF5A6B8C),
    secondaryContainer = Color(0xFFDCE4F6),
    tertiary = Color(0xFF8B5E3C),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEFF3),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ED8CB),
    onPrimary = Color(0xFF05382F),
    primaryContainer = Color(0xFF1E5148),
    secondary = Color(0xFFBCC7E0),
    tertiary = Color(0xFFE7B891),
    background = Color(0xFF111315),
    surface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFF25292D)
)

@Composable
fun DienstplanerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
