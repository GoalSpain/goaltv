package com.goaltv.ui.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.goaltv.data.repository.DataRepository
import com.goaltv.model.Category
import com.goaltv.model.EpgProgram
import com.goaltv.model.LiveChannel
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.viewmodel.PlaybackTarget
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playbackTarget: PlaybackTarget,
    repository: DataRepository,
    onChannelChange: (LiveChannel) -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current

    // Player States
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Custom UI Overlay States
    var isControlsVisible by remember { mutableStateOf(true) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var lastUserInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }



    // Live Channels & EPG State
    var liveChannels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var liveCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var currentChannelIndex by remember { mutableIntStateOf(0) }
    var currentChannel by remember { mutableStateOf<LiveChannel?>(null) }
    var epgPrograms by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }

    // Current Stream Metadata
    var currentStreamTitle by remember { mutableStateOf(playbackTarget.title) }
    var currentStreamSubTitle by remember { mutableStateOf(playbackTarget.subTitle) }
    var currentStreamLogoUrl by remember { mutableStateOf(playbackTarget.logoUrl) }
    var currentStreamUrl by remember { mutableStateOf(playbackTarget.streamUrl) }

    // Track Options
    var audioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var qualityOptions by remember { mutableStateOf<List<QualityOption>>(emptyList()) }

    val focusRequester = remember { FocusRequester() }

    // Stream Format Fallback States
    var fallbackUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentFallbackIndex by remember { mutableIntStateOf(0) }

    // ExoPlayer Instance
    val exoPlayer = remember(context) {
        val playerOkHttpClient = repository.getPlayerOkHttpClient()
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(playerOkHttpClient)
            .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")

        val mediaSourceFactory = DefaultMediaSourceFactory(okHttpDataSourceFactory)

        // Set safe timeout configuration to prevent decoder lockups on low-RAM Smart TVs (e.g. TCL)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            ).build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                playWhenReady = true
            }
    }

    // Intercept back button, stop ExoPlayer, release resources, and return directly to previous screen
    val handleBackPress = {
        if (isDrawerOpen) {
            isDrawerOpen = false
        } else {
            try {
                exoPlayer.stop()
            } catch (_: Exception) {}
            onBackClick()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    // Load Live Channels & EPG if Live TV stream
    LaunchedEffect(playbackTarget) {
        if (playbackTarget.isLive) {
            val channels = repository.getLiveChannels(playbackTarget.categoryId)
            val categories = repository.getLiveCategories()
            liveChannels = channels
            liveCategories = categories

            val foundIdx = channels.indexOfFirst { it.id == playbackTarget.channelId || it.streamUrl == playbackTarget.streamUrl }
            if (foundIdx >= 0) {
                currentChannelIndex = foundIdx
                currentChannel = channels[foundIdx]
                val epg = repository.getEpgForChannel(channels[foundIdx].id, channels[foundIdx].epgId)
                epgPrograms = epg
            }
        }
    }

    // Prepare stream whenever currentStreamUrl changes
    LaunchedEffect(currentStreamUrl) {
        errorMessage = null
        isBuffering = true
        fallbackUrls = generateFallbackUrls(currentStreamUrl)
        currentFallbackIndex = 0
        val urlToPlay = fallbackUrls.getOrNull(0) ?: currentStreamUrl
        if (urlToPlay.isNotBlank() && (urlToPlay.startsWith("http://") || urlToPlay.startsWith("https://") || urlToPlay.startsWith("rtmp://") || urlToPlay.startsWith("file://"))) {
            try {
                exoPlayer.stop()
                exoPlayer.setMediaItem(MediaItem.fromUri(urlToPlay))
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                errorMessage = "Error al abrir la transmisión: ${e.localizedMessage}"
                isBuffering = false
            }
        } else {
            errorMessage = "URL de transmisión no válida"
            isBuffering = false
        }
    }

    // Attach ExoPlayer Listener & Lifecycle Management
    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                if (currentFallbackIndex + 1 < fallbackUrls.size) {
                    currentFallbackIndex++
                    val nextUrl = fallbackUrls[currentFallbackIndex]
                    isBuffering = true
                    try {
                        exoPlayer.stop()
                        exoPlayer.setMediaItem(MediaItem.fromUri(nextUrl))
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } catch (_: Exception) {
                        errorMessage = error.localizedMessage ?: "Fallo de conexión en la transmisión o formato inválido"
                        isBuffering = false
                    }
                } else {
                    errorMessage = error.localizedMessage ?: "Fallo de conexión en la transmisión o formato inválido"
                    isBuffering = false
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val newAudioTracks = mutableListOf<TrackOption>()
                val newSubTracks = mutableListOf<TrackOption>()
                val newQualities = mutableListOf<QualityOption>()

                for (group in tracks.groups) {
                    val trackGroup = group.mediaTrackGroup
                    when (group.type) {
                        C.TRACK_TYPE_AUDIO -> {
                            for (i in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(i)
                                val label = format.label ?: format.language ?: "Pista de Audio ${i + 1}"
                                newAudioTracks.add(
                                    TrackOption(
                                        id = "audio_${group.type}_$i",
                                        name = label,
                                        language = format.language,
                                        groupIndex = 0,
                                        trackIndex = i,
                                        trackType = C.TRACK_TYPE_AUDIO,
                                        isSelected = group.isTrackSelected(i)
                                    )
                                )
                            }
                        }
                        C.TRACK_TYPE_TEXT -> {
                            for (i in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(i)
                                val label = format.label ?: format.language ?: "Subtítulo ${i + 1}"
                                newSubTracks.add(
                                    TrackOption(
                                        id = "sub_${group.type}_$i",
                                        name = label,
                                        language = format.language,
                                        groupIndex = 0,
                                        trackIndex = i,
                                        trackType = C.TRACK_TYPE_TEXT,
                                        isSelected = group.isTrackSelected(i)
                                    )
                                )
                            }
                        }
                        C.TRACK_TYPE_VIDEO -> {
                            for (i in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(i)
                                val label = if (format.height > 0) "${format.height}P" else "Automática"
                                newQualities.add(
                                    QualityOption(
                                        id = "video_${group.type}_$i",
                                        label = label,
                                        width = format.width,
                                        height = format.height,
                                        bitrate = format.bitrate,
                                        groupIndex = 0,
                                        trackIndex = i,
                                        isSelected = group.isTrackSelected(i)
                                    )
                                )
                            }
                        }
                    }
                }

                audioTracks = newAudioTracks
                subtitleTracks = newSubTracks
                qualityOptions = newQualities
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }

        // Prevent screen sleep / suspension during video playback on mobile & TV
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        exoPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            try {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                lifecycleOwner.lifecycle.removeObserver(observer)
                exoPlayer.removeListener(listener)
                
                // Ensure safe thread shutdown on TCL and Mediatek devices
                exoPlayer.playWhenReady = false
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Progress Update Timer
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            } catch (_: Exception) {}
            delay(500)
        }
    }

    // Auto-hide controls timer (4s)
    LaunchedEffect(isControlsVisible, lastUserInteraction, isDrawerOpen) {
        if (isControlsVisible && !isDrawerOpen) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Toast auto-clear timer (3s)
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            toastMessage = null
        }
    }

    // Helper to trigger user interaction
    val triggerInteraction = {
        lastUserInteraction = System.currentTimeMillis()
        if (!isControlsVisible) {
            isControlsVisible = true
        }
    }

    // Channel Switch Helper
    fun switchChannel(newIndex: Int) {
        if (liveChannels.isEmpty()) return
        val clampedIndex = (newIndex + liveChannels.size) % liveChannels.size
        val ch = liveChannels.getOrNull(clampedIndex) ?: return
        currentChannelIndex = clampedIndex
        currentChannel = ch
        currentStreamTitle = ch.name
        currentStreamSubTitle = "TV EN DIRECTO • Canal ${ch.channelNumber ?: (clampedIndex + 1)}"
        currentStreamLogoUrl = ch.iconUrl
        currentStreamUrl = ch.streamUrl
        toastMessage = "Cambiado a: ${ch.name}"

        onChannelChange(ch)
        triggerInteraction()
    }

    val outerBoxFocusRequester = remember { FocusRequester() }

    // Request focus for KeyListener when controls auto-hide
    LaunchedEffect(isControlsVisible) {
        if (!isControlsVisible) {
            try {
                outerBoxFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Main Box layout with Key & Touch listener
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .focusRequester(outerBoxFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                val isSelectionKey = event.key == Key.DirectionCenter ||
                                     event.key == Key.Enter ||
                                     event.key == Key.Spacebar ||
                                     keyCode == 23 || keyCode == 66

                val isMenuKey = event.key == Key.Menu ||
                                keyCode == 82 || keyCode == 227 || keyCode == 176

                val isUpKey = event.key == Key.DirectionUp || keyCode == 19 || keyCode == 166
                val isDownKey = event.key == Key.DirectionDown || keyCode == 20 || keyCode == 167
                val isLeftKey = event.key == Key.DirectionLeft || keyCode == 21 || keyCode == 89
                val isRightKey = event.key == Key.DirectionRight || keyCode == 22 || keyCode == 90

                if (event.type == KeyEventType.KeyDown) {
                    if (isMenuKey) {
                        triggerInteraction()
                        return@onKeyEvent true
                    }

                    if (!isControlsVisible) {
                        triggerInteraction()
                        if (isSelectionKey) {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            return@onKeyEvent true
                        }

                        if (isUpKey) {
                            if (playbackTarget.isLive) {
                                switchChannel(currentChannelIndex - 1)
                            } else {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            }
                            return@onKeyEvent true
                        }

                        if (isDownKey) {
                            if (playbackTarget.isLive) {
                                switchChannel(currentChannelIndex + 1)
                            } else {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            }
                            return@onKeyEvent true
                        }

                        if (isLeftKey) {
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            return@onKeyEvent true
                        }

                        if (isRightKey) {
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            return@onKeyEvent true
                        }

                        if (event.key == Key.Back) {
                            handleBackPress()
                            return@onKeyEvent true
                        }

                        return@onKeyEvent true
                    } else {
                        // When controls ARE visible
                        if (isSelectionKey) {
                            triggerInteraction()
                            return@onKeyEvent false
                        }

                        triggerInteraction()

                        if (isLeftKey) {
                            val moved = focusManager.moveFocus(FocusDirection.Left)
                            if (!moved) focusManager.moveFocus(FocusDirection.Previous)
                            return@onKeyEvent true
                        }

                        if (isRightKey) {
                            val moved = focusManager.moveFocus(FocusDirection.Right)
                            if (!moved) focusManager.moveFocus(FocusDirection.Next)
                            return@onKeyEvent true
                        }

                        if (isUpKey) {
                            if (playbackTarget.isLive) {
                                switchChannel(currentChannelIndex - 1)
                            } else {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            }
                            return@onKeyEvent true
                        }

                        if (isDownKey) {
                            if (playbackTarget.isLive) {
                                switchChannel(currentChannelIndex + 1)
                            } else {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            }
                            return@onKeyEvent true
                        }

                        if (event.key == Key.Back) {
                            handleBackPress()
                            return@onKeyEvent true
                        }

                        return@onKeyEvent false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isControlsVisible) {
                    isControlsVisible = false
                } else {
                    triggerInteraction()
                }
            }
    ) {
        // Video Player View Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (aspectRatioMode) {
                        AspectRatioMode.SIXTEEN_NINE -> Modifier.aspectRatio(16f / 9f)
                        else -> Modifier.fillMaxSize()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = aspectRatioMode.resizeMode
                        keepScreenOn = true
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = aspectRatioMode.resizeMode
                    playerView.keepScreenOn = isPlaying
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom Overlay Controls
        PlayerControls(
            isVisible = isControlsVisible && errorMessage == null,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            title = currentStreamTitle,
            subTitle = currentStreamSubTitle,
            logoUrl = currentStreamLogoUrl,
            isLive = playbackTarget.isLive,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            aspectRatioMode = aspectRatioMode,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            qualityOptions = qualityOptions,
            toastMessage = toastMessage,
            onPlayPauseToggle = {
                triggerInteraction()
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onSeekTo = { pos ->
                triggerInteraction()
                exoPlayer.seekTo(pos)
                currentPositionMs = pos
            },
            onRewind10s = {
                triggerInteraction()
                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                exoPlayer.seekTo(target)
                currentPositionMs = target
            },
            onForward10s = {
                triggerInteraction()
                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                exoPlayer.seekTo(target)
                currentPositionMs = target
            },
            onPreviousClick = {
                if (playbackTarget.isLive) {
                    switchChannel(currentChannelIndex - 1)
                } else {
                    val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                    exoPlayer.seekTo(target)
                }
            },
            onNextClick = {
                if (playbackTarget.isLive) {
                    switchChannel(currentChannelIndex + 1)
                } else {
                    val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                    exoPlayer.seekTo(target)
                }
            },
            onToggleAspectRatio = {
                triggerInteraction()
                val modes = AspectRatioMode.entries.toTypedArray()
                val nextIdx = (aspectRatioMode.ordinal + 1) % modes.size
                aspectRatioMode = modes[nextIdx]
                toastMessage = "Relación de Aspecto: ${aspectRatioMode.label}"
            },
            onSelectAudioTrack = { track ->
                triggerInteraction()
                val builder = exoPlayer.trackSelectionParameters.buildUpon()
                builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                exoPlayer.trackSelectionParameters = builder.build()
                toastMessage = "Audio: ${track.name}"
            },
            onSelectSubtitleTrack = { track ->
                triggerInteraction()
                val builder = exoPlayer.trackSelectionParameters.buildUpon()
                if (track == null) {
                    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    toastMessage = "Subtítulos: Desactivados"
                } else {
                    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    toastMessage = "Subtítulos: ${track.name}"
                }
                exoPlayer.trackSelectionParameters = builder.build()
            },
            onSelectQuality = { quality ->
                triggerInteraction()
                toastMessage = "Calidad: ${quality.label}"
            },
            onToggleDrawer = {
                triggerInteraction()
                isDrawerOpen = !isDrawerOpen
            },
            onBackClick = handleBackPress
        )

        // Live Channels Side Drawer
        ChannelDrawer(
            isOpen = isDrawerOpen,
            channels = liveChannels,
            currentChannelId = currentChannel?.id,
            categories = liveCategories,
            epgPrograms = epgPrograms,
            onChannelSelect = { selectedCh ->
                val idx = liveChannels.indexOfFirst { it.id == selectedCh.id }
                if (idx >= 0) {
                    switchChannel(idx)
                } else {
                    currentStreamTitle = selectedCh.name
                    currentStreamUrl = selectedCh.streamUrl
                    currentStreamLogoUrl = selectedCh.iconUrl
                    toastMessage = "Cambiado a: ${selectedCh.name}"
                }
                isDrawerOpen = false
            },
            onClose = { isDrawerOpen = false },
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // Error Handling View with Retry Button
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.95f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .padding(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Error de Reproducción",
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Error de Reproducción",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = errorMessage ?: "No se pudo reproducir la transmisión.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onBackClick,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("VOLVER", color = Color.White)
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isBuffering = true
                                fallbackUrls = generateFallbackUrls(currentStreamUrl)
                                currentFallbackIndex = 0
                                val urlToPlay = fallbackUrls.getOrNull(0) ?: currentStreamUrl
                                exoPlayer.stop()
                                exoPlayer.setMediaItem(MediaItem.fromUri(urlToPlay))
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reintentar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REINTENTAR TRANSMISIÓN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Auto-request focus for key handling
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private fun generateFallbackUrls(url: String): List<String> {
    if (url.isBlank()) return emptyList()
    val cleanUrl = url.trim()

    val knownExtensions = listOf(".m3u8", ".ts", ".mp4", ".mkv", ".avi", ".flv", ".mov")
    val foundExt = knownExtensions.firstOrNull { cleanUrl.endsWith(it, ignoreCase = true) }

    val baseUrl = if (foundExt != null) {
        cleanUrl.substring(0, cleanUrl.length - foundExt.length)
    } else {
        cleanUrl
    }

    val candidates = mutableListOf<String>()
    candidates.add(cleanUrl)

    val fallbackExtensions = if (cleanUrl.contains("/live/")) {
        listOf(".ts", ".m3u8", "")
    } else {
        listOf(".mp4", ".mkv", ".m3u8", ".ts", "")
    }

    for (ext in fallbackExtensions) {
        val candidate = if (ext.isEmpty()) baseUrl else "$baseUrl$ext"
        if (!candidates.contains(candidate)) {
            candidates.add(candidate)
        }
    }

    return candidates
}
