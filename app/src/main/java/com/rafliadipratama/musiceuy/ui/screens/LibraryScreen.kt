package com.rafliadipratama.musiceuy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.ui.components.TrackItem
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainer
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

private data class PopularGenre(
    val title: String,
    val query: String,
    val gradientColors: List<Color>
)

@Composable
fun LibraryScreen(
    viewModel: MusicPlayerViewModel,
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val curatedFavorites = remember { viewModel.streamSource.getCuratedIndonesianTracks() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    val popularGenres = remember {
        listOf(
            PopularGenre("Pop Indonesia", "Pop Indonesia Viral", listOf(Color(0xFFE91E63), Color(0xFFFF6090))),
            PopularGenre("Indie Indo", "Indie Pop Indonesia", listOf(Color(0xFF7E57C2), Color(0xFFB39DDB))),
            PopularGenre("Koplo & Dangdut", "Dangdut Koplo Indonesia", listOf(Color(0xFFFF9800), Color(0xFFFFB74D))),
            PopularGenre("Rock 2000-an", "Rock Indonesia 2000an", listOf(Color(0xFFD32F2F), Color(0xFFEF5350))),
            PopularGenre("Akustik Senja", "Akustik Indonesia Senja Santai", listOf(Color(0xFF009688), Color(0xFF4DB6AC))),
            PopularGenre("Lo-Fi Chill", "Lofi Hip Hop Chill Beats", listOf(Color(0xFF3F51B5), Color(0xFF7986CB)))
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    text = "Buat Playlist Baru",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Nama playlist...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        placeholder = { Text("Deskripsi (opsional)...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim(), newPlaylistDesc.trim())
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("Buat", color = Color(0xFF0C0E11), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Koleksi Saya",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Playlist, genre, lagu favorit & riwayat",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Tiles: Lagu Disukai & Sering Diputar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Lagu Disukai Tile
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE91E63).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Lagu Disukai",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${curatedFavorites.size} lagu",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Sering Diputar Tile
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.showMostPlayed()
                                onNavigateToHome()
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Sering Diputar",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.mostPlayedTracks.isNotEmpty())
                                    "${uiState.mostPlayedTracks.size} lagu sering"
                                else
                                    "Otomatis tersimpan",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ==================== Playlist Section ====================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Playlist Kamu",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "+ Buat Baru",
                        color = BrandGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick "+ Buat Playlist" Card
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                            modifier = Modifier
                                .size(width = 120.dp, height = 130.dp)
                                .clickable { showCreateDialog = true }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Buat",
                                        tint = BrandGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Buat Playlist",
                                    color = BrandGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Existing Playlists
                    items(uiState.playlists, key = { it.id }) { playlist ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                            modifier = Modifier
                                .size(width = 120.dp, height = 130.dp)
                                .clickable { viewModel.openPlaylist(playlist) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!playlist.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = playlist.coverUrl,
                                        contentDescription = playlist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = null,
                                            tint = BrandGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = playlist.name,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${playlist.tracks.size} lagu",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==================== Genre Explorer Section ====================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Jelajahi Genre Populer",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2x3 Grid for Genres
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in popularGenres.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Column 1
                            val g1 = popularGenres[i]
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clickable {
                                        viewModel.search(g1.query)
                                        onNavigateToHome()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.horizontalGradient(g1.gradientColors))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = g1.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Column 2
                            if (i + 1 < popularGenres.size) {
                                val g2 = popularGenres[i + 1]
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(62.dp)
                                        .clickable {
                                            viewModel.search(g2.query)
                                            onNavigateToHome()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.horizontalGradient(g2.gradientColors))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = g2.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== Curated Indonesian Tracks ====================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lagu Indonesia Pilihan",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(curatedFavorites, key = { it.id }) { track ->
                val isActive = uiState.currentTrack?.id == track.id
                TrackItem(
                    track = track,
                    isPlaying = uiState.isPlaying,
                    isActive = isActive,
                    onClick = {
                        viewModel.playTrack(track, curatedFavorites)
                    },
                    onArtistClick = { artist ->
                        viewModel.openArtistDetail(artist)
                    },
                    onAddToPlaylistClick = {
                        viewModel.showAddToPlaylist(track)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(140.dp))
            }
        }
    }
}
