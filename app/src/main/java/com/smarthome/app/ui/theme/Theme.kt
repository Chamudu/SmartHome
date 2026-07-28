package com.smarthome.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF334155),
    tertiary = Color(0xFF6366F1),
    tertiaryContainer = Color(0xFFEEF2FF),
    onTertiaryContainer = Color(0xFF3730A3),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF818CF8),
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
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
