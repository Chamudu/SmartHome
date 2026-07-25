package com.smarthome.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A67),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1ED),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Color(0xFF4A6361),
    secondaryContainer = Color(0xFFCDE8E5),
    tertiary = Color(0xFF456179),
    tertiaryContainer = Color(0xFFCCE5FF),
    background = Color(0xFFF4FBF9),
    surface = Color(0xFFF4FBF9),
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
