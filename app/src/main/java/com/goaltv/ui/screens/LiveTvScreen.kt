package com.goaltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.goaltv.model.EpgProgram
import com.goaltv.model.LiveChannel
import com.goaltv.ui.components.FocusableCard
import com.goaltv.ui.components.GoalTvLogo
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangeGlow
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusSuccess
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.theme.TextSecondary
import com.goaltv.ui.viewmodel.LiveTvViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun LiveTvScreen(
    viewModel: LiveTvViewModel,
    onChannelPlay: (LiveChannel) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isCompactWidth = screenWidth < 600.dp

        // Dynamically compute channel item row height based on maxHeight so 8 channels fit cleanly on large displays
        // while smaller mobile screens remain fully legible without text clipping
        val headerPaddingEstimate = 120.dp
        val availableListHeight = screenHeight - headerPaddingEstimate
        val computedChannelRowHeight: Dp = (availableListHeight / 8.5f).coerceIn(40.dp, 54.dp)

        val horizontalPadding = if (isCompactWidth) 12.dp else 24.dp
        val verticalPadding = if (screenHeight < 500.dp) 6.dp else 10.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
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
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = "TV",
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TV",
                        fontSize = if (isCompactWidth) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                GoalTvLogo(sizeScale = if (isCompactWidth) 0.65f else 0.75f, showGlow = false)
            }

            // Refresh categories & channels whenever TV screen is opened
            LaunchedEffect(Unit) {
                viewModel.loadData()
            }

            val categoryChipFocusRequester = remember { FocusRequester() }

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    val isAllSelected = uiState.selectedCategory == null && !uiState.isFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectCategory(null) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = if (isAllSelected) Modifier.focusRequester(categoryChipFocusRequester) else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isAllSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isAllSelected) 2.dp else 1.dp,
                                    color = if (isAllSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Todos los Canales",
                                color = if (isAllSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    val isFavSelected = uiState.isFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectFavorites() },
                        shape = RoundedCornerShape(18.dp),
                        modifier = if (isFavSelected) Modifier.focusRequester(categoryChipFocusRequester) else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isFavSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isFavSelected) 2.dp else 1.dp,
                                    color = if (isFavSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⭐ Favoritos",
                                color = if (isFavSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isFavSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                items(uiState.categories, key = { it.id }) { category ->
                    val isSelected = uiState.selectedCategory?.id == category.id
                    FocusableCard(
                        onClick = { viewModel.selectCategory(category) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = if (isSelected) Modifier.focusRequester(categoryChipFocusRequester) else Modifier
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
                                text = category.name,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
                val selectedChannelFocusRequester = remember { FocusRequester() }

                // Main Content: Channels List on Left, EPG Guide on Right
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Channels Column (45% width)
                    Column(
                        modifier = Modifier
                            .weight(if (isCompactWidth) 0.5f else 0.45f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "CANALES (${uiState.channels.size})",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        val listState = rememberLazyListState()
                        val selectedChannelIndex = remember(uiState.channels, uiState.selectedChannel) {
                            uiState.channels.indexOfFirst { it.id == uiState.selectedChannel?.id }
                        }
                        val selectedChannelFocusRequester = remember { FocusRequester() }

                        LaunchedEffect(uiState.selectedChannel?.id) {
                            if (selectedChannelIndex >= 0) {
                                try {
                                    listState.animateScrollToItem(selectedChannelIndex)
                                    selectedChannelFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                        }

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusProperties { up = categoryChipFocusRequester }
                        ) {
                            items(uiState.channels, key = { it.id }) { channel ->
                                val isSelected = uiState.selectedChannel?.id == channel.id
                                ChannelListItem(
                                    channel = channel,
                                    isSelected = isSelected,
                                    onSelect = { viewModel.selectChannel(channel) },
                                    onPlay = { onChannelPlay(channel) },
                                    onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                                    itemHeight = computedChannelRowHeight,
                                    modifier = if (isSelected) Modifier.focusRequester(selectedChannelFocusRequester) else Modifier
                                )
                            }
                        }
                    }

                    // EPG Timeline Column (55% width)
                    Column(
                        modifier = Modifier
                            .weight(if (isCompactWidth) 0.5f else 0.55f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(10.dp)
                            .focusProperties { left = selectedChannelFocusRequester }
                    ) {
                        val channel = uiState.selectedChannel

                        if (channel != null) {
                            // Channel Header inside EPG panel
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = channel.iconUrl,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceDarkCard)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = channel.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Canal ${channel.channelNumber ?: ""}",
                                            fontSize = 11.sp,
                                            color = OrangeGlow
                                        )
                                    }
                                }

                                // Quick Play Button
                                IconButton(
                                    onClick = { onChannelPlay(channel) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(OrangePrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Reproducción Rápida",
                                        tint = Color.White
                                    )
                                }
                            }

                            // EPG Timeline List
                            Text(
                                text = "GUÍA DE PROGRAMACIÓN Y LÍNEA DE TIEMPO",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (uiState.epgPrograms.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No hay datos EPG disponibles para este canal.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                val epgListState = rememberLazyListState()
                                val currentLiveIndex = remember(uiState.selectedChannel?.id, uiState.epgPrograms) {
                                    val idx = uiState.epgPrograms.indexOfFirst { it.isCurrentlyLive }
                                    if (idx >= 0) idx else 0
                                }

                                LaunchedEffect(uiState.selectedChannel?.id, uiState.epgPrograms) {
                                    try {
                                        epgListState.scrollToItem(currentLiveIndex)
                                    } catch (_: Exception) {}
                                }

                                LazyColumn(
                                    state = epgListState,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.epgPrograms, key = { it.id }) { program ->
                                        EpgProgramItem(program = program)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Selecciona un canal para ver la guía EPG.",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: LiveChannel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    itemHeight: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Main Channel Play Area (Logo + Name + Number)
        FocusableCard(
            onClick = {
                if (isSelected) {
                    onPlay()
                } else {
                    onSelect()
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onSelect()
                    }
                },
            shape = RoundedCornerShape(8.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF2B2B40) else SurfaceDarkCard
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = channel.iconUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF16161E))
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = channel.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) OrangeGlow else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Dedicated Focusable Heart / Favorite Button
        FocusableCard(
            onClick = onToggleFavorite,
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(8.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (channel.isFavorite) Color(0xFF3B1A24) else Color(0xFF1E1E2C)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (channel.isFavorite) Color(0xFFFF3366) else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EpgProgramItem(
    program: EpgProgram,
    modifier: Modifier = Modifier
) {
    val isLive = program.isCurrentlyLive || program.isLive
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Madrid")
        }
    }
    val startTime = timeFormatter.format(Date(program.startTimestamp * 1000))
    val endTime = timeFormatter.format(Date(program.endTimestamp * 1000))

    FocusableCard(
        onClick = {},
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isLive) Modifier.border(1.5.dp, OrangePrimary, RoundedCornerShape(8.dp)) else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isLive) Color(0xFF282218) else SurfaceDarkCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Hora",
                        tint = if (isLive) OrangePrimary else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$startTime - $endTime",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) OrangePrimary else TextSecondary
                    )
                }

                if (isLive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusSuccess)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "EN DIRECTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = program.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (!program.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = program.description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLive) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { program.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = OrangePrimary,
                    trackColor = Color(0xFF403020)
                )
            }
        }
    }
}
