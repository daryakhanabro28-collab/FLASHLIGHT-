package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = Color(0xFF70F5FF),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF002266),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = DeepViolet,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = DarkCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun UVPrivacyTheme(
    themeStyle: String = "AMOLED",
    content: @Composable () -> Unit
) {
    val (bg, surface, card) = when (themeStyle) {
        "MIDNIGHT" -> Triple(MidnightNavy, MidnightNavyCard, Color(0xFF16223C))
        "GRAPHITE" -> Triple(GraphiteBg, GraphiteCard, Color(0xFF2C2C32))
        else -> Triple(AmoledBlack, DarkCharcoal, SurfaceElevated)
    }

    val scheme = DarkColorScheme.copy(
        background = bg,
        surface = surface,
        surfaceVariant = card
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
