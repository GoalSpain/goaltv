package com.goaltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goaltv.model.SportsEvent
import com.goaltv.ui.components.FocusableCard
import com.goaltv.ui.components.GoalTvLogo
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangeGlow
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusSuccess
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.viewmodel.SportsViewModel

@Composable
fun SportsScreen(
    viewModel: SportsViewModel,
    onEventPlay: (SportsEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSportsEvents()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val keyCode = event.nativeKeyEvent.keyCode
                    val isLeft = event.key == Key.DirectionLeft || keyCode == 21
                    val isRight = event.key == Key.DirectionRight || keyCode == 22
                    val tabs = listOf("Hoy", "Mañana", "Pasado Mañana")
                    if (isLeft) {
                        val idx = tabs.indexOfFirst { it.equals(uiState.selectedDateTab, ignoreCase = true) }
                        if (idx > 0) viewModel.selectDateTab(tabs[idx - 1])
                        true
                    } else if (isRight) {
                        val idx = tabs.indexOfFirst { it.equals(uiState.selectedDateTab, ignoreCase = true) }
                        if (idx >= 0 && idx < tabs.size - 1) viewModel.selectDateTab(tabs[idx + 1])
                        true
                    } else false
                } else false
            }
    ) {
        val screenWidth = maxWidth
        val isCompactWidth = screenWidth < 600.dp
        val horizontalPadding = if (isCompactWidth) 14.dp else 28.dp
        val verticalPadding = if (maxHeight < 500.dp) 8.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // TOP HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceDarkCard)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver al Dashboard",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "PROGRAMACIÓN DEPORTIVA",
                            color = Color.White,
                            fontSize = if (isCompactWidth) 15.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Guía de eventos deportivos en vivo y transmisiones",
                            color = TextMuted,
                            fontSize = if (isCompactWidth) 10.sp else 11.sp
                        )
                    }
                }

                GoalTvLogo(sizeScale = if (isCompactWidth) 0.6f else 0.75f, showGlow = false)
            }

            Spacer(modifier = Modifier.height(10.dp))

            val todayDateStr = remember { getFormattedDate(0) }
            val tomorrowFullDate = remember { getFullFormattedDate(1) }
            val pasadoFullDate = remember { getFullFormattedDate(2) }

            val dateTabs = listOf(
                "Hoy" to "HOY ($todayDateStr)",
                "Mañana" to tomorrowFullDate,
                "Pasado Mañana" to pasadoFullDate
            )

            // DATE TABS: HOY, MAÑANA & PASADO MAÑANA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dateTabs.forEach { (tabKey, tabLabel) ->
                    val isSelected = uiState.selectedDateTab.equals(tabKey, ignoreCase = true)
                    FocusableCard(
                        onClick = { viewModel.selectDateTab(tabKey) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isSelected) OrangePrimary else SurfaceDarkCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                color = if (isSelected) Color.White else TextMuted,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // EVENTS LIST (Continuous Vertical Scroll)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (uiState.filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No hay eventos deportivos disponibles para '${uiState.selectedDateTab}'",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                val firstLiveIndex = remember(uiState.filteredEvents, uiState.selectedDateTab) {
                    if (uiState.selectedDateTab.equals("Hoy", ignoreCase = true)) {
                        val idx = uiState.filteredEvents.indexOfFirst {
                            isSportsEventCurrentlyLive(it.dateTimeDisplay, true)
                        }
                        if (idx >= 0) idx else 0
                    } else 0
                }

                LaunchedEffect(firstLiveIndex) {
                    if (firstLiveIndex > 0) {
                        try {
                            listState.animateScrollToItem(firstLiveIndex)
                        } catch (_: Exception) {}
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredEvents, key = { it.id }) { event ->
                        SportsEventCard(
                            event = event,
                            isTodayTab = uiState.selectedDateTab.equals("Hoy", ignoreCase = true)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SportsEventCard(
    event: SportsEvent,
    isTodayTab: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isLive = remember(event.dateTimeDisplay, isTodayTab) {
        isSportsEventCurrentlyLive(event.dateTimeDisplay, isTodayTab)
    }

    FocusableCard(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) Color(0xFF282218) else SurfaceDarkCard
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Top Row: Sport Badge + Competition Badge + Live Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sport Category Badge
                    Surface(
                        color = OrangePrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = event.sportCategory,
                            color = OrangeGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Competition Badge
                    Surface(
                        color = Color(0xFF2A1B4E),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF673AB7))
                    ) {
                        Text(
                            text = event.competition,
                            color = Color(0xFFD1C4E9),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Teams / Event Title
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Row: Hour & Channel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isLive) Color(0xFF382B1C) else Color(0xFF1B1B28), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (isLive) OrangePrimary else StatusSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.dateTimeDisplay,
                        color = if (isLive) OrangePrimary else StatusSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = "Emisión",
                        tint = OrangePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.channelName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun getFormattedDate(dayOffset: Int): String {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Madrid")).apply {
        add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
    }
    val sdf = java.text.SimpleDateFormat("d 'de' MMMM", java.util.Locale("es", "ES"))
    return sdf.format(cal.time)
}

private fun getFullFormattedDate(dayOffset: Int): String {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Madrid")).apply {
        add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
    }
    val dayName = java.text.SimpleDateFormat("EEEE", java.util.Locale("es", "ES")).format(cal.time)
    val dayAndMonth = java.text.SimpleDateFormat("d 'de' MMMM", java.util.Locale("es", "ES")).format(cal.time)
    return "${dayName.replaceFirstChar { it.uppercase() }} $dayAndMonth"
}

private fun isSportsEventCurrentlyLive(dateTimeDisplay: String, isTodayTab: Boolean): Boolean {
    if (!isTodayTab) return false
    return try {
        val hourMatch = Regex("""\b(\d{1,2}:\d{2})\b""").find(dateTimeDisplay) ?: return false
        val timeStr = hourMatch.value

        val calNow = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Madrid"))
        val currentHour = calNow.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calNow.get(java.util.Calendar.MINUTE)
        val nowTotalMinutes = currentHour * 60 + currentMinute

        val parts = timeStr.split(":")
        val startHour = parts[0].toInt()
        val startMin = parts[1].toInt()
        val startTotalMinutes = startHour * 60 + startMin

        val endTotalMinutes = startTotalMinutes + 115
        nowTotalMinutes in startTotalMinutes..endTotalMinutes
    } catch (_: Exception) {
        false
    }
}
