package com.smarthome.app.ui.outlet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.FirebaseOutletRepository
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.repository.OutletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OutletUiState(
    val email: String = "",
    val password: String = "",
    val isAuthenticated: Boolean = false,
    val isSigningIn: Boolean = false,
    val isLoadingOutlet: Boolean = false,
    val isSendingCommand: Boolean = false,
    val outlet: OutletDevice? = null,
    val errorMessage: String? = null,
)

class OutletViewModel(
    private val repository: OutletRepository = FirebaseOutletRepository(),
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(
        OutletUiState(
            isAuthenticated = repository.hasAuthenticatedUser,
        ),
    )

    val uiState: StateFlow<OutletUiState> = mutableUiState.asStateFlow()

    private var outletObservation: Job? = null

    init {
        if (repository.hasAuthenticatedUser) {
            observeOutlet()
        }
    }

    fun updateEmail(email: String) {
        mutableUiState.update { state ->
            state.copy(
                email = email,
                errorMessage = null,
            )
        }
    }

    fun updatePassword(password: String) {
        mutableUiState.update { state ->
            state.copy(
                password = password,
                errorMessage = null,
            )
        }
    }

    fun signIn() {
        val state = mutableUiState.value
        val email = state.email.trim()
        val password = state.password

        if (email.isBlank() || password.isBlank()) {
            mutableUiState.update {
                it.copy(errorMessage = "Enter both email and password.")
            }
            return
        }

        if (state.isSigningIn) return

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isSigningIn = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.signIn(email, password)
            }.onSuccess {
                mutableUiState.update {
                    it.copy(
                        password = "",
                        isAuthenticated = true,
                        isSigningIn = false,
                    )
                }
                observeOutlet()
            }.onFailure {
                mutableUiState.update {
                    it.copy(
                        isSigningIn = false,
                        errorMessage = "Sign-in failed. Check your credentials and connection.",
                    )
                }
            }
        }
    }

    fun requestPowerState(powerState: PowerState) {
        val state = mutableUiState.value

        if (state.isSendingCommand ||
            state.outlet?.acceptsPowerCommands != true
        ) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isSendingCommand = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.requestPowerState(
                    homeId = HOME_ID,
                    deviceId = OUTLET_ID,
                    powerState = powerState,
                )
            }.onSuccess {
                mutableUiState.update {
                    it.copy(isSendingCommand = false)
                }
            }.onFailure {
                mutableUiState.update {
                    it.copy(
                        isSendingCommand = false,
                        errorMessage = "The outlet command could not be sent.",
                    )
                }
            }
        }
    }

    fun signOut() {
        outletObservation?.cancel()
        outletObservation = null
        repository.signOut()

        mutableUiState.value = OutletUiState(
            email = mutableUiState.value.email,
        )
    }

    private fun observeOutlet() {
        outletObservation?.cancel()

        mutableUiState.update {
            it.copy(
                isLoadingOutlet = true,
                errorMessage = null,
            )
        }

        outletObservation = viewModelScope.launch {
            repository
                .observeOutlet(
                    homeId = HOME_ID,
                    deviceId = OUTLET_ID,
                )
                .catch {
                    mutableUiState.update { state ->
                        state.copy(
                            isLoadingOutlet = false,
                            errorMessage = "The outlet could not be loaded.",
                        )
                    }
                }
                .collect { outlet ->
                    mutableUiState.update { state ->
                        state.copy(
                            isLoadingOutlet = false,
                            outlet = outlet,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private companion object {
        const val HOME_ID = "demo-home"
        const val OUTLET_ID = "main-outlet"
    }
}