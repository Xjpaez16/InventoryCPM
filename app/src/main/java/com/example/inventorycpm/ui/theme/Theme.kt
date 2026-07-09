package com.example.inventorycpm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = AccentTeal,
    onSecondary = OnPrimary,
    secondaryContainer = Color(0xFF064E4C),
    onSecondaryContainer = Color(0xFFB2DFDD),
    tertiary = AccentAmber,
    onTertiary = Color(0xFF1C1400),
    tertiaryContainer = Color(0xFF4A3800),
    onTertiaryContainer = Color(0xFFFFDEA0),
    error = ErrorRed,
    onError = OnPrimary,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = BackgroundDark,
    onBackground = OnBackground,
    surface = SurfaceDark,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariant,
    outline = CardBorder,
    outlineVariant = Color(0xFF1E293B),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = PrimaryBlueDark,
    surfaceTint = PrimaryBlue,
    scrim = Color(0x80000000)
)

@Composable
fun InventoryCPMTheme(
    content: @Composable () -> Unit
) {
    // La app usa siempre el tema oscuro (mejor visibilidad en campo/exterior)
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
