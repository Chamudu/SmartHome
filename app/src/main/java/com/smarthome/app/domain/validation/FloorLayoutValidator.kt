package com.smarthome.app.domain.validation

import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.LayoutViolation
import com.smarthome.app.domain.model.RoomLayout

object FloorLayoutValidator {
    const val MIN_GRID_SIZE = 4
    const val MAX_GRID_SIZE = 40

    fun validateFloor(
        name: String,
        level: Int,
        gridColumns: Int,
        gridRows: Int,
        existingFloors: List<FloorPlan> = emptyList(),
    ): Set<LayoutViolation> = buildSet {
        if (name.isBlank()) {
            add(LayoutViolation.FLOOR_NAME_BLANK)
        }

        if (existingFloors.any { floor -> floor.level == level }) {
            add(LayoutViolation.FLOOR_LEVEL_DUPLICATE)
        }

        if (gridColumns !in MIN_GRID_SIZE..MAX_GRID_SIZE) {
            add(LayoutViolation.GRID_COLUMNS_OUT_OF_RANGE)
        }

        if (gridRows !in MIN_GRID_SIZE..MAX_GRID_SIZE) {
            add(LayoutViolation.GRID_ROWS_OUT_OF_RANGE)
        }
    }

    fun validateRoom(
        floor: FloorPlan,
        candidate: RoomLayout,
    ): Set<LayoutViolation> = buildSet {
        if (candidate.name.isBlank()) {
            add(LayoutViolation.ROOM_NAME_BLANK)
        }

        if (candidate.column < 0 || candidate.row < 0) {
            add(LayoutViolation.ROOM_ORIGIN_NEGATIVE)
        }

        if (candidate.width <= 0 || candidate.height <= 0) {
            add(LayoutViolation.ROOM_SIZE_NOT_POSITIVE)
        }

        val hasUsableGeometry =
            candidate.column >= 0 &&
                candidate.row >= 0 &&
                candidate.width > 0 &&
                candidate.height > 0

        if (hasUsableGeometry &&
            (candidate.right > floor.gridColumns ||
                candidate.bottom > floor.gridRows)
        ) {
            add(LayoutViolation.ROOM_OUTSIDE_FLOOR)
        }

        if (hasUsableGeometry &&
            floor.rooms
                .asSequence()
                .filterNot { room -> room.id == candidate.id }
                .any { room -> room.overlaps(candidate) }
        ) {
            add(LayoutViolation.ROOM_OVERLAPS_EXISTING)
        }
    }
}

private fun RoomLayout.overlaps(other: RoomLayout): Boolean {
    return column < other.right &&
        right > other.column &&
        row < other.bottom &&
        bottom > other.row
}
