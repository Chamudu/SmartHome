package com.smarthome.app.domain.validation

import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.LayoutViolation
import com.smarthome.app.domain.model.RoomLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.smarthome.app.domain.model.defaultFloorName

class FloorLayoutValidatorTest {
    @Test
    fun `floor names are derived from logical levels`() {
        assertEquals("Basement", defaultFloorName(-1))
        assertEquals("Ground floor", defaultFloorName(0))
        assertEquals("First floor", defaultFloorName(1))
        assertEquals("Second floor", defaultFloorName(2))
        assertEquals("4th floor", defaultFloorName(4))
    }

    @Test
    fun `valid floor dimensions produce no violations`() {
        val violations = FloorLayoutValidator.validateFloor(
            name = "Ground floor",
            level = 0,
            gridColumns = 12,
            gridRows = 16,
        )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `floor rejects blank name and grid dimensions outside limits`() {
        val violations = FloorLayoutValidator.validateFloor(
            name = " ",
            level = 0,
            gridColumns = 3,
            gridRows = 41,
        )

        assertEquals(
            setOf(
                LayoutViolation.FLOOR_NAME_BLANK,
                LayoutViolation.GRID_COLUMNS_OUT_OF_RANGE,
                LayoutViolation.GRID_ROWS_OUT_OF_RANGE,
            ),
            violations,
        )
    }

    @Test
    fun `floor rejects a level already used in the home`() {
        val violations = FloorLayoutValidator.validateFloor(
            name = "Other ground floor",
            level = 0,
            gridColumns = 12,
            gridRows = 16,
            existingFloors = listOf(floor()),
        )

        assertEquals(
            setOf(LayoutViolation.FLOOR_LEVEL_DUPLICATE),
            violations,
        )
    }

    @Test
    fun `room contained by floor produces no violations`() {
        val violations = FloorLayoutValidator.validateRoom(
            floor = floor(),
            candidate = room(
                id = "kitchen",
                column = 0,
                row = 0,
                width = 5,
                height = 6,
            ),
        )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `room cannot extend beyond floor boundary`() {
        val violations = FloorLayoutValidator.validateRoom(
            floor = floor(),
            candidate = room(
                id = "kitchen",
                column = 8,
                row = 12,
                width = 5,
                height = 5,
            ),
        )

        assertEquals(
            setOf(LayoutViolation.ROOM_OUTSIDE_FLOOR),
            violations,
        )
    }

    @Test
    fun `room cannot overlap an existing room`() {
        val existingRoom = room(
            id = "kitchen",
            column = 0,
            row = 0,
            width = 5,
            height = 6,
        )
        val floor = floor(rooms = listOf(existingRoom))

        val violations = FloorLayoutValidator.validateRoom(
            floor = floor,
            candidate = room(
                id = "living-room",
                column = 4,
                row = 2,
                width = 5,
                height = 5,
            ),
        )

        assertEquals(
            setOf(LayoutViolation.ROOM_OVERLAPS_EXISTING),
            violations,
        )
    }

    @Test
    fun `rooms may share an edge without overlapping`() {
        val existingRoom = room(
            id = "kitchen",
            column = 0,
            row = 0,
            width = 5,
            height = 6,
        )
        val floor = floor(rooms = listOf(existingRoom))

        val violations = FloorLayoutValidator.validateRoom(
            floor = floor,
            candidate = room(
                id = "living-room",
                column = 5,
                row = 0,
                width = 7,
                height = 6,
            ),
        )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `editing a room excludes itself from overlap detection`() {
        val existingRoom = room(
            id = "kitchen",
            column = 0,
            row = 0,
            width = 5,
            height = 6,
        )
        val floor = floor(rooms = listOf(existingRoom))

        val violations = FloorLayoutValidator.validateRoom(
            floor = floor,
            candidate = existingRoom.copy(name = "Main kitchen"),
        )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `device position must remain inside floor grid`() {
        assertTrue(
            FloorLayoutValidator.validateDevicePosition(
                floor(),
                column = 11,
                row = 15,
            ).isEmpty(),
        )

        assertEquals(
            setOf(LayoutViolation.DEVICE_POSITION_OUTSIDE_FLOOR),
            FloorLayoutValidator.validateDevicePosition(
                floor(),
                column = 12,
                row = 16,
            ),
        )
    }

    private fun floor(
        rooms: List<RoomLayout> = emptyList(),
    ) = FloorPlan(
        id = "ground-floor",
        name = "Ground floor",
        level = 0,
        gridColumns = 12,
        gridRows = 16,
        rooms = rooms,
    )

    private fun room(
        id: String,
        column: Int,
        row: Int,
        width: Int,
        height: Int,
    ) = RoomLayout(
        id = id,
        name = "Room",
        column = column,
        row = row,
        width = width,
        height = height,
    )
}
