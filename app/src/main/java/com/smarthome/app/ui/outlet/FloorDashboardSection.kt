package com.smarthome.app.ui.outlet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.RoomLayout

@Composable
fun FloorDashboardSection(
    state: OutletUiState,
    onFloorSelected: (String) -> Unit,
    onFloorCreated: (String, Int, Int, Int) -> Unit,
    onRoomCreated: (String, Int, Int, Int, Int) -> Unit,
    onFloorDeleted: () -> Unit,
    onRoomDeleted: (String) -> Unit,
    onFloorUpdated: (String, Int, Int, Int) -> Unit,
    onRoomUpdated: (String, String, Int, Int, Int, Int) -> Unit,
    onOutletPlaced: (Int, Int) -> Unit,
) {
    var showFloorDialog by remember { mutableStateOf(false) }
    var showRoomDialog by remember { mutableStateOf(false) }
    var confirmFloorDeletion by remember { mutableStateOf(false) }
    var roomPendingDeletion by remember { mutableStateOf<String?>(null) }
    var showFloorEditDialog by remember { mutableStateOf(false) }
    var roomPendingEdit by remember { mutableStateOf<RoomLayout?>(null) }
    var showPlacementDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Floor plan",
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = { showFloorDialog = true }) {
            Text("Add floor")
        }
    }

    when {
        state.isLoadingFloors -> {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        state.floors.isEmpty() -> {
            Text("No floor plans yet. Add a floor to begin the layout.")
        }

        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.floors.forEach { floor ->
                    if (floor.id == state.selectedFloorId) {
                        Button(onClick = { onFloorSelected(floor.id) }) {
                            Text(floor.name)
                        }
                    } else {
                        OutlinedButton(onClick = { onFloorSelected(floor.id) }) {
                            Text(floor.name)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.selectedFloor?.let { floor ->
                FloorGrid(
                    floor = floor,
                    outlet = state.outlet?.takeIf { outlet -> outlet.floorId == floor.id },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${floor.gridColumns} × ${floor.gridRows} grid · " +
                        "${floor.rooms.size} rooms",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                        TextButton(onClick = { showFloorEditDialog = true }) {
                            Text("Edit floor")
                        }
                        TextButton(onClick = { confirmFloorDeletion = true }) {
                            Text("Delete floor")
                        }
                        TextButton(onClick = { showRoomDialog = true }) {
                            Text("Add room")
                        }
                        TextButton(onClick = { showPlacementDialog = true }) {
                            Text("Place outlet")
                        }
                }

                floor.rooms.forEach { room ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(room.name, modifier = Modifier.padding(vertical = 12.dp))
                        Row {
                            TextButton(onClick = { roomPendingEdit = room }) {
                                Text("Edit")
                            }
                            TextButton(onClick = { roomPendingDeletion = room.id }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    state.layoutMessage?.let { message ->
        Text(
            text = message,
            color = if (
                message.endsWith("created.") ||
                message.endsWith("updated.") ||
                message.endsWith("placed.") ||
                message.endsWith("deleted.")
            ) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (showFloorDialog) {
        AddFloorDialog(
            isSaving = state.isSavingLayout,
            onDismiss = { showFloorDialog = false },
            onSubmit = { name, level, columns, rows ->
                onFloorCreated(name, level, columns, rows)
                showFloorDialog = false
            },
        )
    }

    if (showFloorEditDialog) {
        state.selectedFloor?.let { floor ->
            FloorDialog(
                floor = floor,
                isSaving = state.isSavingLayout,
                onDismiss = { showFloorEditDialog = false },
                onSubmit = { name, level, columns, rows ->
                    onFloorUpdated(name, level, columns, rows)
                    showFloorEditDialog = false
                },
            )
        }
    }

    if (showRoomDialog) {
        AddRoomDialog(
            isSaving = state.isSavingLayout,
            onDismiss = { showRoomDialog = false },
            onSubmit = { name, column, row, width, height ->
                onRoomCreated(name, column, row, width, height)
                showRoomDialog = false
            },
        )
    }

    roomPendingEdit?.let { room ->
        RoomDialog(
            room = room,
            isSaving = state.isSavingLayout,
            onDismiss = { roomPendingEdit = null },
            onSubmit = { name, column, row, width, height ->
                onRoomUpdated(room.id, name, column, row, width, height)
                roomPendingEdit = null
            },
        )
    }

    if (showPlacementDialog) {
        PlacementDialog(
            onDismiss = { showPlacementDialog = false },
            onSubmit = { column, row ->
                onOutletPlaced(column, row)
                showPlacementDialog = false
            },
        )
    }

    if (confirmFloorDeletion) {
        ConfirmationDialog(
            title = "Delete floor?",
            message = "All rooms on this floor will also be deleted. Floors containing devices cannot be deleted.",
            onDismiss = { confirmFloorDeletion = false },
            onConfirm = {
                confirmFloorDeletion = false
                onFloorDeleted()
            },
        )
    }

    roomPendingDeletion?.let { roomId ->
        ConfirmationDialog(
            title = "Delete room?",
            message = "This removes the room from the floor layout. Rooms containing devices cannot be deleted.",
            onDismiss = { roomPendingDeletion = null },
            onConfirm = {
                roomPendingDeletion = null
                onRoomDeleted(roomId)
            },
        )
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun FloorGrid(
    floor: FloorPlan,
    outlet: com.smarthome.app.domain.model.OutletDevice?,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val roomBackground = MaterialTheme.colorScheme.secondaryContainer
    val roomBorder = MaterialTheme.colorScheme.secondary

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(floor.gridColumns.toFloat() / floor.gridRows)
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        val cellWidth = maxWidth / floor.gridColumns
        val cellHeight = maxHeight / floor.gridRows

        Canvas(modifier = Modifier.matchParentSize()) {
            val columnWidth = size.width / floor.gridColumns
            val rowHeight = size.height / floor.gridRows

            for (column in 1 until floor.gridColumns) {
                val x = columnWidth * column
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height))
            }
            for (row in 1 until floor.gridRows) {
                val y = rowHeight * row
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y))
            }
        }

        floor.rooms.forEach { room ->
            Box(
                modifier = Modifier
                    .offset(
                        x = cellWidth * room.column,
                        y = cellHeight * room.row,
                    )
                    .size(
                        width = cellWidth * room.width,
                        height = cellHeight * room.height,
                    )
                    .background(roomBackground)
                    .border(2.dp, roomBorder)
                    .padding(4.dp),
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        outlet?.let { device ->
            Box(
                modifier = Modifier
                    .offset(
                        x = cellWidth * device.column,
                        y = cellHeight * device.row,
                    )
                    .size(cellWidth, cellHeight)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .border(2.dp, MaterialTheme.colorScheme.error)
                    .padding(2.dp),
            ) {
                Text(
                    text = "Outlet\n${device.reportedStatus.name}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AddFloorDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int) -> Unit,
) {
    FloorDialog(null, isSaving, onDismiss, onSubmit)
}

@Composable
private fun FloorDialog(
    floor: FloorPlan?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int) -> Unit,
) {
    var name by remember { mutableStateOf(floor?.name.orEmpty()) }
    var level by remember { mutableStateOf(floor?.level?.toString() ?: "0") }
    var columns by remember { mutableStateOf(floor?.gridColumns?.toString() ?: "12") }
    var rows by remember { mutableStateOf(floor?.gridRows?.toString() ?: "16") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (floor == null) "Add floor" else "Edit floor") },
        text = {
            LayoutForm(
                fields = listOf(
                    LayoutField("Name", name) { name = it },
                    LayoutField("Level (ground = 0)", level) { level = it },
                    LayoutField("Columns", columns) { columns = it },
                    LayoutField("Rows", rows) { rows = it },
                ),
                error = formError,
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val values = listOf(level, columns, rows).map(String::toIntOrNull)
                    if (values.any { it == null }) {
                        formError = "Level and grid dimensions must be whole numbers."
                    } else {
                        onSubmit(name, values[0]!!, values[1]!!, values[2]!!)
                    }
                },
            ) { Text(if (floor == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddRoomDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int, Int) -> Unit,
) {
    RoomDialog(null, isSaving, onDismiss, onSubmit)
}

@Composable
private fun RoomDialog(
    room: RoomLayout?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int, Int) -> Unit,
) {
    var name by remember { mutableStateOf(room?.name.orEmpty()) }
    var column by remember { mutableStateOf(room?.column?.toString() ?: "0") }
    var row by remember { mutableStateOf(room?.row?.toString() ?: "0") }
    var width by remember { mutableStateOf(room?.width?.toString() ?: "4") }
    var height by remember { mutableStateOf(room?.height?.toString() ?: "4") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (room == null) "Add room" else "Edit room") },
        text = {
            LayoutForm(
                fields = listOf(
                    LayoutField("Name", name) { name = it },
                    LayoutField("Column", column) { column = it },
                    LayoutField("Row", row) { row = it },
                    LayoutField("Width", width) { width = it },
                    LayoutField("Height", height) { height = it },
                ),
                error = formError,
            )
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val values = listOf(column, row, width, height).map(String::toIntOrNull)
                    if (values.any { it == null }) {
                        formError = "Coordinates and dimensions must be whole numbers."
                    } else {
                        onSubmit(name, values[0]!!, values[1]!!, values[2]!!, values[3]!!)
                    }
                },
            ) { Text(if (room == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PlacementDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, Int) -> Unit,
) {
    var column by remember { mutableStateOf("0") }
    var row by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Place outlet") },
        text = {
            LayoutForm(
                fields = listOf(
                    LayoutField("Column", column) { column = it },
                    LayoutField("Row", row) { row = it },
                ),
                error = error,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedColumn = column.toIntOrNull()
                val parsedRow = row.toIntOrNull()
                if (parsedColumn == null || parsedRow == null) {
                    error = "Coordinates must be whole numbers."
                } else {
                    onSubmit(parsedColumn, parsedRow)
                }
            }) { Text("Place") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private data class LayoutField(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
)

@Composable
private fun LayoutForm(
    fields: List<LayoutField>,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEach { field ->
            OutlinedTextField(
                value = field.value,
                onValueChange = field.onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(field.label) },
                singleLine = true,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
