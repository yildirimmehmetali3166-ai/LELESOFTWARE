package com.lelesoftware.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF050914),
    primaryContainer = Color(0xFF2B1F0A),
    onPrimaryContainer = PlatinumRim,
    secondary = CyanAccent,
    onSecondary = Color(0xFF050914),
    secondaryContainer = Color(0xFF0B2536),
    onSecondaryContainer = SkyTelemetry,
    tertiary = CrimsonBrand,
    onTertiary = Color.White,
    background = SlateDarkBackground,
    onBackground = TextLight,
    surface = SlateCardSurface,
    onSurface = TextLight,
    surfaceVariant = SlateElevatedSurface,
    onSurfaceVariant = TextMuted,
    outline = SlateCardBorder,
    error = RoseDanger,
    onError = Color.White
)

private val CorporateShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LeleSoftwareTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = CorporateShapes,
        content = content
    )
}
