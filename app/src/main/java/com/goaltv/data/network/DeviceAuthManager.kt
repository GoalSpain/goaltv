package com.goaltv.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface

data class DeviceAuthResult(
    val isBlocked: Boolean = false,
    val deviceId: String = "",
    val deviceIp: String = "",
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null
)

object DeviceAuthManager {

    private const val API_URL = "http://84.235.239.152/admin/api.php"
    private const val CONFIGURAR_URL = "http://84.235.239.152/admin/configurar.php"

    @Volatile
    private var cachedIp: String? = null

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return try {
            val rawId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            val cleanHex = rawId.replace("-", "").trim()
            if (cleanHex.length >= 6) {
                cleanHex.takeLast(6).uppercase()
            } else {
                ("G" + Math.abs(cleanHex.hashCode()).toString(16)).take(6).uppercase()
            }
        } catch (_: Exception) {
            "G12345"
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotBlank() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return ""
    }

    suspend fun fetchDeviceIp(client: OkHttpClient): String = withContext(Dispatchers.IO) {
        cachedIp?.let { if (it.isNotBlank()) return@withContext it }
        try {
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .header("User-Agent", "GoalTV-DeviceAuth")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val ip = resp.body.string().trim()
                    if (ip.isNotBlank()) {
                        cachedIp = ip
                        return@withContext ip
                    }
                }
            }
        } catch (_: Exception) {}

        val localIp = getLocalIpAddress()
        if (localIp.isNotBlank()) {
            cachedIp = localIp
            return@withContext localIp
        }
        ""
    }

    suspend fun getQrUrl(context: Context): String = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId(context)
        val client = NetworkModule.getOkHttpClient()
        val ip = fetchDeviceIp(client)

        // Registrar en BD que el dispositivo existe
        registerDeviceAndIpInDb(client, deviceId, ip)

        "$CONFIGURAR_URL?id=$deviceId"
    }

    private fun registerDeviceAndIpInDb(client: OkHttpClient, deviceId: String, ip: String): DeviceAuthResult? {
        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val osVersion = Build.VERSION.RELEASE

        try {
            val formBody = FormBody.Builder()
                .add("device_id", deviceId)
                .add("id", deviceId)
                .add("ip", ip)
                .add("tipo_dispositivo", "tv")
                .add("modelo", model)
                .add("os", osVersion)
                .add("nombre_app", "goal_tv")
                .build()

            val request = Request.Builder()
                .url("$API_URL?device_id=$deviceId&id=$deviceId&tipo_dispositivo=tv&ip=$ip&t=${System.currentTimeMillis()}")
                .post(formBody)
                .header("User-Agent", "GoalTV-DeviceAuth")
                .header("Accept", "application/json, text/html, text/plain")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body.string()
                    val res = parseCredentialsFromResponse(bodyStr, deviceId, ip)
                    if (res != null && !res.username.isNullOrBlank() && !res.password.isNullOrBlank()) {
                        return res
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun parseCredentialsFromResponse(bodyStr: String, deviceId: String, ip: String): DeviceAuthResult? {
        val trimmed = bodyStr.trim()
        if (trimmed.isBlank()) return null

        var isBlocked = false
        var serverUrl = ""
        var username = ""
        var password = ""

        fun inspectObject(json: JSONObject) {
            if (json.optBoolean("bloqueado", false) || json.optBoolean("is_blocked", false) || json.optBoolean("blocked", false)) {
                isBlocked = true
            }

            val u = json.optString("usuario",
                    json.optString("username",
                    json.optString("user",
                    json.optString("usr",
                    json.optString("login",
                    json.optString("account", ""))))))
            if (u.isNotBlank() && !u.equals("null", ignoreCase = true)) username = u

            val p = json.optString("password",
                    json.optString("clave",
                    json.optString("pass",
                    json.optString("pwd",
                    json.optString("contrasena",
                    json.optString("contraseña", ""))))))
            if (p.isNotBlank() && !p.equals("null", ignoreCase = true)) password = p

            val s = json.optString("server_url",
                    json.optString("servidor",
                    json.optString("url",
                    json.optString("server",
                    json.optString("host",
                    json.optString("dns", ""))))))
            if (s.isNotBlank() && !s.equals("null", ignoreCase = true)) serverUrl = s

            // Inspect nested objects if username or password not found yet
            if (username.isBlank() || password.isBlank()) {
                val nestedKeys = listOf("data", "response", "result", "datos", "cuenta", "credentials", "info", "user")
                for (key in nestedKeys) {
                    val child = json.optJSONObject(key)
                    if (child != null) {
                        inspectObject(child)
                        if (username.isNotBlank() && password.isNotBlank()) break
                    }
                }
            }
        }

        try {
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                inspectObject(json)
            } else if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                if (arr.length() > 0) {
                    val firstObj = arr.optJSONObject(0)
                    if (firstObj != null) inspectObject(firstObj)
                }
            } else if (trimmed.contains("<input", ignoreCase = true) || trimmed.contains("usuario", ignoreCase = true)) {
                fun extractInput(name: String): String {
                    val regex = Regex("""name=["']?$name["']?\s+[^>]*value=["']?([^"'>\s]+)["']?""", RegexOption.IGNORE_CASE)
                    val m = regex.find(trimmed)
                    if (m != null) return m.groupValues[1]

                    val regexAlt = Regex("""value=["']?([^"'>\s]+)["']?\s+[^>]*name=["']?$name["']?""", RegexOption.IGNORE_CASE)
                    val mAlt = regexAlt.find(trimmed)
                    if (mAlt != null) return mAlt.groupValues[1]

                    return ""
                }

                val u = extractInput("usuario").ifBlank { extractInput("username").ifBlank { extractInput("user") } }
                val p = extractInput("password").ifBlank { extractInput("clave").ifBlank { extractInput("pass") } }
                val s = extractInput("server_url").ifBlank { extractInput("servidor").ifBlank { extractInput("url") } }

                if (u.isNotBlank() && p.isNotBlank()) {
                    username = u
                    password = p
                    serverUrl = s
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (username.isNotBlank() && password.isNotBlank()) {
            return DeviceAuthResult(
                isBlocked = isBlocked,
                deviceId = deviceId,
                deviceIp = ip,
                serverUrl = serverUrl.ifBlank { null },
                username = username,
                password = password
            )
        } else if (isBlocked) {
            return DeviceAuthResult(isBlocked = true, deviceId = deviceId, deviceIp = ip)
        }

        return null
    }

    suspend fun verifyDevice(context: Context): DeviceAuthResult = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId(context)
        val client = NetworkModule.getOkHttpClient()
        val ip = fetchDeviceIp(client)

        // Registrar en BD y comprobar si ya se recibieron las credenciales temporales
        val regResult = registerDeviceAndIpInDb(client, deviceId, ip)
        if (regResult != null && !regResult.username.isNullOrBlank() && !regResult.password.isNullOrBlank()) {
            return@withContext regResult
        }

        val urlsToTry = listOf(
            "$API_URL?device_id=$deviceId&id=$deviceId&tipo_dispositivo=tv&ip=$ip&t=${System.currentTimeMillis()}",
            "$CONFIGURAR_URL?device_id=$deviceId&id=$deviceId&tipo_dispositivo=tv&ip=$ip&json=1&t=${System.currentTimeMillis()}"
        )

        for (url in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GoalTV-DeviceAuth")
                    .header("Accept", "application/json, text/html, text/plain")
                    .header("Cache-Control", "no-cache, no-store")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body.string()
                        val res = parseCredentialsFromResponse(bodyStr, deviceId, ip)
                        if (res != null && !res.username.isNullOrBlank() && !res.password.isNullOrBlank()) {
                            return@withContext res
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        DeviceAuthResult(isBlocked = false, deviceId = deviceId, deviceIp = ip)
    }
}