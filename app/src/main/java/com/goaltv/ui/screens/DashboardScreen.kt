package com.goaltv.ui.screens

import com.goaltv.R
import com.goaltv.ui.viewmodel.LiveTvViewModel
import com.goaltv.ui.viewmodel.VodViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.goaltv.ui.theme.StatusError
import com.goaltv.ui.theme.SurfaceDark
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.goaltv.ui.components.FocusableCard
import com.goaltv.ui.components.HeaderBar
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangeGlow
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusSuccess
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.viewmodel.AppScreen
import com.goaltv.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    liveTvViewModel: LiveTvViewModel,
    vodViewModel: VodViewModel,
    modifier: Modifier = Modifier
) {
    val isReloading by viewModel.isReloading.collectAsState()
    val isDohEnabled by viewModel.isDohEnabled.collectAsState()
    val connectedUser by viewModel.connectedUser.collectAsState()
    val accountExpiration by viewModel.accountExpiration.collectAsState()
    val lastVisitedScreen by viewModel.lastVisitedScreen.collectAsState()

    val tvFocusRequester = remember { FocusRequester() }
    val cineFocusRequester = remember { FocusRequester() }
    val seriesFocusRequester = remember { FocusRequester() }
    val ajustesFocusRequester = remember { FocusRequester() }
    val actualizarFocusRequester = remember { FocusRequester() }
    val deportesFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            when (lastVisitedScreen) {
                AppScreen.MOVIES -> cineFocusRequester.requestFocus()
                AppScreen.SERIES -> seriesFocusRequester.requestFocus()
                AppScreen.SETTINGS -> ajustesFocusRequester.requestFocus()
                AppScreen.SPORTS -> deportesFocusRequester.requestFocus()
                else -> tvFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var activeDialogTitle by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    BackHandler {
        showExitDialog = true
    }

    if (showSearchDialog) {
        MasterSearchDialog(
            viewModel = viewModel,
            liveTvViewModel = liveTvViewModel,
            vodViewModel = vodViewModel,
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitDialog = false
                (context as? ComponentActivity)?.finish()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isCompactWidth = screenWidth < 600.dp
        val isCompactHeight = screenHeight < 500.dp

        val horizontalPadding = if (isCompactWidth) 14.dp else 28.dp
        val verticalPadding = if (isCompactHeight) 8.dp else 16.dp

        // MAIN DASHBOARD CONTENT AREA
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // TOP HEADER BAR
            HeaderBar(
                isReloading = isReloading,
                isDohEnabled = isDohEnabled,
                onSearchClick = { showSearchDialog = true },
                onReloadClick = { viewModel.reloadPlaylist() },
                onAccountClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                onSettingsClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                onToggleDoh = { viewModel.setDohEnabled(!isDohEnabled) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // CENTER & RIGHT TILES AREA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 8.dp else 14.dp)
            ) {
                // CENTER AREA: 3 TALL VERTICAL POSTER CARDS
                Row(
                    modifier = Modifier
                        .weight(3.2f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 6.dp else 12.dp)
                ) {
                    // Card 1: TV
                    TallPosterCard(
                        title = "TV",
                        subtitle = "",
                        icon = Icons.Rounded.Tv,
                        backgroundGradient = listOf(
                            Color(0xFF0052D4),
                            Color(0xFF4364F7),
                            Color(0xFF1D2671)
                        ),
                        imageUrl = R.drawable.tv,
                        isCompact = isCompactWidth || isCompactHeight,
                        onClick = { viewModel.navigateTo(AppScreen.LIVE_TV) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .focusRequester(tvFocusRequester)
                    )

                    // Card 2: CINE
                    TallPosterCard(
                        title = "CINE",
                        subtitle = "",
                        icon = Icons.Rounded.Movie,
                        backgroundGradient = listOf(
                            Color(0xFF8E24AA),
                            Color(0xFF6A1B9A),
                            Color(0xFF4A148C)
                        ),
                        imageUrl = R.drawable.cine,
                        isCompact = isCompactWidth || isCompactHeight,
                        onClick = { viewModel.navigateTo(AppScreen.MOVIES) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .focusRequester(cineFocusRequester)
                    )

                    // Card 3: SERIES
                    TallPosterCard(
                        title = "SERIES",
                        subtitle = "",
                        icon = Icons.Rounded.VideoLibrary,
                        backgroundGradient = listOf(
                            Color(0xFFFF3300),
                            Color(0xFFE52D27),
                            Color(0xFFB31217)
                        ),
                        imageUrl = R.drawable.series,
                        isCompact = isCompactWidth || isCompactHeight,
                        onClick = { viewModel.navigateTo(AppScreen.SERIES) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .focusRequester(seriesFocusRequester)
                    )
                }

                // RIGHT COLUMN TILES: 3 STACKED VIOLET TILES (PROGRAMACION DEPORTIVA, ACTUALIZAR LISTA & AJUSTES)
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactWidth) 4.dp else 8.dp)
                ) {
                    // Tile 1: PROGRAMACION DEPORTIVA
                    StackedVioletTile(
                        title = "PROGRAMACION DEPORTIVA",
                        subtitle = "",
                        icon = Icons.Rounded.EmojiEvents,
                        isCompact = isCompactWidth || isCompactHeight,
                        onClick = { viewModel.navigateTo(AppScreen.SPORTS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(deportesFocusRequester)
                    )

                    // Tile 2: CATCH-UP
                    StackedVioletTile(
                        title = "CATCH-UP",
                        subtitle = "Grabaciones TV",
                        icon = Icons.Rounded.History,
                        isCompact = isCompactWidth || isCompactHeight,
                        onClick = { viewModel.navigateTo(AppScreen.CATCH_UP) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(actualizarFocusRequester)
                    )

                    // Tile 3: AJUSTES
                    StackedVioletTile(
                        title = "AJUSTES",
                        subtitle = "",
                        icon = Icons.Rounded.Settings,
                        isCompact = isCompactWidth || isCompactHeight,
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(ajustesFocusRequester)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // BOTTOM FOOTER STATUS BAR
            BottomFooterStatusBar(
                expirationDate = accountExpiration,
                connectedUser = connectedUser,
                isCompact = isCompactWidth
            )
        }
    }

    // Master Search Dialog
    if (showSearchDialog) {
        MasterSearchDialog(
            viewModel = viewModel,
            liveTvViewModel = liveTvViewModel,
            vodViewModel = vodViewModel,
            onDismiss = { showSearchDialog = false }
        )
    }

    // Generic Info Dialog
    activeDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { activeDialogTitle = null },
            confirmButton = {
                TextButton(onClick = { activeDialogTitle = null }) {
                    Text("Entendido", color = OrangePrimary)
                }
            },
            title = {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "La función '$title' está activa en esta versión. Explore sus canales y contenido en el menú principal.",
                    color = TextMuted
                )
            },
            containerColor = SurfaceDarkCard
        )
    }
}

/**
 * Center Area: Tall Vertical Poster Card with Rich Gradients & Responsive Sizing
 */
@Composable
fun TallPosterCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundGradient: List<Color>,
    imageUrl: Any? = null,
    isCompact: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(if (isCompact) 12.dp else 18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(backgroundGradient))
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Main Card Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompact) 6.dp else 12.dp, vertical = if (isCompact) 6.dp else 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Center Icon with Circular Glow
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 42.dp else 56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(if (isCompact) 24.dp else 32.dp)
                    )
                }

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = if (isCompact) 10.dp else 16.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (isCompact) 13.sp else 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = if (isCompact) 9.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Right Column: Stacked Violet/Purple Tiles
 */
@Composable
fun StackedVioletTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isCompact: Boolean = false,
    isAnimating: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "stacked_tile_rotation"
    )

    FocusableCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF311B92),
                            Color(0xFF512DA8),
                            Color(0xFF673AB7)
                        )
                    )
                )
                .padding(horizontal = if (isCompact) 8.dp else 14.dp, vertical = if (isCompact) 6.dp else 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 32.dp else 42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier
                            .size(if (isCompact) 18.dp else 24.dp)
                            .rotate(rotation)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (isCompact) 12.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = if (isCompact) 9.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Footer Status Bar
 */
@Composable
fun BottomFooterStatusBar(
    expirationDate: String,
    connectedUser: String,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0E111B))
            .padding(horizontal = if (isCompact) 8.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPIRACIÓN: $expirationDate",
                color = OrangeGlow,
                fontSize = if (isCompact) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Conectado: $connectedUser",
                color = StatusSuccess,
                fontSize = if (isCompact) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Master Search Dialog ("Búsqueda maestra")
 */
@Composable
fun MasterSearchDialog(
    viewModel: MainViewModel,
    liveTvViewModel: LiveTvViewModel,
    vodViewModel: VodViewModel,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(550.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDarkCard
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Búsqueda Maestra",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input field
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.updateSearchQuery(it)
                    },
                    placeholder = { Text("Buscar", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Results list
                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (query.isBlank()) {
                        item {
                            Text(
                                text = "Ingrese un término para buscar en TV, CINE y SERIES.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    } else if (searchResults.channels.isEmpty() && searchResults.movies.isEmpty() && searchResults.series.isEmpty()) {
                        item {
                            Text(
                                text = "No se encontraron resultados para '$query'",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    } else {
                        // Channels
                        items(searchResults.channels) { ch ->
                            SearchResultRow(
                                title = ch.name,
                                category = "TV",
                                icon = Icons.Rounded.Tv,
                                onClick = {
                                    liveTvViewModel.selectChannel(ch)
                                    viewModel.navigateTo(AppScreen.LIVE_TV)
                                    onDismiss()
                                }
                            )
                        }

                        // Movies
                        items(searchResults.movies) { movie ->
                            SearchResultRow(
                                title = movie.name,
                                category = "CINE",
                                icon = Icons.Rounded.Movie,
                                onClick = {
                                    vodViewModel.selectMovie(movie)
                                    viewModel.navigateTo(AppScreen.MOVIES)
                                    onDismiss()
                                }
                            )
                        }

                        // Series
                        items(searchResults.series) { series ->
                            SearchResultRow(
                                title = series.name,
                                category = "SERIES",
                                icon = Icons.Rounded.VideoLibrary,
                                onClick = {
                                    vodViewModel.selectSeries(series)
                                    viewModel.navigateTo(AppScreen.SERIES)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    title: String,
    category: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2235))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = category,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val salirFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            salirFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text("Salir de GoalTV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Text("¿Está seguro de que desea salir de la aplicación?", color = TextMuted, fontSize = 14.sp)
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SALIR on the LEFT side with default focus
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.focusRequester(salirFocusRequester),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SALIR", fontWeight = FontWeight.Bold, color = Color.White)
                }

                // CANCELAR on the RIGHT side
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A40)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CANCELAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null
    )
}
