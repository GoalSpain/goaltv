package com.goaltv.ui.screens

import androidx.compose.foundation.background
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.goaltv.data.network.DeviceAuthManager
import com.goaltv.data.network.DeviceAuthResult
import com.goaltv.ui.components.QrCodeView
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goaltv.data.repository.DataSourceMode
import com.goaltv.ui.components.GoalTvLogo
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusError
import com.goaltv.ui.theme.StatusSuccess
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.viewmodel.AuthViewModel

import androidx.activity.compose.BackHandler

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isConnected = isLoggedIn || (uiState.activeMode == DataSourceMode.XTREAM && uiState.authResponse?.userInfo != null)
    var showQrDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // En la pantalla de cuenta conectada, al darle al botón atrás, debe volver al panel principal
    BackHandler(enabled = isConnected) {
        onLoginSuccess()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo
            GoalTvLogo(sizeScale = 1.2f, showGlow = true)



            Spacer(modifier = Modifier.height(18.dp))

            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(0.82f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Account Info Card if authenticated or logged in
                    if (isConnected) {
                        val userInfo = uiState.authResponse?.userInfo
                        val displayUsername = userInfo?.username?.ifBlank { uiState.username } ?: uiState.username.ifBlank { "Usuario Activo" }
                        val displayStatus = userInfo?.status ?: "Activo"
                        val displayExpDate = formatLoginExpDate(userInfo?.expDate)
                        val displayConns = "${userInfo?.activeCons ?: "1"} / ${userInfo?.maxConnections ?: "1"}"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Conectado",
                                        tint = StatusSuccess
                                    )
                                    Text(
                                        text = "Cuenta Conectada",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                AccountDetailRow("Nombre de Usuario", displayUsername)
                                AccountDetailRow("Estado de Cuenta", displayStatus)
                                AccountDetailRow("Caducidad", displayExpDate)
                                AccountDetailRow("Conexiones Activas", displayConns)

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = onLoginSuccess,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                    ) {
                                        Text("ABRIR PANEL PRINCIPAL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.logout() },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PowerSettingsNew,
                                            contentDescription = "Desconectarse",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Form Title Header Row with QR Button on Top Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Iniciar Sesión",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // QR Button on Top Right
                            Button(
                                onClick = { showQrDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232538)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode,
                                        contentDescription = "Código QR",
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Iniciar con QR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Username Field
                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = { viewModel.onUsernameChange(it) },
                            label = { Text("Nombre de Usuario") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = "Nombre de Usuario", tint = OrangePrimary)
                            },
                            colors = customTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { viewModel.onPasswordChange(it) },
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Lock, contentDescription = "Contraseña", tint = OrangePrimary)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = customTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.connectXtream(onLoginSuccess) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            if (uiState.isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = uiState.statusMessage ?: "Buscando el mejor servidor para tu conexión...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = "CONECTAR AL SERVIDOR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Status, Error or Success Messages
                        uiState.errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = msg,
                                color = StatusError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        uiState.successMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = msg,
                                color = StatusSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        val deviceId = remember { DeviceAuthManager.getDeviceId(context) }
        var qrUrl by remember { mutableStateOf("http://84.235.239.152/admin/configurar.php?id=$deviceId") }

        LaunchedEffect(Unit) {
            val generatedUrl = DeviceAuthManager.getQrUrl(context)
            qrUrl = generatedUrl
        }

        QrCodeLoginDialog(
            qrUrl = qrUrl,
            deviceId = deviceId,
            onDismiss = { showQrDialog = false },
            onDataReceived = { authRes ->
                showQrDialog = false
                val sUrl = authRes.serverUrl ?: com.goaltv.ui.viewmodel.FIXED_SERVER_URL
                val user = authRes.username.orEmpty()
                val pass = authRes.password.orEmpty()
                viewModel.connectWithCredentials(sUrl, user, pass, onSuccess = onLoginSuccess)
            }
        )
    }
}

@Composable
fun QrCodeLoginDialog(
    qrUrl: String,
    deviceId: String,
    onDismiss: () -> Unit,
    onDataReceived: (DeviceAuthResult) -> Unit
) {
    val context = LocalContext.current

    // Escuchar activamente los datos recibidos desde http://84.235.239.152/admin/configurar.php
    LaunchedEffect(Unit) {
        while (isActive) {
            val authRes = withContext(Dispatchers.IO) {
                DeviceAuthManager.verifyDevice(context)
            }
            if (!authRes.username.isNullOrBlank() && !authRes.password.isNullOrBlank()) {
                onDataReceived(authRes)
                break
            }
            delay(1500L)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = "Código QR",
                    tint = OrangePrimary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Iniciar Sesión con Código QR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Escanea este código QR con la cámara de tu móvil para configurar la cuenta desde la web:",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                QrCodeView(
                    content = qrUrl,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFF222232),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "ID Dispositivo: $deviceId",
                        color = OrangePrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = OrangePrimary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Esperando recibir los datos, esta ventana se cerrará automáticamente",
                        color = OrangePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CERRAR", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = null
    )
}

@Composable
fun AccountDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangePrimary,
    unfocusedBorderColor = Color(0xFF33334A),
    focusedLabelColor = OrangePrimary,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = OrangePrimary,
    focusedContainerColor = SurfaceDarkCard,
    unfocusedContainerColor = SurfaceDarkCard
)

private fun formatLoginExpDate(expDate: String?): String {
    return try {
        if (!expDate.isNullOrBlank() && expDate != "0" && !expDate.equals("null", ignoreCase = true)) {
            val ts = expDate.toLongOrNull()
            if (ts != null && ts > 0) {
                val millis = if (ts < 100000000000L) ts * 1000L else ts
                val date = Date(millis)
                return SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES")).format(date)
            }
        }
        val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES")).format(oneYearAhead)
    } catch (_: Exception) {
        val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES")).format(oneYearAhead)
    }
}
