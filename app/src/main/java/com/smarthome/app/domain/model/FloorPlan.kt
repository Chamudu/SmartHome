package com.smarthome.app.domain.model

data class FloorPlan(
    val id: String,
    val name: String,
    val level: Int,
    val gridColumns: Int,
    val gridRows: Int,
    val rooms: List<RoomLayout> = emptyList(),
)

data class RoomLayout(
    val id: String,
    val name: String,
    val column: Int,
    val row: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int
        get() = column + width

    val bottom: Int
        get() = row + height
}

enum class LayoutViolation {
    FLOOR_NAME_BLANK,
    FLOOR_LEVEL_DUPLICATE,
    GRID_COLUMNS_OUT_OF_RANGE,
    GRID_ROWS_OUT_OF_RANGE,
    ROOM_NAME_BLANK,
    ROOM_ORIGIN_NEGATIVE,
    ROOM_SIZE_NOT_POSITIVE,
    ROOM_OUTSIDE_FLOOR,
    ROOM_OVERLAPS_EXISTING,
    DEVICE_POSITION_OUTSIDE_FLOOR,
    DEVICE_POSITION_OUTSIDE_ROOM,
}

fun defaultFloorName(level: Int): String = when (level) {
    in Int.MIN_VALUE..-2 -> "Basement ${-level.toLong()}"
    -1 -> "Basement"
    0 -> "Ground floor"
    1 -> "First floor"
    2 -> "Second floor"
    3 -> "Third floor"
    else -> "${level}th floor"
}
