package com.goaltv.ui.screens

import com.goaltv.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.goaltv.model.Episode
import com.goaltv.model.Series
import com.goaltv.ui.components.FocusableCard
import com.goaltv.ui.components.GoalTvLogo
import com.goaltv.ui.components.SeriesDetailDialog
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.GoldAccent
import com.goaltv.ui.theme.OrangeGlow
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.theme.TextSecondary
import com.goaltv.ui.viewmodel.VodViewModel

@Composable
fun SeriesScreen(
    viewModel: VodViewModel,
    onEpisodePlay: (Episode) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val firstSeriesFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.seriesList) {
        if (uiState.seriesList.isNotEmpty()) {
            try {
                firstSeriesFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Series Detail Modal
    uiState.selectedSeries?.let { selectedSeries ->
        SeriesDetailDialog(
            series = selectedSeries,
            selectedSeasonNum = uiState.selectedSeasonNum,
            episodes = uiState.seriesEpisodes,
            onSeasonSelect = { num -> viewModel.selectSeason(num) },
            onEpisodePlay = { ep ->
                onEpisodePlay(ep)
                viewModel.selectSeries(null)
            },
            onToggleFavorite = { id -> viewModel.toggleSeriesFavorite(id) },
            onDismiss = { viewModel.selectSeries(null) }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.selectSeriesCategory(null)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
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
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = "Series",
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Series",
                        fontSize = if (isCompactWidth) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                GoalTvLogo(sizeScale = if (isCompactWidth) 0.65f else 0.75f, showGlow = false)
            }

            val categoryChipFocusRequester = remember { FocusRequester() }

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    val isAllSelected = uiState.selectedSeriesCategory == null && !uiState.isSeriesFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectSeriesCategory(null) },
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
                                text = "Todas las Series",
                                color = if (isAllSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    val isFavSelected = uiState.isSeriesFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectSeriesFavorites() },
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

                items(uiState.seriesCategories, key = { it.id }) { cat ->
                    val isSelected = uiState.selectedSeriesCategory?.id == cat.id
                    FocusableCard(
                        onClick = { viewModel.selectSeriesCategory(cat) },
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
                                text = cat.name,
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (uiState.seriesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No se encontraron series de TV.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                // Adaptive Series Grid: minSize = 110.dp shows 3-4 series on mobile and 5-6 on TV
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusProperties { up = categoryChipFocusRequester }
                ) {
                    itemsIndexed(uiState.seriesList, key = { _, s -> s.id }) { index, series ->
                        SeriesCard(
                            series = series,
                            onClick = { viewModel.selectSeries(series) },
                            modifier = if (index == 0) Modifier.focusRequester(firstSeriesFocusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesCard(
    series: Series,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayPosterUrl by remember(series.id, series.posterUrl) { mutableStateOf(series.posterUrl) }

    LaunchedEffect(series.id, series.posterUrl) {
        if (displayPosterUrl.isNullOrBlank()) {
            try {
                val info = com.goaltv.data.network.TmdbHelper.fetchTmdbInfo(series.name, isMovie = false)
                if (info?.posterPath != null) {
                    displayPosterUrl = info.posterPath
                }
            } catch (_: Exception) {}
        }
    }

    FocusableCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!displayPosterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = displayPosterUrl,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDarkCard)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDarkCard)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = series.name,
                            tint = OrangePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = series.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            series.rating?.let { rating ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Calificación",
                            tint = GoldAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$rating",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = series.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${series.seasons.size.coerceAtLeast(1)} Temporada(s)",
                        color = OrangeGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
