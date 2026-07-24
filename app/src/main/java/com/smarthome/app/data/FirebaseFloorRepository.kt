package com.smarthome.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.RoomLayout
import com.smarthome.app.domain.repository.FloorRepository
import com.smarthome.app.domain.repository.FloorContainsDevicesException
import com.smarthome.app.domain.repository.RoomContainsDevicesException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseFloorRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : FloorRepository {

    override fun observeFloors(
        homeId: String,
    ): Flow<List<FloorPlan>> = callbackFlow {
        val registration = floorsCollection(homeId)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> close(exception)
                    snapshot == null -> close(
                        IllegalStateException("Floor snapshot is missing."),
                    )

                    else -> runCatching {
                        snapshot.documents
                            .map(DocumentSnapshot::toFloorPlan)
                            .sortedWith(
                                compareBy<FloorPlan> { floor -> floor.level }
                                    .thenBy { floor -> floor.name },
                            )
                    }.onSuccess { floors ->
                        trySend(floors)
                    }.onFailure { mappingError ->
                        close(mappingError)
                    }
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    override fun observeRooms(
        homeId: String,
        floorId: String,
    ): Flow<List<RoomLayout>> = callbackFlow {
        val registration = roomsCollection(homeId, floorId)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> close(exception)
                    snapshot == null -> close(
                        IllegalStateException("Room snapshot is missing."),
                    )

                    else -> runCatching {
                        snapshot.documents
                            .map(DocumentSnapshot::toRoomLayout)
                            .sortedWith(
                                compareBy<RoomLayout> { room -> room.row }
                                    .thenBy { room -> room.column },
                            )
                    }.onSuccess { rooms ->
                        trySend(rooms)
                    }.onFailure { mappingError ->
                        close(mappingError)
                    }
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun createFloor(
        homeId: String,
        name: String,
        level: Int,
        gridColumns: Int,
        gridRows: Int,
    ): String {
        return floorsCollection(homeId)
            .add(
                mapOf(
                    "name" to name.trim(),
                    "level" to level,
                    "gridColumns" to gridColumns,
                    "gridRows" to gridRows,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
            .id
    }

    override suspend fun createRoom(
        homeId: String,
        floorId: String,
        room: RoomLayout,
    ): String {
        return roomsCollection(homeId, floorId)
            .add(
                mapOf(
                    "name" to room.name.trim(),
                    "column" to room.column,
                    "row" to room.row,
                    "width" to room.width,
                    "height" to room.height,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
            .id
    }

    override suspend fun updateFloor(
        homeId: String,
        floor: FloorPlan,
    ) {
        floorsCollection(homeId)
            .document(floor.id)
            .update(
                mapOf(
                    "name" to floor.name.trim(),
                    "level" to floor.level,
                    "gridColumns" to floor.gridColumns,
                    "gridRows" to floor.gridRows,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    override suspend fun updateRoom(
        homeId: String,
        floorId: String,
        room: RoomLayout,
    ) {
        roomsCollection(homeId, floorId)
            .document(room.id)
            .update(
                mapOf(
                    "name" to room.name.trim(),
                    "column" to room.column,
                    "row" to room.row,
                    "width" to room.width,
                    "height" to room.height,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    override suspend fun deleteFloor(
        homeId: String,
        floorId: String,
    ) {
        val assignedDevices = firestore
            .collection("homes")
            .document(homeId)
            .collection("devices")
            .whereEqualTo("floorId", floorId)
            .limit(1)
            .get()
            .await()

        if (!assignedDevices.isEmpty) {
            throw FloorContainsDevicesException()
        }

        val floorDocument = floorsCollection(homeId).document(floorId)
        val roomDocuments = floorDocument.collection("rooms").get().await().documents

        check(roomDocuments.size < 500) {
            "This floor contains too many rooms for one atomic deletion."
        }

        val batch = firestore.batch()
        roomDocuments.forEach { room -> batch.delete(room.reference) }
        batch.delete(floorDocument)
        batch.commit().await()
    }

    override suspend fun deleteRoom(
        homeId: String,
        floorId: String,
        roomId: String,
    ) {
        val assignedDevices = firestore
            .collection("homes")
            .document(homeId)
            .collection("devices")
            .whereEqualTo("roomId", roomId)
            .limit(1)
            .get()
            .await()

        if (!assignedDevices.isEmpty) {
            throw RoomContainsDevicesException()
        }

        roomsCollection(homeId, floorId)
            .document(roomId)
            .delete()
            .await()
    }

    private fun floorsCollection(homeId: String) = firestore
        .collection("homes")
        .document(homeId)
        .collection("floors")

    private fun roomsCollection(
        homeId: String,
        floorId: String,
    ) = floorsCollection(homeId)
        .document(floorId)
        .collection("rooms")
}

private fun DocumentSnapshot.toFloorPlan(): FloorPlan {
    return FloorPlan(
        id = id,
        name = getString("name")
            ?: throw IllegalStateException("Floor name is missing."),
        level = requiredInt("level"),
        gridColumns = requiredInt("gridColumns"),
        gridRows = requiredInt("gridRows"),
    )
}

private fun DocumentSnapshot.toRoomLayout(): RoomLayout {
    return RoomLayout(
        id = id,
        name = getString("name")
            ?: throw IllegalStateException("Room name is missing."),
        column = requiredInt("column"),
        row = requiredInt("row"),
        width = requiredInt("width"),
        height = requiredInt("height"),
    )
}

private fun DocumentSnapshot.requiredInt(field: String): Int {
    val value = getLong(field)
        ?: throw IllegalStateException("$field is missing.")

    if (value !in Int.MIN_VALUE..Int.MAX_VALUE) {
        throw IllegalStateException("$field is outside the supported range.")
    }

    return value.toInt()
}
