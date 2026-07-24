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

@Composable
fun FloorDashboardSection(
    state: OutletUiState,
    onFloorSelected: (String) -> Unit,
    onFloorCreated: (String, Int, Int, Int) -> Unit,
    onRoomCreated: (String, Int, Int, Int, Int) -> Unit,
) {
    var showFloorDialog by remember { mutableStateOf(false) }
    var showRoomDialog by remember { mutableStateOf(false) }

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
                FloorGrid(floor = floor)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${floor.gridColumns} × ${floor.gridRows} grid · " +
                            "${floor.rooms.size} rooms",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { showRoomDialog = true }) {
                        Text("Add room")
                    }
                }
            }
        }
    }

    state.layoutMessage?.let { message ->
        Text(
            text = message,
            color = if (message.endsWith("created.")) {
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
}

@Composable
private fun FloorGrid(floor: FloorPlan) {
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
    }
}

@Composable
private fun AddFloorDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, Int, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("0") }
    var columns by remember { mutableStateOf("12") }
    var rows by remember { mutableStateOf("16") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add floor") },
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
            ) { Text("Create") }
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
    var name by remember { mutableStateOf("") }
    var column by remember { mutableStateOf("0") }
    var row by remember { mutableStateOf("0") }
    var width by remember { mutableStateOf("4") }
    var height by remember { mutableStateOf("4") }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add room") },
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
            ) { Text("Create") }
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
