package com.goaltv

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.goaltv.data.network.DeviceAuthManager
import com.goaltv.ui.GoalTVApp

data class DeepLinkAuthData(
    val status: String = "",
    val username: String = "",
    val password: String = "",
    val serverUrl: String = "",
    val targetDeviceId: String = ""
)

class MainActivity : ComponentActivity() {

    private val deepLinkData = mutableStateOf<DeepLinkAuthData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (_: Exception) {}
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            GoalTVApp(
                modifier = Modifier.fillMaxSize(),
                deepLinkAuthData = deepLinkData.value,
                onDeepLinkHandled = { deepLinkData.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        val scheme = data?.scheme
        if (data != null && (scheme == "goaltv" || scheme == "goaltv-tv") && data.host == "configuracion-guardada") {
            val targetDeviceId = data.getQueryParameter("id")
                ?: data.getQueryParameter("device_id")
                ?: ""
            val myDeviceId = DeviceAuthManager.getDeviceId(this)

            val status = data.getQueryParameter("status") ?: "ok"
            val usuario = data.getQueryParameter("usuario")
                ?: data.getQueryParameter("user")
                ?: data.getQueryParameter("username")
                ?: ""
            val password = data.getQueryParameter("password")
                ?: data.getQueryParameter("pass")
                ?: ""
            val servidor = data.getQueryParameter("servidor")
                ?: data.getQueryParameter("server_url")
                ?: data.getQueryParameter("server")
                ?: ""

            if (targetDeviceId.isNotBlank() && !targetDeviceId.equals(myDeviceId, ignoreCase = true)) {
                // El deep link es para otro dispositivo (p. ej. la TV)
                deepLinkData.value = DeepLinkAuthData(
                    status = "other_device",
                    targetDeviceId = targetDeviceId
                )
            } else {
                deepLinkData.value = DeepLinkAuthData(
                    status = status,
                    username = usuario,
                    password = password,
                    serverUrl = servidor,
                    targetDeviceId = targetDeviceId
                )
            }
        }
    }
}