package com.smarthome.app.ui.outlet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.DeviceProfile
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.RoomLayout
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.defaultFloorName
import com.smarthome.app.ui.theme.SmartHomeIcons
import com.smarthome.app.ui.theme.SmartHomeThemeColors

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
    onDeviceCreated: (String, DeviceProfile, Int, Int, Int, Int, String) -> Unit,
    onDeviceMoved: (String, Int, Int) -> Unit,
    onDeviceDeleted: (String) -> Unit,
) {
    var showFloorDialog by remember { mutableStateOf(false) }
    var showRoomDialog by remember { mutableStateOf(false) }
    var confirmFloorDeletion by remember { mutableStateOf(false) }
    var roomPendingDeletion by remember { mutableStateOf<String?>(null) }
    var showFloorEditDialog by remember { mutableStateOf(false) }
    var roomPendingEdit by remember { mutableStateOf<RoomLayout?>(null) }
    var showPlacementDialog by remember { mutableStateOf(false) }
    var deviceCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var roomDraft by remember { mutableStateOf<RoomLayout?>(null) }
    var selectedDevice by remember { mutableStateOf<SmartDevice?>(null) }
    var movingDevice by remember { mutableStateOf<SmartDevice?>(null) }
    var devicePendingDeletion by remember { mutableStateOf<SmartDevice?>(null) }

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
                    devices = state.devices.filter { device -> device.floorId == floor.id },
                    onEmptyCellLongPressed = { column, row -> deviceCell = column to row },
                    onEmptyAreaDragged = { column, row, width, height ->
                        roomDraft = RoomLayout("", "", column, row, width, height)
                    },
                    onDeviceTapped = { device -> selectedDevice = device },
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
                        TextButton(onClick = { deviceCell = 0 to 0 }) {
                            Text("Add device")
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

    roomDraft?.let { draft ->
        AddRoomDialog(
            isSaving = state.isSavingLayout,
            initialRoom = draft,
            onDismiss = { roomDraft = null },
            onSubmit = { name, column, row, width, height ->
                onRoomCreated(name, column, row, width, height)
                roomDraft = null
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

    deviceCell?.let { (column, row) ->
        AddDeviceDialog(
            initialColumn = column,
            initialRow = row,
            isSaving = state.isCreatingDevice,
            onDismiss = { deviceCell = null },
            onSubmit = { name, profile, deviceColumn, deviceRow, channels, duration, uri ->
                onDeviceCreated(
                    name,
                    profile,
                    deviceColumn,
                    deviceRow,
                    channels,
                    duration,
                    uri,
                )
                deviceCell = null
            },
        )
    }

    selectedDevice?.let { device ->
        DeviceDetailDialog(
            device = device,
            onDismiss = { selectedDevice = null },
            onMove = {
                selectedDevice = null
                movingDevice = device
            },
            onDelete = {
                selectedDevice = null
                devicePendingDeletion = device
            },
        )
    }

    movingDevice?.let { device ->
        PlacementDialog(
            title = "Move ${device.name}",
            initialColumn = device.column,
            initialRow = device.row,
            onDismiss = { movingDevice = null },
            onSubmit = { column, row ->
                onDeviceMoved(device.id, column, row)
                movingDevice = null
            },
        )
    }

    devicePendingDeletion?.let { device ->
        ConfirmationDialog(
            title = "Delete ${device.name}?",
            message = "This removes the device. Existing event history is not deleted.",
            onDismiss = { devicePendingDeletion = null },
            onConfirm = {
                onDeviceDeleted(device.id)
                devicePendingDeletion = null
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
    devices: List<SmartDevice>,
    onEmptyCellLongPressed: (Int, Int) -> Unit,
    onEmptyAreaDragged: (Int, Int, Int, Int) -> Unit,
    onDeviceTapped: (SmartDevice) -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val roomBackground = MaterialTheme.colorScheme.secondaryContainer
    val roomBorder = MaterialTheme.colorScheme.secondary
    val hapticFeedback = LocalHapticFeedback.current
    var dragStartCell by remember(floor.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragEndCell by remember(floor.id) { mutableStateOf<Pair<Int, Int>?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(floor.gridColumns.toFloat() / floor.gridRows)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .pointerInput(floor.id, devices) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val column = (offset.x / size.width * floor.gridColumns)
                            .toInt().coerceIn(0, floor.gridColumns - 1)
                        val row = (offset.y / size.height * floor.gridRows)
                            .toInt().coerceIn(0, floor.gridRows - 1)
                        if (devices.none { it.column == column && it.row == row }) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEmptyCellLongPressed(column, row)
                        }
                    },
                )
            }
            .pointerInput(floor.id, floor.rooms) {
                fun Offset.toCell(): Pair<Int, Int> {
                    val column = (x / size.width * floor.gridColumns)
                        .toInt().coerceIn(0, floor.gridColumns - 1)
                    val row = (y / size.height * floor.gridRows)
                        .toInt().coerceIn(0, floor.gridRows - 1)
                    return column to row
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        val cell = offset.toCell()
                        val startsInsideRoom = floor.rooms.any { room ->
                            cell.first in room.column until room.right &&
                                cell.second in room.row until room.bottom
                        }
                        if (!startsInsideRoom) {
                            dragStartCell = cell
                            dragEndCell = cell
                        }
                    },
                    onDrag = { change, _ ->
                        if (dragStartCell != null) dragEndCell = change.position.toCell()
                    },
                    onDragCancel = {
                        dragStartCell = null
                        dragEndCell = null
                    },
                    onDragEnd = {
                        val start = dragStartCell
                        val end = dragEndCell
                        if (start != null && end != null) {
                            val left = minOf(start.first, end.first)
                            val top = minOf(start.second, end.second)
                            val right = maxOf(start.first, end.first)
                            val bottom = maxOf(start.second, end.second)
                            onEmptyAreaDragged(
                                left,
                                top,
                                right - left + 1,
                                bottom - top + 1,
                            )
                        }
                        dragStartCell = null
                        dragEndCell = null
                    },
                )
            },
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
                    .background(
                        color = roomBackground,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(1.5.dp, roomBorder, shape = RoundedCornerShape(4.dp))
                    .padding(6.dp),
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val start = dragStartCell
        val end = dragEndCell
        if (start != null && end != null) {
            val left = minOf(start.first, end.first)
            val top = minOf(start.second, end.second)
            val right = maxOf(start.first, end.first)
            val bottom = maxOf(start.second, end.second)
            Box(
                modifier = Modifier
                    .offset(x = cellWidth * left, y = cellHeight * top)
                    .size(
                        width = cellWidth * (right - left + 1),
                        height = cellHeight * (bottom - top + 1),
                    )
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
            )
        }

        devices.forEach { device ->
            val isDeviceOn = device.reportedStatus == DeviceStatus.ON
            val isError = device.reportedStatus == DeviceStatus.ERROR
            val isDisconnected = device.reportedStatus == DeviceStatus.DISCONNECTED

            val (badgeBg, badgeBorder, iconTint) = when {
                isError -> Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.onErrorContainer
                )
                isDisconnected -> Triple(
                    SmartHomeThemeColors.statusDisconnectedContainer,
                    SmartHomeThemeColors.statusDisconnected,
                    SmartHomeThemeColors.statusDisconnected
                )
                isDeviceOn -> Triple(
                    SmartHomeThemeColors.statusOnContainer,
                    SmartHomeThemeColors.statusOn,
                    SmartHomeThemeColors.statusOn
                )
                else -> Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.outline,
                    MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = cellWidth * device.column,
                        y = cellHeight * device.row,
                    )
                    .size(cellWidth, cellHeight)
                    .padding(2.dp)
                    .background(badgeBg, RoundedCornerShape(6.dp))
                    .border(1.5.dp, badgeBorder, RoundedCornerShape(6.dp))
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${device.name}, ${device.profile.displayName}"
                        stateDescription = device.reportedStatus.name
                        role = Role.Button
                    }
                    .clickable { onDeviceTapped(device) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (device.profile) {
                        DeviceProfile.LIGHT -> SmartHomeIcons.Light
                        DeviceProfile.CAMERA -> SmartHomeIcons.Camera
                        DeviceProfile.SAFETY_OUTLET -> SmartHomeIcons.Safety
                        DeviceProfile.MULTI_SWITCH -> SmartHomeIcons.Power
                        DeviceProfile.OUTLET -> SmartHomeIcons.Power
                    },
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailDialog(
    device: SmartDevice,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(device.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(device.profile.displayName)
                Text("Status: ${device.reportedStatus.name}")
                Text("Position: ${device.column}, ${device.row}")
                Text("Tap Move to enter another valid grid coordinate.")
            }
        },
        confirmButton = { TextButton(onClick = onMove) { Text("Move") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

private val cameraSampleUris = listOf(
    "Living room" to "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=640&q=80",
    "Kitchen" to "https://images.unsplash.com/photo-1484154218962-a197022b5858?w=640&q=80",
    "Front door" to "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=640&q=80",
)

@Composable
private fun AddDeviceDialog(
    initialColumn: Int,
    initialRow: Int,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, DeviceProfile, Int, Int, Int, Int, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf(DeviceProfile.OUTLET) }
    var column by remember { mutableStateOf(initialColumn.toString()) }
    var row by remember { mutableStateOf(initialRow.toString()) }
    var channels by remember { mutableStateOf("2") }
    var duration by remember { mutableStateOf("15") }
    var mediaUri by remember { mutableStateOf(cameraSampleUris.first().second) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Device profile", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    DeviceProfile.entries.forEach { option ->
                        if (option == profile) {
                            Button(onClick = { profile = option }) { Text(option.displayName) }
                        } else {
                            TextButton(onClick = { profile = option }) { Text(option.displayName) }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = column,
                        onValueChange = { column = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Column") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = row,
                        onValueChange = { row = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Row") },
                        singleLine = true,
                    )
                }
                when (profile) {
                    DeviceProfile.MULTI_SWITCH -> OutlinedTextField(
                        value = channels,
                        onValueChange = { channels = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Channels (2, 3, or 5)") },
                        singleLine = true,
                    )
                    DeviceProfile.SAFETY_OUTLET -> OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Maximum on-time (minutes)") },
                        singleLine = true,
                    )
                    DeviceProfile.CAMERA -> {
                        OutlinedTextField(
                            value = mediaUri,
                            onValueChange = { mediaUri = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Mock media HTTPS URI") },
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sample snapshots", style = MaterialTheme.typography.labelLarge)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            cameraSampleUris.forEach { (label, uri) ->
                                if (mediaUri == uri) {
                                    Button(onClick = { mediaUri = uri }) { Text(label) }
                                } else {
                                    TextButton(onClick = { mediaUri = uri }) { Text(label) }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(
                    "Tip: long-pressing a grid cell prefills its coordinate.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val parsedColumn = column.toIntOrNull()
                    val parsedRow = row.toIntOrNull()
                    val parsedChannels = channels.toIntOrNull()
                    val parsedDuration = duration.toIntOrNull()
                    if (name.isBlank()) {
                        error = "Enter a device name."
                    } else if (
                        parsedColumn == null || parsedRow == null ||
                        parsedChannels == null || parsedDuration == null
                    ) {
                        error = "Coordinates and configuration must be whole numbers."
                    } else {
                        onSubmit(
                            name,
                            profile,
                            parsedColumn,
                            parsedRow,
                            parsedChannels,
                            parsedDuration,
                            mediaUri,
                        )
                    }
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
            val parsedLevel = level.toIntOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (floor == null) {
                    Text(
                        text = "Name: ${parsedLevel?.let(::defaultFloorName) ?: "Enter a level"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                LayoutForm(
                    fields = buildList {
                        if (floor != null) add(LayoutField("Name", name) { name = it })
                        add(LayoutField("Level (ground = 0)", level) { level = it })
                        add(LayoutField("Columns", columns) { columns = it })
                        add(LayoutField("Rows", rows) { rows = it })
                    },
                    error = formError,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val values = listOf(level, columns, rows).map(String::toIntOrNull)
                    if (values.any { it == null }) {
                        formError = "Level and grid dimensions must be whole numbers."
                    } else {
                        val resolvedName = if (floor == null) {
                            defaultFloorName(values[0]!!)
                        } else {
                            name
                        }
                        onSubmit(resolvedName, values[0]!!, values[1]!!, values[2]!!)
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
    initialRoom: RoomLayout? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int, Int) -> Unit,
) {
    RoomDialog(initialRoom, isSaving, onDismiss, onSubmit)
}

@Composable
private fun RoomDialog(
    room: RoomLayout?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int, Int) -> Unit,
) {
    val isNew = room == null || room.id.isBlank()
    var name by remember { mutableStateOf(room?.name.orEmpty()) }
    var column by remember { mutableStateOf(room?.column?.toString() ?: "0") }
    var row by remember { mutableStateOf(room?.row?.toString() ?: "0") }
    var width by remember { mutableStateOf(room?.width?.toString() ?: "4") }
    var height by remember { mutableStateOf(room?.height?.toString() ?: "4") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add room" else "Edit room") },
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
            ) { Text(if (isNew) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PlacementDialog(
    title: String = "Place outlet",
    initialColumn: Int = 0,
    initialRow: Int = 0,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int) -> Unit,
) {
    var column by remember { mutableStateOf(initialColumn.toString()) }
    var row by remember { mutableStateOf(initialRow.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
