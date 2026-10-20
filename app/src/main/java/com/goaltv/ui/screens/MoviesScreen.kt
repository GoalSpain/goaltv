package com.goaltv.ui.screens

import com.goaltv.R
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
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.goaltv.model.VodMovie
import com.goaltv.ui.components.FocusableCard
import com.goaltv.ui.components.GoalTvLogo
import com.goaltv.ui.components.MovieDetailDialog
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.GoldAccent
import com.goaltv.ui.theme.OrangeGlow
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.theme.TextSecondary
import com.goaltv.ui.viewmodel.VodViewModel

@Composable
fun MoviesScreen(
    viewModel: VodViewModel,
    onMoviePlay: (VodMovie) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredMovies = uiState.vodMovies

    val firstMovieFocusRequester = remember { FocusRequester() }

    LaunchedEffect(filteredMovies) {
        if (filteredMovies.isNotEmpty()) {
            try {
                firstMovieFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Detail Dialog State
    uiState.selectedMovie?.let { selectedMovie ->
        MovieDetailDialog(
            movie = selectedMovie,
            onDismiss = { viewModel.selectMovie(null) },
            onPlayClick = { movie -> onMoviePlay(movie) },
            onToggleFavorite = { id -> viewModel.toggleMovieFavorite(id) }
        )
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
            // Header Bar
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
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = "CINE",
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CINE",
                        fontSize = if (isCompactWidth) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                GoalTvLogo(sizeScale = if (isCompactWidth) 0.65f else 0.75f, showGlow = false)
            }

            LaunchedEffect(Unit) {
                if (uiState.vodMovies.isEmpty()) {
                    viewModel.loadData()
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val categoryChipFocusRequester = remember { FocusRequester() }

            // Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                item {
                    val isAllSelected = uiState.selectedVodCategory == null && !uiState.isMovieFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectVodCategory(null) },
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
                                text = "Todas las Películas",
                                color = if (isAllSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                item {
                    val isFavSelected = uiState.isMovieFavoritesSelected
                    FocusableCard(
                        onClick = { viewModel.selectMovieFavorites() },
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

                itemsIndexed(uiState.vodCategories, key = { idx, cat -> "${cat.id}_$idx" }) { _, cat ->
                    val isSelected = uiState.selectedVodCategory?.id == cat.id
                    FocusableCard(
                        onClick = { viewModel.selectVodCategory(cat) },
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
            } else if (filteredMovies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No se encontraron películas.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                // Adaptive Grid: minSize = 105.dp shows 4-5 posters on mobile and 6 on TV cleanly
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 105.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusProperties { up = categoryChipFocusRequester }
                ) {
                    gridItemsIndexed(filteredMovies, key = { idx, m -> "${m.id}_$idx" }) { index, movie ->
                        MoviePosterCard(
                            movie = movie,
                            onClick = { viewModel.selectMovie(movie) },
                            modifier = if (index == 0) Modifier.focusRequester(firstMovieFocusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}

fun formatReleaseYear(dateStr: String?): String? {
    if (dateStr.isNullOrBlank()) return null
    val trimmed = dateStr.trim()
    if (trimmed.length == 4 && trimmed.all { it.isDigit() }) {
        val yearNum = trimmed.toIntOrNull()
        if (yearNum != null && yearNum in 1900..2099) return trimmed
    }
    if (trimmed.length >= 4 && trimmed.take(4).all { it.isDigit() }) {
        val yearNum = trimmed.take(4).toIntOrNull()
        if (yearNum != null && yearNum in 1900..2099) return yearNum.toString()
    }
    return null
}

@Composable
fun StarRatingBar(
    rating: Double?,
    modifier: Modifier = Modifier,
    starSize: Dp = 10.dp,
    starColor: Color = GoldAccent,
    showNumericRating: Boolean = true
) {
    val score = rating?.toFloat() ?: 0f
    val filledCount = if (score > 5f) (score / 2f).roundToInt().coerceIn(1, 5) else score.roundToInt().coerceIn(1, 5)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = if (i <= filledCount) starColor else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(starSize)
            )
        }

        if (showNumericRating && rating != null && rating > 0.0) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = String.format(Locale.US, "%.1f", rating),
                color = starColor,
                fontSize = (starSize.value + 1).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MoviePosterCard(
    movie: VodMovie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayPosterUrl by remember(movie.id, movie.posterUrl) { mutableStateOf(movie.posterUrl) }

    LaunchedEffect(movie.id, movie.posterUrl) {
        if (displayPosterUrl.isNullOrBlank()) {
            try {
                val info = com.goaltv.data.network.TmdbHelper.fetchTmdbInfo(movie.name, isMovie = true)
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
            .height(145.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!displayPosterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = displayPosterUrl,
                    contentDescription = movie.name,
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
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = movie.name,
                            tint = OrangePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = movie.name,
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

            // Star Rating Badge (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                StarRatingBar(rating = movie.rating, starSize = 9.dp, showNumericRating = true)
            }

            // Bottom Gradient Overlay & Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = movie.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    formatReleaseYear(movie.releaseDate)?.let { year ->
                        Text(
                            text = year,
                            color = OrangeGlow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
