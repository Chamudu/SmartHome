package com.smarthome.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val LightColors = lightColorScheme(
    primary = Color(0xFF8B5CF6), // Vibrant Violet
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE), // Soft purple background
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFFF43F5E), // Vibrant Rose
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6), // Soft rose background
    onSecondaryContainer = Color(0xFF881337),
    tertiary = Color(0xFF10B981), // Emerald green
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = Color(0xFFFAFAFA), // Very clean white-grey
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF4F4F5), // Smooth light grey for cards
    onSurfaceVariant = Color(0xFF3F3F46),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFFB7185),
    onSecondary = Color(0xFF4C0519),
    secondaryContainer = Color(0xFF9F1239),
    onSecondaryContainer = Color(0xFFFFE4E6),
    tertiary = Color(0xFF34D399),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF09090B),
    surface = Color(0xFF18181B),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF27272A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
)

object SmartHomeThemeColors {
    private val isDark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val statusOn: Color
        @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF10B981)

    val statusOnContainer: Color
        @Composable get() = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)

    val statusDisconnected: Color
        @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val statusDisconnectedContainer: Color
        @Composable get() = if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
}

@Composable
fun SmartHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
