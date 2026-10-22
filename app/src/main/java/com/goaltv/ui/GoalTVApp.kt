package com.goaltv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.launch
import com.goaltv.data.network.DeviceAuthManager
import com.goaltv.data.network.DeviceAuthResult
import com.goaltv.ui.components.BlockedDeviceDialog
import com.goaltv.ui.components.FreeDeviceDialog
import com.goaltv.data.network.UpdateInfo
import com.goaltv.data.network.UpdateManager
import com.goaltv.ui.components.UpdateDialog
import com.goaltv.data.repository.DataRepositoryImpl
import com.goaltv.ui.screens.CatchUpScreen
import com.goaltv.ui.screens.DashboardScreen
import com.goaltv.ui.screens.LiveTvScreen
import com.goaltv.ui.screens.LoginScreen
import com.goaltv.ui.screens.MoviesScreen
import com.goaltv.ui.screens.SeriesScreen
import com.goaltv.ui.screens.SettingsScreen
import com.goaltv.ui.screens.SportsScreen
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.GoaltvTheme
import com.goaltv.ui.viewmodel.AppScreen
import com.goaltv.ui.viewmodel.AuthViewModel
import com.goaltv.ui.viewmodel.CatchUpViewModel
import com.goaltv.ui.viewmodel.LiveTvViewModel
import com.goaltv.ui.viewmodel.MainViewModel
import com.goaltv.ui.viewmodel.SportsViewModel
import com.goaltv.ui.viewmodel.VodViewModel

@Composable
fun GoalTVApp(
    modifier: Modifier = Modifier,
    deepLinkAuthData: com.goaltv.DeepLinkAuthData? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    GoaltvTheme {
        val context = LocalContext.current.applicationContext
        // Shared repository instance with persistent context
        val repository = remember { DataRepositoryImpl(context) }

        val mainViewModel: MainViewModel = viewModel { MainViewModel(repository) }
        val authViewModel: AuthViewModel = viewModel { AuthViewModel(repository) }
        val liveTvViewModel: LiveTvViewModel = viewModel { LiveTvViewModel(repository) }
        val vodViewModel: VodViewModel = viewModel { VodViewModel(repository) }
        val sportsViewModel: SportsViewModel = viewModel { SportsViewModel(repository) }
        val catchUpViewModel: CatchUpViewModel = viewModel { CatchUpViewModel(repository) }

        val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
        val currentScreen by mainViewModel.currentScreen.collectAsState()
        val activePlaybackTarget by mainViewModel.activePlaybackTarget.collectAsState()

        // Check for App Updates & Device Verification on Startup
        val activityContext = LocalContext.current
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
        var pendingUpdateInfo by remember { androidx.compose.runtime.mutableStateOf<UpdateInfo?>(null) }
        var deviceAuthResult by remember { androidx.compose.runtime.mutableStateOf<DeviceAuthResult?>(null) }

        LaunchedEffect(Unit) {
            // Ejecutar en paralelo para evitar que un bloqueo de red en Github afecte el envío a la BD
            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val updateInfo = UpdateManager.checkForUpdate(activityContext)
                    if (updateInfo.isUpdateAvailable) {
                        pendingUpdateInfo = updateInfo
                    }
                } catch (_: Exception) {}
            }

            launch(kotlinx.coroutines.Dispatchers.IO) {
                // Reintentar envío a BD si la TV arrancó antes de tener conexión a Internet
                var retryCount = 0
                while (retryCount < 5) {
                    try {
                        val authRes = DeviceAuthManager.verifyDevice(activityContext)
                        if (authRes.deviceIp.isNotBlank() || authRes.username.isNullOrBlank() == false) {
                            deviceAuthResult = authRes
                            break
                        }
                    } catch (_: Exception) {}
                    retryCount++
                    kotlinx.coroutines.delay(5000)
                }
            }
        }

        // Handle deep link web configuration redirect (goaltv://configuracion-guardada?status=ok&usuario=...&password=...)
        LaunchedEffect(deepLinkAuthData) {
            val linkData = deepLinkAuthData
            if (linkData != null) {
                if (linkData.status == "other_device") {
                    Toast.makeText(
                        context,
                        "Configuración guardada para la TV (ID: ${linkData.targetDeviceId}). La TV iniciará sesión automáticamente.",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (linkData.status.equals("ok", ignoreCase = true) && linkData.username.isNotBlank() && linkData.password.isNotBlank()) {
                    Toast.makeText(context, "Configuración recibida. Conectando...", Toast.LENGTH_SHORT).show()
                    val targetServer = linkData.serverUrl.ifBlank { com.goaltv.ui.viewmodel.FIXED_SERVER_URL }
                    authViewModel.connectWithCredentials(
                        serverUrl = targetServer,
                        user = linkData.username,
                        pass = linkData.password,
                        onSuccess = {
                            liveTvViewModel.loadData()
                            vodViewModel.loadData()
                            sportsViewModel.loadSportsEvents()
                            catchUpViewModel.loadCatchUpData()
                            mainViewModel.navigateTo(AppScreen.DASHBOARD)
                        }
                    )
                } else {
                    Toast.makeText(context, "Configuración guardada en la web. Conectando...", Toast.LENGTH_SHORT).show()
                    authViewModel.loginWithDeviceAuth(activityContext) {
                        liveTvViewModel.loadData()
                        vodViewModel.loadData()
                        sportsViewModel.loadSportsEvents()
                        catchUpViewModel.loadCatchUpData()
                        mainViewModel.navigateTo(AppScreen.DASHBOARD)
                    }
                }
                onDeepLinkHandled()
            }
        }

        // Obligatorily refresh server auth/expiration on app launch when logged in without clearing channels
        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                mainViewModel.navigateTo(AppScreen.DASHBOARD)
                // Usar caché de listas descargadas previamente, pero sí actualizar
                // las conexiones y expiración silenciosamente sin bloquear.
                mainViewModel.refreshAccountInfo()
                
                liveTvViewModel.loadData()
                vodViewModel.loadData()
                sportsViewModel.loadSportsEvents()
                catchUpViewModel.loadCatchUpData()
            } else {
                mainViewModel.navigateTo(AppScreen.LOGIN)
            }
        }

        // Handle system back button navigation safely
        BackHandler(enabled = true) {
            try {
                if (activePlaybackTarget != null) {
                    mainViewModel.clearPlayback()
                } else {
                    when (currentScreen) {
                        AppScreen.LOGIN, AppScreen.DASHBOARD -> {
                            (activityContext as? ComponentActivity)?.finish()
                        }
                        else -> {
                            mainViewModel.navigateTo(AppScreen.DASHBOARD)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.LOGIN -> {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                liveTvViewModel.loadData()
                                vodViewModel.loadData()
                                sportsViewModel.loadSportsEvents()
                                mainViewModel.navigateTo(AppScreen.DASHBOARD)
                            }
                        )
                    }
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = mainViewModel,
                            liveTvViewModel = liveTvViewModel,
                            vodViewModel = vodViewModel
                        )
                    }
                    AppScreen.LIVE_TV, AppScreen.EPG -> {
                        LiveTvScreen(
                            viewModel = liveTvViewModel,
                            onChannelPlay = { channel ->
                                mainViewModel.playChannel(channel)
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.MOVIES -> {
                        MoviesScreen(
                            viewModel = vodViewModel,
                            onMoviePlay = { movie ->
                                mainViewModel.playMovie(movie)
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.SERIES -> {
                        SeriesScreen(
                            viewModel = vodViewModel,
                            onEpisodePlay = { ep ->
                                mainViewModel.playStream(
                                    title = ep.title,
                                    streamUrl = ep.streamUrl,
                                    subTitle = "S${ep.seasonNum} E${ep.episodeNum} • ${ep.duration ?: ""}",
                                    isLive = false,
                                    logoUrl = ep.iconUrl
                                )
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.SPORTS -> {
                        SportsScreen(
                            viewModel = sportsViewModel,
                            onEventPlay = { event ->
                                mainViewModel.playStream(
                                    title = event.title,
                                    streamUrl = event.streamUrl,
                                    subTitle = "${event.competition} • ${event.channelName}",
                                    isLive = true,
                                    logoUrl = event.iconUrl
                                )
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.FAVORITES -> {
                        DashboardScreen(
                            viewModel = mainViewModel,
                            liveTvViewModel = liveTvViewModel,
                            vodViewModel = vodViewModel
                        )
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = mainViewModel,
                            onLogoutClick = {
                                authViewModel.logout()
                                mainViewModel.navigateTo(AppScreen.LOGIN)
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.CATCH_UP -> {
                        CatchUpScreen(
                            viewModel = catchUpViewModel,
                            onPlayCatchup = { streamUrl, title, subTitle, logoUrl ->
                                mainViewModel.playStream(
                                    title = title,
                                    streamUrl = streamUrl,
                                    subTitle = subTitle,
                                    isLive = false,
                                    logoUrl = logoUrl
                                )
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                }
            }

            // Full Screen Player Overlay when activePlaybackTarget is present
            val activeTarget = activePlaybackTarget
            if (activeTarget != null) {
                com.goaltv.ui.player.PlayerScreen(
                    playbackTarget = activeTarget,
                    repository = repository,
                    onChannelChange = { ch -> liveTvViewModel.selectChannel(ch) },
                    onBackClick = { mainViewModel.clearPlayback() }
                )
            }

            // Device Auth Dialog Overlay (Only shown if blocked)
            val authRes = deviceAuthResult
            if (authRes != null && authRes.isBlocked) {
                BlockedDeviceDialog(
                    onExitClick = { (activityContext as? ComponentActivity)?.finish() }
                )
            }

            // Update Dialog Popup
            val updateInfo = pendingUpdateInfo
            if (updateInfo != null) {
                UpdateDialog(
                    updateInfo = updateInfo,
                    onDismiss = { pendingUpdateInfo = null }
                )
            }
        }
    }
}
