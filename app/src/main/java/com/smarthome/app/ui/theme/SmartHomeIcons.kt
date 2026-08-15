package com.smarthome.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object SmartHomeIcons {
    val Home = icon("M10,20 L10,14 L14,14 L14,20 L19,20 L19,12 L22,12 L12,3 L2,12 L5,12 L5,20 Z")
    val Devices = icon("M4,6 L20,6 L20,17 L4,17 Z M8,19 L16,19 L16,21 L8,21 Z M6,8 L6,15 L18,15 L18,8 Z")
    val Layout = icon("M3,3 L11,3 L11,11 L3,11 Z M13,3 L21,3 L21,11 L13,11 Z M3,13 L11,13 L11,21 L3,21 Z M13,13 L21,13 L21,21 L13,21 Z")
    val Light = icon("M9,21 L15,21 L15,19 L9,19 Z M12,2 C8.7,2 6,4.7 6,8 C6,10.2 7.2,12.1 9,13.2 L9,17 L15,17 L15,13.2 C16.8,12.1 18,10.2 18,8 C18,4.7 15.3,2 12,2 Z")
    val Power = icon("M11,2 L13,2 L13,12 L11,12 Z M7.1,4.4 C3.9,6.2 2,9.7 2.8,13.3 C3.7,17.5 7.4,20.5 11.7,20.7 C16.7,20.9 20.9,16.9 21,12 C21,8.8 19.3,5.9 16.9,4.4 L15.5,6 C17.6,7.2 19,9.4 19,12 C19,15.9 15.9,19 12,19 C8.1,19 5,15.9 5,12 C5,9.4 6.4,7.2 8.5,6 Z")
    val Safety = icon("M12,2 L20,5 L20,11 C20,16.1 16.6,20.7 12,22 C7.4,20.7 4,16.1 4,11 L4,5 Z M11,7 L11,13 L13,13 L13,7 Z M11,15 L11,17 L13,17 L13,15 Z")
    val Camera = icon("M3,6 L17,6 L17,9 L21,7 L21,17 L17,15 L17,18 L3,18 Z M5,8 L5,16 L15,16 L15,8 Z")
    val Warning = icon("M1,21 L12,2 L23,21 Z M11,8 L11,14 L13,14 L13,8 Z M11,16 L11,18 L13,18 L13,16 Z")
    val Schedule = icon("M7,2 L9,2 L9,4 L15,4 L15,2 L17,2 L17,4 L20,4 L20,21 L4,21 L4,4 L7,4 Z M6,9 L18,9 L18,6 L6,6 Z M11,11 L13,11 L13,15 L16,15 L16,17 L11,17 Z")
    val Profile = icon("M12,2 C9.2,2 7,4.2 7,7 C7,9.8 9.2,12 12,12 C14.8,12 17,9.8 17,7 C17,4.2 14.8,2 12,2 Z M12,14 C7.6,14 4,16.2 4,19 L4,22 L20,22 L20,19 C20,16.2 16.4,14 12,14 Z")
    val Activity = icon("M3,4 L5,4 L5,20 L3,20 Z M7,9 L9,9 L9,20 L7,20 Z M11,6 L13,6 L13,20 L11,20 Z M15,12 L17,12 L17,20 L15,20 Z M19,8 L21,8 L21,20 L19,20 Z")

    private fun icon(path: String): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}
