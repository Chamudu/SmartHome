package com.smarthome.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF087DB7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8EDFB),
    onPrimaryContainer = Color(0xFF002F43),
    secondary = Color(0xFF4A626D),
    secondaryContainer = Color(0xFFD6E7ED),
    tertiary = Color(0xFF67587A),
    tertiaryContainer = Color(0xFFEDDCFF),
    background = Color(0xFFF8F6F0),
    surface = Color(0xFFF8F6F0),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5D1),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF00504D),
    onPrimaryContainer = Color(0xFF9CF1ED),
    secondary = Color(0xFFB1CCC9),
    secondaryContainer = Color(0xFF334B49),
    tertiary = Color(0xFFACCBE5),
    tertiaryContainer = Color(0xFF2D4960),
    background = Color(0xFF0E1514),
    surface = Color(0xFF0E1514),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
)

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
