package com.smarthome.app.ui.outlet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState

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
        onFloorSelected = viewModel::selectFloor,
        onFloorCreated = viewModel::createFloor,
        onRoomCreated = viewModel::createRoom,
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
    onFloorSelected: (String) -> Unit,
    onFloorCreated: (String, Int, Int, Int) -> Unit,
    onRoomCreated: (String, Int, Int, Int, Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.isAuthenticated) {
            OutletDashboard(
                state = state,
                onSignOut = onSignOut,
                onPowerStateRequested = onPowerStateRequested,
                onFloorSelected = onFloorSelected,
                onFloorCreated = onFloorCreated,
                onRoomCreated = onRoomCreated,
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
    onFloorSelected: (String) -> Unit,
    onFloorCreated: (String, Int, Int, Int) -> Unit,
    onRoomCreated: (String, Int, Int, Int, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Primary home",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Ground floor",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            TextButton(onClick = onSignOut) {
                Text("Sign out")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FloorDashboardSection(
            state = state,
            onFloorSelected = onFloorSelected,
            onFloorCreated = onFloorCreated,
            onRoomCreated = onRoomCreated,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Devices",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoadingOutlet -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading outlet…")
            }

            state.outlet != null -> {
                OutletCard(
                    outlet = state.outlet,
                    isSendingCommand = state.isSendingCommand,
                    onPowerStateRequested = onPowerStateRequested,
                )
            }

            else -> {
                Text("No outlet is available.")
            }
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
