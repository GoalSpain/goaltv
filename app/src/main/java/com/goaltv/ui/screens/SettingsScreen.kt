package com.goaltv.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import com.goaltv.ui.components.FocusableCard
import com.goaltv.ui.components.GoalTvLogo
import com.goaltv.ui.theme.OrangeGlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.goaltv.data.network.UpdateInfo
import com.goaltv.data.network.UpdateManager
import com.goaltv.ui.components.UpdateDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusError
import com.goaltv.ui.theme.StatusSuccess
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.theme.TextSecondary
import com.goaltv.ui.viewmodel.AppScreen
import com.goaltv.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    var selectedDecoder by remember { mutableStateOf("Hardware (HW)") }
    var selectedAspectRatio by remember { mutableStateOf("Ajustar a Pantalla") }
    var selectedBuffer by remember { mutableStateOf("Mediano (3s)") }

    var categories by remember { mutableStateOf<List<com.goaltv.model.Category>>(emptyList()) }
    var hiddenCategoryIds by remember { mutableStateOf(viewModel.repository.getHiddenCategoryIds()) }

    LaunchedEffect(Unit) {
        try {
            categories = viewModel.repository.getAllLiveCategories()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Ajustes",
                    tint = OrangePrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ajustes de Reproductor y Servidor",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            GoalTvLogo(sizeScale = 0.75f, showGlow = false)
        }

        // Section 1: Player Decoder Settings
        SettingsCard(
            title = "MODO DE DECODIFICADOR",
            icon = Icons.Rounded.Memory
        ) {
            val decoders = listOf("Hardware (HW)", "Software (SW)", "Automático")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                decoders.forEach { mode ->
                    val isSelected = selectedDecoder == mode
                    FocusableCard(
                        onClick = { selectedDecoder = mode },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 2: Aspect Ratio Preference
        SettingsCard(
            title = "RELACIÓN DE ASPECTO",
            icon = Icons.Rounded.AspectRatio
        ) {
            val aspectRatios = listOf("Ajustar a Pantalla", "16:9", "Zoom", "Estirar")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                aspectRatios.forEach { ratio ->
                    val isSelected = selectedAspectRatio == ratio
                    FocusableCard(
                        onClick = { selectedAspectRatio = ratio },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3: Buffer Settings
        SettingsCard(
            title = "TAMAÑO DE BÚFER",
            icon = Icons.Rounded.Speed
        ) {
            val buffers = listOf("Pequeño (1s)", "Mediano (3s)", "Grande (5s)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buffers.forEach { buf ->
                    val isSelected = selectedBuffer == buf
                    FocusableCard(
                        onClick = { selectedBuffer = buf },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buf,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 5: Gestión y Orden de Categorías
        var showCategoryDialog by remember { mutableStateOf(false) }

        if (showCategoryDialog) {
            CategoryManagementDialog(
                categories = categories,
                hiddenCategoryIds = hiddenCategoryIds,
                onToggleHidden = { catId ->
                    viewModel.repository.toggleCategoryHidden(catId)
                    hiddenCategoryIds = viewModel.repository.getHiddenCategoryIds()
                },
                onReorder = { newOrderList ->
                    categories = newOrderList
                    viewModel.repository.saveCategoryOrder(newOrderList.map { it.id })
                },
                onDismiss = { showCategoryDialog = false }
            )
        }

        if (categories.isNotEmpty()) {
            SettingsCard(
                title = "GESTIÓN Y ORDEN DE CATEGORÍAS DE TV",
                icon = Icons.Rounded.VisibilityOff
            ) {
                Column {
                    Text(
                        text = "Ordena las categorías de canales a tu gusto y oculta las que no te interesen.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FocusableCard(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("GESTIONAR Y ORDENAR CATEGORÍAS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 6: Backup Local con Buscador de Archivos
        var backupMessage by remember { mutableStateOf<String?>(null) }
        val backupContext = LocalContext.current
        val backupScope = rememberCoroutineScope()

        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                backupScope.launch {
                    val success = viewModel.importBackupFromUri(backupContext, uri)
                    backupMessage = if (success) "Ajustes y favoritos restaurados con éxito" else "Error al restaurar copia local"
                }
            }
        }

        SettingsCard(
            title = "COPIA DE SEGURIDAD Y RESTAURACIÓN LOCAL",
            icon = Icons.Rounded.Save
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Guarda tu selección de favoritos y el orden de canales para restaurarlo facilmente cuando quieras",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusableCard(
                        onClick = {
                            val success = viewModel.repository.exportLocalBackup()
                            backupMessage = if (success) {
                                "Copia de seguridad guardada con éxito en la carpeta Descargas (goaltv_backup.json)"
                            } else {
                                "Error al guardar copia local de ajustes"
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("EXPORTAR AJUSTES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }

                    FocusableCard(
                        onClick = {
                            val success = viewModel.repository.importLocalBackup()
                            if (success) {
                                backupMessage = "Ajustes y favoritos restaurados con éxito desde Descargas (goaltv_backup.json)"
                            } else {
                                try {
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                } catch (_: Exception) {
                                    backupMessage = "No se encontró el archivo goaltv_backup.json en Descargas"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("RESTAURAR AJUSTES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                backupMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = StatusSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 7: Software Update
        var isCheckingUpdate by remember { mutableStateOf(false) }
        var updateStatusMessage by remember { mutableStateOf<String?>(null) }
        var manualUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        val updateContext = LocalContext.current
        val updateScope = rememberCoroutineScope()

        val appVersionName = remember {
            try {
                val pInfo = updateContext.packageManager.getPackageInfo(updateContext.packageName, 0)
                pInfo.versionName ?: "1.0"
            } catch (_: Exception) {
                "1.0"
            }
        }

        var showCurrentVersionDialog by remember { mutableStateOf(false) }

        if (showCurrentVersionDialog) {
            com.goaltv.ui.components.CurrentVersionDialog(
                versionName = appVersionName,
                changelog = "Novedades en GoalTV v$appVersionName:\n\n⚽ Programación Deportiva Corregida y Mejorada: Integración de la guía con vista de 3 días.\n\n🧼 Mejorada Limpieza de Nombres de Canales\n\n🎯 Mejora en la guía EPG propia de los canales",
                onDismiss = { showCurrentVersionDialog = false }
            )
        }

        if (manualUpdateInfo != null) {
            UpdateDialog(
                updateInfo = manualUpdateInfo!!,
                onDismiss = { manualUpdateInfo = null }
            )
        }

        SettingsCard(
            title = "ACTUALIZACIÓN DE SOFTWARE",
            icon = Icons.Rounded.SystemUpdate
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comprueba si existe una versión más reciente de GoalTV:",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2A2A40))
                            .clickable { showCurrentVersionDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Versión actual: v$appVersionName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeGlow
                        )
                    }
                }

                val currentDeviceId = remember { com.goaltv.data.network.DeviceAuthManager.getDeviceId(updateContext) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID de Dispositivo:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF222232))
                            .border(1.dp, Color(0xFF33334A), RoundedCornerShape(6.dp))
                            .clickable {
                                val clipboard = updateContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("ID Dispositivo", currentDeviceId)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(updateContext, "ID de Dispositivo copiado", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentDeviceId,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                    }
                }

                FocusableCard(
                    onClick = {
                        isCheckingUpdate = true
                        updateStatusMessage = null
                        updateScope.launch {
                            val info = UpdateManager.checkForUpdate(updateContext)
                            isCheckingUpdate = false
                            if (info.isUpdateAvailable) {
                                manualUpdateInfo = info
                            } else {
                                updateStatusMessage = "GoalTV está actualizado a la última versión."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enableScale = false,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("BUSCAR ACTUALIZACIÓN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }

                updateStatusMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = StatusSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 4: Server & Account Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "GESTIÓN DE SERVIDOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                FocusableCard(
                    onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enableScale = false,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("MI CUENTA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FocusableCard(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enableScale = false,
                    colors = CardDefaults.cardColors(containerColor = StatusError),
                    focusedBorderColor = Color(0xFFFFD700)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = "Cerrar Sesión",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DESCONECTARSE DE LA CUENTA",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = OrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun CategoryManagementDialog(
    categories: List<com.goaltv.model.Category>,
    hiddenCategoryIds: Set<String>,
    onToggleHidden: (String) -> Unit,
    onReorder: (List<com.goaltv.model.Category>) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(20.dp)),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GESTIÓN DE CATEGORÍAS",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ordena las categorías de canales a tu gusto y oculta las que no te interesen.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(categories, key = { _, cat -> cat.id }) { index, cat ->
                        val isHidden = hiddenCategoryIds.contains(cat.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDarkCard, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isHidden) TextMuted else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                // Move to Top Button (⏫)
                                FocusableCard(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = categories.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(0, item)
                                            onReorder(mutable)
                                        }
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (index > 0) Color(0xFF1E2235) else Color(0xFF121420))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("⏫", fontSize = 11.sp, color = if (index > 0) OrangePrimary else TextMuted)
                                    }
                                }

                                // Up Button (⬆)
                                FocusableCard(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = categories.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index - 1, item)
                                            onReorder(mutable)
                                        }
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (index > 0) Color(0xFF1E2235) else Color(0xFF121420))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("⬆", fontSize = 11.sp, color = if (index > 0) OrangePrimary else TextMuted)
                                    }
                                }

                                // Down Button (⬇)
                                FocusableCard(
                                    onClick = {
                                        if (index < categories.size - 1) {
                                            val mutable = categories.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index + 1, item)
                                            onReorder(mutable)
                                        }
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (index < categories.size - 1) Color(0xFF1E2235) else Color(0xFF121420))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("⬇", fontSize = 11.sp, color = if (index < categories.size - 1) OrangePrimary else TextMuted)
                                    }
                                }

                                // Move to Bottom Button (⏬)
                                FocusableCard(
                                    onClick = {
                                        if (index < categories.size - 1) {
                                            val mutable = categories.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(item)
                                            onReorder(mutable)
                                        }
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (index < categories.size - 1) Color(0xFF1E2235) else Color(0xFF121420))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("⏬", fontSize = 11.sp, color = if (index < categories.size - 1) OrangePrimary else TextMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Hide/Show Chip Button
                                FilterChip(
                                    selected = isHidden,
                                    onClick = { onToggleHidden(cat.id) },
                                    label = {
                                        Text(
                                            text = if (isHidden) "🙈 Oculta" else "👁️ Visible",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusError,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF1E2E24),
                                        labelColor = StatusSuccess
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("GUARDAR Y CERRAR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
