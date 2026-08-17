package com.smarthome.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0), // Deep Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF), // Light blue background
    onPrimaryContainer = Color(0xFF00256E),
    secondary = Color(0xFF0277BD), // Darker blue
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE5FF),
    onSecondaryContainer = Color(0xFF001D34),
    tertiary = Color(0xFF006A60), // Teal accent
    tertiaryContainer = Color(0xFFCBF5ED),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF5F8FF), // Slight blue-tinted white
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E7F0), // Blue-grey cards
    onSurfaceVariant = Color(0xFF3A4459),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9), // Light blue for dark theme
    onPrimary = Color(0xFF003064),
    primaryContainer = Color(0xFF00468A),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF004D67),
    onSecondaryContainer = Color(0xFFCDE5FF),
    tertiary = Color(0xFF4DCEBF),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFCBF5ED),
    background = Color(0xFF0D1B2A), // Deep dark navy
    surface = Color(0xFF132337),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E3248),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

object SmartHomeThemeColors {
    private val isDark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val statusOn: Color
        @Composable get() = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)

    val statusOnContainer: Color
        @Composable get() = if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)

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
