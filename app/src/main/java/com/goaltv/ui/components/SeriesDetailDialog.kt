package com.goaltv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.goaltv.model.Episode
import com.goaltv.model.Series
import com.goaltv.ui.screens.StarRatingBar
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.theme.TextSecondary

@Composable
fun SeriesDetailDialog(
    series: Series,
    selectedSeasonNum: Int,
    episodes: List<Episode>,
    onSeasonSelect: (Int) -> Unit,
    onEpisodePlay: (Episode) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredEpisodes = episodes.filter { it.seasonNum == selectedSeasonNum }
        .ifEmpty { episodes }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = SurfaceDark
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Item
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 36.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AsyncImage(
                                model = series.posterUrl,
                                contentDescription = series.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(95.dp)
                                    .height(135.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDarkCard)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = series.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = { onToggleFavorite(series.id) }) {
                                        Icon(
                                            imageVector = if (series.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Favorito",
                                            tint = if (series.isFavorite) OrangePrimary else Color.White
                                        )
                                    }
                                }

                                if (series.rating != null && series.rating > 0.0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    StarRatingBar(rating = series.rating, starSize = 13.dp, showNumericRating = true)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = series.overview ?: "No hay resumen disponible.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 4
                                )
                            }
                        }
                    }

                    // Seasons Section
                    item {
                        Text(
                            text = "Temporadas",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        val availableSeasons = if (series.seasons.isNotEmpty()) {
                            series.seasons.map { it.seasonNumber }.distinct().sorted()
                        } else {
                            episodes.map { it.seasonNum }.distinct().sorted()
                        }
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableSeasons) { seasonNum ->
                                val isSelected = seasonNum == selectedSeasonNum
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSeasonSelect(seasonNum) },
                                    label = { Text("Temporada $seasonNum") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangePrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceDarkCard,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Episodes Section Header
                    item {
                        Text(
                            text = "Episodios (${filteredEpisodes.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Episode Items
                    items(filteredEpisodes) { episode ->
                        EpisodeRowItem(
                            episode = episode,
                            onPlayClick = { onEpisodePlay(episode) }
                        )
                    }
                }

                // Close Button Top Right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    episode: Episode,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onPlayClick() },
        color = SurfaceDarkCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play Button Circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrangePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Reproducir Episodio",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Episode Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${episode.episodeNum}. ${episode.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                episode.duration?.let { dur ->
                    Text(
                        text = dur,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
