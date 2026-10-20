package com.goaltv.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goaltv.data.network.DeviceAuthResult
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusError
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.TextMuted

@Composable
fun BlockedDeviceDialog(
    onExitClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = "Bloqueado",
                    tint = StatusError,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Acceso denegado",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Text(
                text = "Este dispositivo ha sido bloqueado.",
                color = TextMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onExitClick,
                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SALIR", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = null
    )
}

@Composable
fun FreeDeviceDialog(
    authResult: DeviceAuthResult,
    onReverify: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Versión Gratuita",
                    tint = OrangePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Versión Gratuita",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Tu dispositivo no es Premium todavía.\n\nTu ID de activación es:",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF222232),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authResult.deviceId,
                        color = OrangePrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("ID Dispositivo", authResult.deviceId)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        onReverify()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A40)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("COPIAR ID", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = onReverify,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("YA ME HAN ACTIVADO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        },
        dismissButton = null
    )
}