package com.goaltv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goaltv.data.network.UpdateInfo
import com.goaltv.data.network.UpdateManager
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    val actualizarFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isDownloading) {
        if (!isDownloading) {
            try {
                actualizarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "¡Nueva Versión ${updateInfo.versionName}!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Novedades y cambios:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = updateInfo.changelog,
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Descargando actualización... (${(downloadProgress * 100).toInt()}%)",
                        color = OrangePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = OrangePrimary,
                        trackColor = Color(0xFF2A2A3E)
                    )
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ACTUALIZAR button on the LEFT side with default focus
                    Button(
                        onClick = {
                            isDownloading = true
                            scope.launch {
                                UpdateManager.downloadAndInstallApk(
                                    context = context,
                                    apkUrl = updateInfo.apkUrl,
                                    onProgress = { downloadProgress = it }
                                )
                                isDownloading = false
                                onDismiss()
                            }
                        },
                        modifier = Modifier.focusRequester(actualizarFocusRequester),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ACTUALIZAR", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // CANCELAR button on the RIGHT side
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A40)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("CANCELAR", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(20.dp))
                    Text("Descargando...", color = TextMuted, fontSize = 12.sp)
                }
            }
        },
        dismissButton = null
    )
}