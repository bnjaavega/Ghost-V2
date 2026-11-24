package com.ghost.editor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta de Ghost: únicamente modo oscuro. */
object GhostColors {
    val Bg = Color(0xFF000000)
    val Card1 = Color(0xFF1C1C1E)
    val Card2 = Color(0xFF2C2C2E)
    val Card3 = Color(0xFF3A3A3C)
    val Text = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val Danger = Color(0xFFFF453A)
    val Success = Color(0xFF32D74B)
    val Warning = Color(0xFFFFD60A)
    val CodeBg = Color(0xFF0E0E10)
}

private val GhostColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = GhostColors.Card3,
    onPrimaryContainer = Color.White,
    secondary = GhostColors.TextSecondary,
    onSecondary = Color.Black,
    secondaryContainer = GhostColors.Card3,
    onSecondaryContainer = Color.White,
    background = GhostColors.Bg,
    onBackground = GhostColors.Text,
    surface = GhostColors.Card1,
    onSurface = GhostColors.Text,
    surfaceVariant = GhostColors.Card2,
    onSurfaceVariant = GhostColors.TextSecondary,
    surfaceContainer = GhostColors.Card1,
    surfaceContainerHigh = GhostColors.Card2,
    surfaceContainerHighest = GhostColors.Card3,
    outline = GhostColors.Card3,
    outlineVariant = GhostColors.Card2,
    error = GhostColors.Danger,
    onError = Color.White
)

// Tipografía del sistema para la interfaz, tamaños contenidos.
private val GhostTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

/** Fuente monoespaciada para código y consola. */
val CodeFont: FontFamily = FontFamily.Monospace

@Composable
fun GhostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GhostColorScheme,
        typography = GhostTypography,
        content = content
    )
}
