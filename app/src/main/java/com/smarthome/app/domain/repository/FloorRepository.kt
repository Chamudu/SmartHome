package com.smarthome.app.domain.repository

import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.RoomLayout
import kotlinx.coroutines.flow.Flow

interface FloorRepository {
    fun observeFloors(homeId: String): Flow<List<FloorPlan>>

    fun observeRooms(
        homeId: String,
        floorId: String,
    ): Flow<List<RoomLayout>>

    suspend fun createFloor(
        homeId: String,
        name: String,
        level: Int,
        gridColumns: Int,
        gridRows: Int,
    ): String

    suspend fun createRoom(
        homeId: String,
        floorId: String,
        room: RoomLayout,
    ): String

    suspend fun deleteFloor(
        homeId: String,
        floorId: String,
    )

    suspend fun deleteRoom(
        homeId: String,
        floorId: String,
        roomId: String,
    )
}

class FloorContainsDevicesException : IllegalStateException(
    "Move or remove devices before deleting this floor.",
)
