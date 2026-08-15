package com.smarthome.app.ui.outlet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.DeviceProfile
import com.smarthome.app.domain.model.DeviceConfiguration
import com.smarthome.app.domain.model.DeviceEvent
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.CameraConnectivity
import com.smarthome.app.domain.model.AlertSeverity
import com.smarthome.app.domain.model.HomeAlert
import com.smarthome.app.domain.model.toCameraConnectivity
import com.smarthome.app.domain.usage.EnergyEstimator
import com.smarthome.app.domain.usage.EnergyEstimate
import com.smarthome.app.domain.usage.UsageCalculator
import com.smarthome.app.domain.usage.UsageReport
import com.smarthome.app.ui.theme.SmartHomeIcons
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.smarthome.app.ui.theme.SmartHomeThemeColors
import coil.compose.AsyncImage

@Composable
fun OutletRoute(
    viewModel: OutletViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OutletScreen(
        state = state,
        onEmailChanged = viewModel::updateEmail,
        onPasswordChanged = viewModel::updatePassword,
        onSignIn = viewModel::signIn,
        onSignOut = viewModel::signOut,
        onPowerStateRequested = viewModel::requestPowerState,
        onSwitchChannelStateRequested = viewModel::requestSwitchChannelState,
        onDevicePowerStateRequested = viewModel::requestDevicePowerState,
        onLightScheduleUpdated = viewModel::updateLightSchedule,
        onFloorSelected = viewModel::selectFloor,
        onFloorCreated = viewModel::createFloor,
        onRoomCreated = viewModel::createRoom,
        onFloorDeleted = viewModel::deleteSelectedFloor,
        onRoomDeleted = viewModel::deleteRoom,
        onFloorUpdated = viewModel::updateSelectedFloor,
        onRoomUpdated = viewModel::updateRoom,
        onOutletPlaced = viewModel::placeOutlet,
        onDeviceCreated = viewModel::createDevice,
        onDeviceMoved = viewModel::moveDevice,
        onDeviceDeleted = viewModel::deleteDevice,
    )
}

@Composable
private fun OutletScreen(
    state: OutletUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onPowerStateRequested: (PowerState) -> Unit,
    onSwitchChannelStateRequested: (String, String, PowerState) -> Unit,
    onDevicePowerStateRequested: (String, PowerState) -> Unit,
    onLightScheduleUpdated: (String, Boolean, String, String, String) -> Unit,
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
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.isAuthenticated) {
            OutletDashboard(
                state = state,
                onSignOut = onSignOut,
                onPowerStateRequested = onPowerStateRequested,
                onSwitchChannelStateRequested = onSwitchChannelStateRequested,
                onDevicePowerStateRequested = onDevicePowerStateRequested,
                onLightScheduleUpdated = onLightScheduleUpdated,
                onFloorSelected = onFloorSelected,
                onFloorCreated = onFloorCreated,
                onRoomCreated = onRoomCreated,
                onFloorDeleted = onFloorDeleted,
                onRoomDeleted = onRoomDeleted,
                onFloorUpdated = onFloorUpdated,
                onRoomUpdated = onRoomUpdated,
                onOutletPlaced = onOutletPlaced,
                onDeviceCreated = onDeviceCreated,
                onDeviceMoved = onDeviceMoved,
                onDeviceDeleted = onDeviceDeleted,
            )
        } else {
            SignInScreen(
                state = state,
                onEmailChanged = onEmailChanged,
                onPasswordChanged = onPasswordChanged,
                onSignIn = onSignIn,
            )
        }
    }
}

@Composable
private fun SignInScreen(
    state: OutletUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Smart Home",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in to monitor and control your home.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSigningIn,
            label = {
                Text("Email")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSigningIn,
            label = {
                Text("Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSignIn()
                },
            ),
            singleLine = true,
        )

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSigningIn,
        ) {
            if (state.isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Sign in")
            }
        }
    }
}

@Composable
private fun OutletDashboard(
    state: OutletUiState,
    onSignOut: () -> Unit,
    onPowerStateRequested: (PowerState) -> Unit,
    onSwitchChannelStateRequested: (String, String, PowerState) -> Unit,
    onDevicePowerStateRequested: (String, PowerState) -> Unit,
    onLightScheduleUpdated: (String, Boolean, String, String, String) -> Unit,
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var scheduleDevice by remember { mutableStateOf<SmartDevice?>(null) }
    var lastReadSafetyAlertTime by rememberSaveable { androidx.compose.runtime.mutableLongStateOf(0L) }

    androidx.compose.runtime.LaunchedEffect(selectedTab, state.reportAlerts) {
        if (selectedTab == 3) {
            val maxTime = state.reportAlerts
                .filter { it.severity == com.smarthome.app.domain.model.AlertSeverity.CRITICAL }
                .maxOfOrNull { it.createdAtMillis } ?: 0L
            if (maxTime > lastReadSafetyAlertTime) {
                lastReadSafetyAlertTime = maxTime
            }
        }
    }

    scheduleDevice?.let { device ->
        LightScheduleDialog(
            device = device,
            isSaving = device.id in state.scheduleUpdatesInFlight,
            onDismiss = { scheduleDevice = null },
            onSave = { enabled, start, end, timezone ->
                onLightScheduleUpdated(device.id, enabled, start, end, timezone)
                scheduleDevice = null
            },
        )
    }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    val filters = listOf("All", "Lights", "Outlets", "Cameras", "Switches", "Active")

    val filteredDevices = remember(state.devices, selectedFilter) {
        when (selectedFilter) {
            "Lights" -> state.devices.filter { it.profile == DeviceProfile.LIGHT }
            "Outlets" -> state.devices.filter { it.profile == DeviceProfile.OUTLET || it.profile == DeviceProfile.SAFETY_OUTLET }
            "Cameras" -> state.devices.filter { it.profile == DeviceProfile.CAMERA }
            "Switches" -> state.devices.filter { it.profile == DeviceProfile.MULTI_SWITCH }
            "Active" -> state.devices.filter { it.reportedStatus == DeviceStatus.ON }
            else -> state.devices
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        SmartHomeIcons.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "Primary home",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = state.selectedFloor?.name ?: "No floor selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }

                TextButton(onClick = { selectedTab = 3 }) {
                    Icon(
                        SmartHomeIcons.Profile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Profile", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (state.isOffline || state.isRecovering) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        SmartHomeIcons.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = when {
                            state.isOffline ->
                                "Offline — showing cached data. Commands will retry automatically when the connection returns."
                            else ->
                                "Reconnecting… the latest state will appear shortly."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Devices") },
                icon = { Icon(SmartHomeIcons.Devices, contentDescription = null) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Usage") },
                icon = { Icon(SmartHomeIcons.Usage, contentDescription = null) },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Layout") },
                icon = { Icon(SmartHomeIcons.Layout, contentDescription = null) },
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Activity") },
                icon = { Icon(SmartHomeIcons.Report, contentDescription = null) },
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Safety") },
                icon = { 
                    val criticalCount = state.reportAlerts.count { it.severity == com.smarthome.app.domain.model.AlertSeverity.CRITICAL && it.createdAtMillis > lastReadSafetyAlertTime }
                    if (criticalCount > 0) {
                        androidx.compose.material3.BadgedBox(
                            badge = { androidx.compose.material3.Badge { Text(criticalCount.toString()) } }
                        ) {
                            Icon(SmartHomeIcons.Safety, contentDescription = null)
                        }
                    } else {
                        Icon(SmartHomeIcons.Safety, contentDescription = null)
                    }
                },
            )
            Tab(
                selected = selectedTab == 5,
                onClick = { selectedTab = 5 },
                text = { Text("Profile") },
                icon = { Icon(SmartHomeIcons.Profile, contentDescription = null) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryTile(
                    label = "Online",
                    value = state.devices.count { it.reportedStatus != DeviceStatus.DISCONNECTED }.toString(),
                    icon = SmartHomeIcons.Devices,
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    label = "Active",
                    value = state.devices.count { it.reportedStatus == DeviceStatus.ON }.toString(),
                    icon = SmartHomeIcons.Power,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))



            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Your devices",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Monitor confirmed state and send power commands.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoadingDevices -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading devices…")
                }

                filteredDevices.isEmpty() -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                SmartHomeIcons.Devices,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilter == "All") "No devices yet. Add one from the Layout tab." else "No devices matching '$selectedFilter'.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                else -> filteredDevices.forEach { device ->
                    val floorName = state.floors.firstOrNull { it.id == device.floorId }?.name
                        ?: "Unknown floor"
                    val roomName = state.rooms
                        .takeIf { device.floorId == state.selectedFloorId }
                        ?.firstOrNull { it.id == device.roomId }
                        ?.name
                    DeviceSummaryCard(
                        device = device,
                        locationLabel = listOfNotNull(floorName, roomName).joinToString(" · "),
                        commandsInFlight = state.switchCommandsInFlight,
                        deviceCommandsInFlight = state.deviceCommandsInFlight,
                        onSwitchChannelStateRequested = onSwitchChannelStateRequested,
                        onDevicePowerStateRequested = onDevicePowerStateRequested,
                        onEditSchedule = { scheduleDevice = device },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } else if (selectedTab == 1) {
            ActivitySection(
                devices = state.devices,
                eventsByDevice = state.eventsByDevice,
                isLoading = state.isLoadingEvents,
                eventsErrorMessage = state.eventsErrorMessage,
            )
        } else if (selectedTab == 2) {
            FloorDashboardSection(
                state = state,
                onFloorSelected = onFloorSelected,
                onFloorCreated = onFloorCreated,
                onRoomCreated = onRoomCreated,
                onFloorDeleted = onFloorDeleted,
                onRoomDeleted = onRoomDeleted,
                onFloorUpdated = onFloorUpdated,
                onRoomUpdated = onRoomUpdated,
                onOutletPlaced = onOutletPlaced,
                onDeviceCreated = onDeviceCreated,
                onDeviceMoved = onDeviceMoved,
                onDeviceDeleted = onDeviceDeleted,
            )
        } else if (selectedTab == 2) {
            ReportSection(
                reportAlerts = state.reportAlerts.filter { it.severity == com.smarthome.app.domain.model.AlertSeverity.INFO },
                devices = state.devices,
                isLoading = state.isLoadingReport,
            )
        } else if (selectedTab == 3) {
            SafetySection(
                reportAlerts = state.reportAlerts.filter { it.severity == com.smarthome.app.domain.model.AlertSeverity.CRITICAL },
                devices = state.devices,
                isLoading = state.isLoadingReport,
            )
        } else {
            ProfileSection(
                email = state.accountEmail ?: state.email.takeIf(String::isNotBlank),
                onSignOut = onSignOut,
            )
        }

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AlertCard(
    alert: HomeAlert,
    deviceName: String?,
) {
    val containerColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        AlertSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        AlertSeverity.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = alert.severity.name,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(alert.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Device: ${deviceName ?: "Unknown device"}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DeviceSummaryCard(
    device: SmartDevice,
    locationLabel: String,
    commandsInFlight: Set<String>,
    deviceCommandsInFlight: Set<String>,
    onSwitchChannelStateRequested: (String, String, PowerState) -> Unit,
    onDevicePowerStateRequested: (String, PowerState) -> Unit,
    onEditSchedule: () -> Unit,
) {
    val containerColor = when (device.reportedStatus) {
        DeviceStatus.ON -> MaterialTheme.colorScheme.primaryContainer
        DeviceStatus.OFF -> MaterialTheme.colorScheme.surfaceVariant
        DeviceStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        DeviceStatus.DISCONNECTED -> MaterialTheme.colorScheme.secondaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = deviceProfileIcon(device.profile),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(device.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "${device.profile.displayName} · $locationLabel",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        device.reportedStatus.name,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Grid ${device.column}, ${device.row}", style = MaterialTheme.typography.bodySmall)
            val detail = when (val config = device.configuration) {
                DeviceConfiguration.Outlet -> null
                is DeviceConfiguration.MultiSwitch -> "${config.channelCount} independently controlled channels"
                is DeviceConfiguration.SafetyOutlet ->
                    "Safety cutoff: ${config.maxOnDurationSeconds / 60} minutes"
                is DeviceConfiguration.Light -> if (config.scheduleEnabled) {
                    "Scheduled ${config.startLocalTime}–${config.endLocalTime} · ${config.timezone}"
                } else {
                    "Manual control · automation not configured"
                }
                is DeviceConfiguration.Camera -> "Mock camera media configured"
            }
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (device.profile == DeviceProfile.CAMERA) CameraSnapshot(device)
            val supportsCommonPower = device.profile in setOf(
                DeviceProfile.OUTLET,
                DeviceProfile.SAFETY_OUTLET,
                DeviceProfile.LIGHT,
            )
            if (supportsCommonPower) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                val unavailableReason = when {
                    !device.reportedStatus.acceptsPowerCommands ->
                        "Control unavailable: device reports ${device.reportedStatus.name}."
                    device.commandState == com.smarthome.app.domain.model.CommandState.PENDING ->
                        "Waiting for hardware confirmation…"
                    else -> "Hardware confirmed ${device.reportedStatus.name}."
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Power", style = MaterialTheme.typography.titleSmall)
                        Text(unavailableReason, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = if (device.commandState == com.smarthome.app.domain.model.CommandState.PENDING) {
                            device.desiredStatus == PowerState.ON
                        } else {
                            device.reportedStatus == DeviceStatus.ON
                        },
                        onCheckedChange = { enabled ->
                            onDevicePowerStateRequested(
                                device.id,
                                if (enabled) PowerState.ON else PowerState.OFF,
                            )
                        },
                        enabled = device.reportedStatus.acceptsPowerCommands &&
                            device.commandState != com.smarthome.app.domain.model.CommandState.PENDING &&
                            device.id !in deviceCommandsInFlight,
                    )
                }
            }
            if (device.profile == DeviceProfile.LIGHT) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onEditSchedule,
                    enabled = device.id !in deviceCommandsInFlight,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(SmartHomeIcons.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Edit schedule")
                }
            }
            val multiSwitch = device.configuration as? DeviceConfiguration.MultiSwitch
            multiSwitch?.channels?.forEach { channel ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channel.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (channel.isPending) {
                                "${channel.reportedStatus.name} → ${channel.desiredStatus.name} · PENDING"
                            } else {
                                channel.reportedStatus.name
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    val key = "${device.id}:${channel.id}"
                    Switch(
                        checked = if (channel.isPending) {
                            channel.desiredStatus == PowerState.ON
                        } else {
                            channel.reportedStatus == DeviceStatus.ON
                        },
                        onCheckedChange = { enabled ->
                            onSwitchChannelStateRequested(
                                device.id,
                                channel.id,
                                if (enabled) PowerState.ON else PowerState.OFF,
                            )
                        },
                        enabled = key !in commandsInFlight && device.reportedStatus.acceptsPowerCommands,
                    )
                }
            }
        }
    }
}

private enum class CameraLoadState { LOADING, SUCCESS, ERROR }

@Composable
private fun CameraSnapshot(device: SmartDevice) {
    val configuration = device.configuration as? DeviceConfiguration.Camera ?: return
    var loadState by remember(configuration.mediaUri) { mutableStateOf(CameraLoadState.LOADING) }
    val connectivity = device.reportedStatus.toCameraConnectivity()

    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp)),
    ) {
        AsyncImage(
            model = configuration.mediaUri,
            contentDescription = "Mock camera snapshot from ${device.name}",
            contentScale = ContentScale.Crop,
            onLoading = { loadState = CameraLoadState.LOADING },
            onSuccess = { loadState = CameraLoadState.SUCCESS },
            onError = { loadState = CameraLoadState.ERROR },
            modifier = Modifier.fillMaxSize(),
        )

        if (loadState != CameraLoadState.SUCCESS) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (loadState == CameraLoadState.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading mock snapshot…", color = Color(0xFFCBD5E1))
                } else {
                    Icon(
                        SmartHomeIcons.Camera,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Snapshot unavailable", color = Color(0xFFFECACA))
                }
            }
        }

        Surface(
            color = Color(0xCC000000),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        ) {
            Text(
                "MOCK SNAPSHOT",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }

        Surface(
            color = cameraConnectivityColor(connectivity),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
        ) {
            Text(
                connectivity.displayName,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }

        Text(
            text = when (loadState) {
                CameraLoadState.LOADING -> "LOADING"
                CameraLoadState.SUCCESS -> "AVAILABLE"
                CameraLoadState.ERROR -> "ERROR"
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Captured ${formatCaptureTime(configuration.capturedAtMillis)} · ${configuration.mediaType}",
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun cameraConnectivityColor(connectivity: CameraConnectivity): Color = when (connectivity) {
    CameraConnectivity.ONLINE -> Color(0xFF15803D)
    CameraConnectivity.OFFLINE -> Color(0xFF7C4A00)
    CameraConnectivity.ERROR -> Color(0xFFB91C1C)
}

private fun formatCaptureTime(millis: Long?): String {
    if (millis == null) return "time unknown"
    val instant = Instant.ofEpochMilli(millis)
    return DateTimeFormatter.ofPattern("MMM d, HH:mm")
        .format(instant.atZone(ZoneId.systemDefault()))
}

@Composable
private fun ProfileSection(
    email: String?,
    onSignOut: () -> Unit,
) {
    val accountLabel = email ?: "Authenticated account"
    val initial = accountLabel.firstOrNull()?.uppercase() ?: "U"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Manage the account connected to this home.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            initial,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    Text("Home member", style = MaterialTheme.typography.titleMedium)
                    Text(accountLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Session", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Your Firebase Authentication session protects access to home data and controls.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign out")
                }
            }
        }
    }
}

private val EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, HH:mm")

private fun formatEventTime(millis: Long): String {
    return EVENT_TIME_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

private fun formatCost(cost: Double): String {
    val cents = Math.round(cost * 100).toLong()
    return "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

@Composable
private fun ActivitySection(
    devices: List<SmartDevice>,
    eventsByDevice: Map<String, List<DeviceEvent>>,
    isLoading: Boolean,
    eventsErrorMessage: String?,
) {
    var periodLabel by rememberSaveable { mutableStateOf("7 days") }
    val periodOptions = listOf("Today", "7 days", "30 days")
    val zone = ZoneId.systemDefault()
    val nowMillis = System.currentTimeMillis()
    val periodStart = remember(periodLabel, nowMillis) {
        when (periodLabel) {
            "Today" -> LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            "30 days" -> nowMillis - 30L * 24 * 60 * 60 * 1000
            else -> nowMillis - 7L * 24 * 60 * 60 * 1000
        }
    }

    val latestEventMillis = remember(eventsByDevice) {
        eventsByDevice.values.flatten().maxOfOrNull { it.occurredAtMillis }
    }
    val periodEndMillis = maxOf(nowMillis, latestEventMillis ?: nowMillis)

    val deviceReports = remember(devices, eventsByDevice, periodStart, periodEndMillis) {
        devices.map { device ->
            device to UsageCalculator.report(
                events = eventsByDevice[device.id].orEmpty(),
                periodStartMillis = periodStart,
                periodEndMillis = periodEndMillis,
            )
        }
    }
    val totalActivations = deviceReports.sumOf { it.second.totalActivations }
    val totalDurationMillis = deviceReports.sumOf { it.second.totalDurationMillis }
    val deviceEstimates = remember(deviceReports) {
        deviceReports.associate { (device, report) ->
            device.id to EnergyEstimator.estimateReport(device.profile, report)
        }
    }
    val totalEnergyKwh = deviceEstimates.values.sumOf { it.energyKwh }
    val totalCost = deviceEstimates.values.sumOf { it.cost }

    val recentEvents = remember(eventsByDevice) {
        eventsByDevice
            .flatMap { (deviceId, events) -> events.map { deviceId to it } }
            .sortedByDescending { it.second.occurredAtMillis }
            .take(20)
    }
    val deviceNames = remember(devices) {
        devices.associate { it.id to it.name }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Usage", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Usage pairs ON/OFF events into activation counts and active time. " +
                "Energy is estimated from assumed per-profile wattage; incomplete " +
                "intervals are bounded to the selected period.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        eventsErrorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            periodOptions.forEach { option ->
                FilterChip(
                    selected = periodLabel == option,
                    onClick = { periodLabel = option },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryTile(
                label = "Activations",
                value = totalActivations.toString(),
                icon = SmartHomeIcons.Activity,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "Active time",
                value = UsageCalculator.formatDuration(totalDurationMillis),
                icon = SmartHomeIcons.Schedule,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryTile(
                label = "Est. energy",
                value = EnergyEstimator.formatEnergy(totalEnergyKwh),
                icon = SmartHomeIcons.Usage,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                label = "Est. cost",
                value = formatCost(totalCost),
                icon = SmartHomeIcons.Activity,
                modifier = Modifier.weight(1f),
            )
        }

        Text("Per-device usage", style = MaterialTheme.typography.titleLarge)
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("Loading usage…", style = MaterialTheme.typography.bodyMedium)
            }

            devices.isEmpty() -> Text(
                "No devices yet. Add one from the Layout tab.",
                style = MaterialTheme.typography.bodyMedium,
            )

            else -> deviceReports.forEach { (device, report) ->
                UsageCard(
                    device = device,
                    report = report,
                    periodLabel = periodLabel,
                    estimate = deviceEstimates[device.id] ?: EnergyEstimate(0.0, 0.0),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Text("Recent events", style = MaterialTheme.typography.titleLarge)
        if (recentEvents.isEmpty()) {
            Text(
                "No events recorded yet. Toggle a device to generate usage history.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            recentEvents.forEach { (deviceId, event) ->
                EventRow(
                    event = event,
                    deviceName = deviceNames[deviceId] ?: "Unknown device",
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UsageCard(
    device: SmartDevice,
    report: UsageReport,
    periodLabel: String,
    estimate: EnergyEstimate,
) {
    val channelNames = (device.configuration as? DeviceConfiguration.MultiSwitch)
        ?.channels
        ?.associate { it.id to it.name }
        .orEmpty()
    val channelEntries = report.entries.filter { it.key.isNotEmpty() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        deviceProfileIcon(device.profile),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(device.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${device.profile.displayName} · $periodLabel",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        "${report.totalActivations} activations · " +
                            UsageCalculator.formatDuration(report.totalDurationMillis),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Est. energy ${EnergyEstimator.formatEnergy(estimate.energyKwh)} · " +
                    "Est. cost ${formatCost(estimate.cost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (channelEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                channelEntries.forEach { entry ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            channelNames[entry.key] ?: "Channel ${entry.key}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            buildString {
                                append(entry.usage.activationCount)
                                append(" · ")
                                append(UsageCalculator.formatDuration(entry.usage.durationMillis))
                                if (entry.usage.ongoing) append(" · active now")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: DeviceEvent,
    deviceName: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(deviceName)
                        append(" → ")
                        append(event.toStatus?.name ?: event.type)
                        if (event.channelId != null) append(" · ${event.channelId}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${event.origin.name} · ${event.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatEventTime(event.occurredAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LightScheduleDialog(
    device: SmartDevice,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, String, String) -> Unit,
) {
    val configuration = device.configuration as? DeviceConfiguration.Light ?: return
    var enabled by rememberSaveable(device.id) { mutableStateOf(configuration.scheduleEnabled) }
    var start by rememberSaveable(device.id) { mutableStateOf(configuration.startLocalTime) }
    var end by rememberSaveable(device.id) { mutableStateOf(configuration.endLocalTime) }
    var timezone by rememberSaveable(device.id) { mutableStateOf(configuration.timezone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(SmartHomeIcons.Schedule, contentDescription = null) },
        title = { Text("${device.name} schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Automatic operation")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Turn on (HH:mm)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("Turn off (HH:mm)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = { Text("Timezone") },
                    singleLine = true,
                )
                Text(
                    "Schedules may cross midnight, for example 18:00–06:00.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(enabled, start, end, timezone) },
                enabled = !isSaving,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun deviceProfileIcon(profile: DeviceProfile): ImageVector = when (profile) {
    DeviceProfile.OUTLET -> SmartHomeIcons.Power
    DeviceProfile.MULTI_SWITCH -> SmartHomeIcons.Devices
    DeviceProfile.SAFETY_OUTLET -> SmartHomeIcons.Safety
    DeviceProfile.LIGHT -> SmartHomeIcons.Light
    DeviceProfile.CAMERA -> SmartHomeIcons.Camera
}

@Composable
private fun OutletCard(
    outlet: OutletDevice,
    isSendingCommand: Boolean,
    onPowerStateRequested: (PowerState) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = outlet.name,
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                text = "Electrical outlet",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            StatusRow(
                label = "Desired",
                value = outlet.desiredStatus.name,
            )

            StatusRow(
                label = "Reported",
                value = outlet.reportedStatus.name,
            )

            StatusRow(
                label = "Command",
                value = outlet.commandState.name,
            )

            if (outlet.isCommandPending) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Waiting for hardware confirmation…")
                }
            }

            if (outlet.reportedStatus == DeviceStatus.ERROR) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "The outlet reported an error. Power control is disabled.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (outlet.reportedStatus == DeviceStatus.DISCONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "The outlet is disconnected. Power control is disabled.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val controlsEnabled =
                outlet.acceptsPowerCommands && !isSendingCommand

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        onPowerStateRequested(PowerState.ON)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = controlsEnabled &&
                        outlet.reportedStatus != DeviceStatus.ON,
                ) {
                    Text("Turn on")
                }

                OutlinedButton(
                    onClick = {
                        onPowerStateRequested(PowerState.OFF)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = controlsEnabled &&
                        outlet.reportedStatus != DeviceStatus.OFF,
                ) {
                    Text("Turn off")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// ── Reports Tab ──────────────────────────────────────────────────────────────

@Composable
private fun ReportSection(
    reportAlerts: List<HomeAlert>,
    devices: List<SmartDevice>,
    isLoading: Boolean,
) {
    if (isLoading) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading activity data…",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    // Count alerts per device
    val countByDevice: Map<String, Int> = reportAlerts
        .groupBy { it.deviceId }
        .mapValues { it.value.size }

    // Most recent alert timestamp per device
    val lastSeenByDevice: Map<String, Long> = reportAlerts
        .groupBy { it.deviceId }
        .mapValues { entry -> entry.value.maxOf { it.createdAtMillis } }

    val devicesWithActivity = countByDevice.size
    val totalEvents = reportAlerts.size

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Activity Log",
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        text = "Chronological history of your home.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (reportAlerts.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    SmartHomeIcons.Report,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "No standard activity recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Toggle a device to generate events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    } else {
        reportAlerts.sortedByDescending { it.createdAtMillis }.forEach { alert ->
            AlertCard(
                alert = alert,
                deviceName = devices.firstOrNull { it.id == alert.deviceId }?.name,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReportStatTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun DeviceActivityRow(
    name: String,
    profile: DeviceProfile?,
    eventCount: Int,
    maxCount: Int,
    lastSeenMillis: Long?,
) {
    val glyph = when (profile) {
        DeviceProfile.OUTLET -> "◉"
        DeviceProfile.MULTI_SWITCH -> "≡"
        DeviceProfile.SAFETY_OUTLET -> "⚠"
        DeviceProfile.LIGHT -> "✦"
        DeviceProfile.CAMERA -> "▣"
        null -> "?"
    }
    val barFraction = (eventCount.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val lastSeenText = lastSeenMillis?.let {
        val diff = System.currentTimeMillis() - it
        when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000L}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
            else -> "${diff / 86_400_000L}d ago"
        }
    } ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = glyph,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = profile?.displayName ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$eventCount event${if (eventCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = lastSeenText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Activity bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(3.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .height(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun SafetySection(
    reportAlerts: List<com.smarthome.app.domain.model.HomeAlert>,
    devices: List<SmartDevice>,
    isLoading: Boolean,
) {
    if (isLoading) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Safety Overview", style = MaterialTheme.typography.titleLarge)
        Text("Recent critical cutoffs and offline hardware alerts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        
        Spacer(modifier = Modifier.height(16.dp))

        if (reportAlerts.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(SmartHomeIcons.Safety, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No safety issues reported recently.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            reportAlerts.sortedByDescending { it.createdAtMillis }.forEach { alert ->
                AlertCard(
                    alert = alert,
                    deviceName = devices.firstOrNull { it.id == alert.deviceId }?.name,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
