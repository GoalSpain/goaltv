package com.goaltv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goaltv.data.repository.DataRepository
import com.goaltv.data.repository.DataSourceMode
import com.goaltv.model.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val FIXED_SERVER_URL = "http://zeta.proxy-pass.xyz:80"

data class AuthUiState(
    val serverUrl: String = FIXED_SERVER_URL,
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val authResponse: AuthResponse? = null,
    val activeMode: DataSourceMode = DataSourceMode.XTREAM
)

class AuthViewModel(
    private val repository: DataRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val isDohEnabled: StateFlow<Boolean> = repository.isDohEnabled

    fun setDohEnabled(enabled: Boolean) {
        repository.setDohEnabled(enabled)
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(activeMode = mode)
            }
        }
        viewModelScope.launch {
            repository.activeXtreamAccount.collect { acc ->
                if (acc != null) {
                    _uiState.value = _uiState.value.copy(
                        serverUrl = acc.serverUrl.ifBlank { FIXED_SERVER_URL },
                        username = acc.username,
                        password = acc.password,
                        authResponse = acc.authResponse
                    )
                }
            }
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, errorMessage = null)
    }

    fun onUsernameChange(user: String) {
        _uiState.value = _uiState.value.copy(username = user, errorMessage = null)
    }

    fun onPasswordChange(pass: String) {
        _uiState.value = _uiState.value.copy(password = pass, errorMessage = null)
    }

    fun connectXtream(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Por favor ingrese Nombre de Usuario y Contraseña")
            return
        }

        viewModelScope.launch {
            val timerJob = launch {
                kotlinx.coroutines.delay(1200L)
                if (_uiState.value.isLoading) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Buscando el mejor servidor para tu conexión..."
                    )
                }
            }

            try {
                _uiState.value = state.copy(
                    isLoading = true,
                    statusMessage = "Buscando el mejor servidor para tu conexión...",
                    errorMessage = null,
                    successMessage = null
                )
                val targetUrl = state.serverUrl.ifBlank { FIXED_SERVER_URL }
                val result = repository.connectXtreamServer(targetUrl, state.username, state.password)
                timerJob.cancel()

                result.onSuccess { auth ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = null,
                        authResponse = auth,
                        successMessage = "¡Conectado exitosamente al Servidor!",
                        errorMessage = null
                    )
                    onSuccess()
                }.onFailure { err ->
                    val rawMsg = err.message ?: ""
                    val displayMsg = if (rawMsg.contains("Empty response body", ignoreCase = true)) {
                        "Fallo de conexion con el servidor"
                    } else if (rawMsg.isNotBlank()) {
                        rawMsg
                    } else {
                        "Fallo de conexion con el servidor"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = null,
                        errorMessage = displayMsg
                    )
                }
            } catch (_: Exception) {
                timerJob.cancel()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = null,
                    errorMessage = "Fallo de conexion con el servidor"
                )
            }
        }
    }

    fun connectWithCredentials(
        serverUrl: String,
        user: String,
        pass: String,
        onSuccess: () -> Unit = {}
    ) {
        val targetUrl = serverUrl.ifBlank { FIXED_SERVER_URL }
        _uiState.value = _uiState.value.copy(
            serverUrl = targetUrl,
            username = user,
            password = pass
        )
        connectXtream(onSuccess)
    }

    fun loginWithDeviceAuth(context: android.content.Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusMessage = "Cargando datos configurados en la web...",
                errorMessage = null,
                successMessage = null
            )
            try {
                val authRes = com.goaltv.data.network.DeviceAuthManager.verifyDevice(context)
                val sUrl = authRes.serverUrl?.ifBlank { null } ?: FIXED_SERVER_URL
                val user = authRes.username?.ifBlank { null } ?: _uiState.value.username
                val pass = authRes.password?.ifBlank { null } ?: _uiState.value.password

                if (user.isNotBlank() && pass.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(
                        serverUrl = sUrl,
                        username = user,
                        password = pass
                    )
                    val result = repository.connectXtreamServer(sUrl, user, pass)
                    result.onSuccess { auth ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            statusMessage = null,
                            authResponse = auth,
                            successMessage = "¡Configuración web aplicada exitosamente!",
                            errorMessage = null
                        )
                        onSuccess()
                    }.onFailure { err ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            statusMessage = null,
                            errorMessage = err.message ?: "Fallo de conexión con el servidor"
                        )
                    }
                } else {
                    repository.refreshData()
                    val acc = repository.activeXtreamAccount.value
                    if (acc != null && acc.username.isNotBlank()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            statusMessage = null,
                            serverUrl = acc.serverUrl,
                            username = acc.username,
                            password = acc.password,
                            authResponse = acc.authResponse
                        )
                        onSuccess()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            statusMessage = null,
                            errorMessage = "No se encontraron credenciales válidas en el servidor web."
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = null,
                    errorMessage = "Error al conectar con la configuración web"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.clearSession()
                _uiState.value = _uiState.value.copy(
                    username = "",
                    password = "",
                    authResponse = null,
                    successMessage = "Sesión cerrada."
                )
            } catch (_: Exception) {}
        }
    }

    fun importBackup(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val success = repository.importLocalBackup()
            if (success) {
                val acc = repository.activeXtreamAccount.value
                val user = acc?.username ?: ""
                val pass = acc?.password ?: ""
                if (user.isNotBlank() && pass.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(
                        username = user,
                        password = pass,
                        isLoading = false
                    )
                    connectXtream(onSuccess)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Ajustes importados correctamente."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se encontró copia local para restaurar."
                )
            }
        }
    }
}
