package com.rafliadipratama.musiceuy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.data.model.Album
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.ui.components.TrackItem
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainer
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerLow
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.theme.VerifiedBadge
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

private data class OfficialChart(
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>
)

@Composable
fun HomeScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val homeFilterPills = listOf("Semua", "Fokus / Belajar", "Santai & Chill", "Workout", "Pesta", "Pop Indonesia", "Indie", "Rock", "Dangdut")
    val searchCategoryPills = listOf("Semua", "Lagu", "Artis", "Album")

    val officialCharts = remember {
        listOf(
            OfficialChart("Top 50 Indonesia", "Hits terpopuler Tanah Air", listOf(Color(0xFF007991), Color(0xFF78FFD6))),
            OfficialChart("Top 50 Global", "Hits teratas seluruh dunia", listOf(Color(0xFF8A2387), Color(0xFFE94057))),
            OfficialChart("Viral Hits Indonesia", "Tren viral TikTok & medsos", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
            OfficialChart("Rilisan Baru", "Koleksi musik rilisan terbaru", listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
        )
    }

    val isSearchMode = searchInput.isNotBlank() || uiState.searchResults.isNotEmpty() ||
            uiState.searchResultArtists.isNotEmpty() || uiState.searchResultAlbums.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: MusicEuy Brand Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_musiceuy_logo),
                        contentDescription = "MusicEuy Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MusicEuy",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text("Cari lagu, artis, atau album...", color = TextMuted, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            viewModel.loadTrending()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainer,
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedBorderColor = BrandGreen,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    viewModel.search(searchInput)
                })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            if (isSearchMode) {
                // Category Filter Pills (Semua, Lagu, Artis, Album)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchCategoryPills) { cat ->
                        val isSelected = uiState.searchCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) BrandGreen else SurfaceContainerHigh)
                                .clickable { viewModel.setSearchCategory(cat) }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color(0xFF0C0E11) else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Home Quick Filter Pills
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(homeFilterPills) { pill ->
                        val isSelected = uiState.selectedHomeFilter == pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) BrandGreen else SurfaceContainerHigh)
                                .clickable {
                                    viewModel.selectHomeFilter(pill)
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = pill,
                                color = if (isSelected) Color(0xFF0C0E11) else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Scrollable Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 200.dp)
            ) {
                // Search Loading State
                if (uiState.isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrandGreen, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                // Error Message
                uiState.error?.let { err ->
                    item {
                        Text(
                            text = err,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }

                // ==================== SEARCH MODE CONTENT ====================
                if (isSearchMode) {
                    val showArtists = (uiState.searchCategory == "Semua" || uiState.searchCategory == "Artis") &&
                            uiState.searchResultArtists.isNotEmpty()
                    val showAlbums = (uiState.searchCategory == "Semua" || uiState.searchCategory == "Album") &&
                            uiState.searchResultAlbums.isNotEmpty()
                    val showTracks = (uiState.searchCategory == "Semua" || uiState.searchCategory == "Lagu") &&
                            uiState.searchResults.isNotEmpty()

                    // Section: Artis Ditemukan
                    if (showArtists) {
                        item {
                            SectionHeader(title = "Artis Terkait", icon = Icons.Default.Person)
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.searchResultArtists, key = { it.id }) { artist ->
                                    ArtistSearchCard(
                                        artist = artist,
                                        onClick = { viewModel.openArtistDetail(artist) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Section: Album Ditemukan
                    if (showAlbums) {
                        item {
                            SectionHeader(title = "Album & Single", icon = Icons.Default.Album)
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.searchResultAlbums, key = { it.id }) { album ->
                                    AlbumSearchCard(
                                        album = album,
                                        onClick = { viewModel.openAlbum(album) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Section: Lagu Ditemukan
                    if (showTracks) {
                        item {
                            SectionHeader(title = "Lagu", icon = Icons.Default.Whatshot)
                        }

                        items(uiState.searchResults, key = { it.id }) { track ->
                            val isLiked = uiState.likedTrackIds.contains(track.id)
                            val activeTrack = track.copy(isLiked = isLiked)
                            val isActive = uiState.currentTrack?.id == track.id
                            TrackItem(
                                track = activeTrack,
                                isPlaying = uiState.isPlaying,
                                isActive = isActive,
                                onClick = {
                                    viewModel.playTrack(activeTrack, uiState.searchResults)
                                },
                                onLongClick = {
                                    viewModel.showTrackContextMenu(activeTrack)
                                },
                                onArtistClick = { artist ->
                                    viewModel.openArtistDetail(artist)
                                },
                                onAddToPlaylistClick = {
                                    viewModel.showAddToPlaylist(activeTrack)
                                },
                                onToggleLikeClick = {
                                    viewModel.toggleLike(activeTrack)
                                }
                            )
                        }
                    }
                } else {
                    // ==================== HOME YOUTUBE MUSIC LAYOUT ====================

                    // 0. Tangga Lagu & Charts Resmi
                    item {
                        SectionHeader(title = "Tangga Lagu & Charts Resmi", icon = Icons.Default.Leaderboard)
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(officialCharts) { chart ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 175.dp, height = 115.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(chart.gradientColors))
                                        .clickable { viewModel.playOfficialChart(chart.title) }
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = chart.title,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = chart.subtitle,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Putar Chart",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // 1. Putar Cepat / Favoritmu (Jika ada history dengar)
                    if (uiState.mostPlayedTracks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Putar Cepat Favoritmu", icon = Icons.AutoMirrored.Filled.TrendingUp)
                        }

                        val quickPicks = uiState.mostPlayedTracks.take(4)
                        items(quickPicks, key = { "quick_${it.id}" }) { track ->
                            val isLiked = uiState.likedTrackIds.contains(track.id)
                            val activeTrack = track.copy(isLiked = isLiked)
                            val isActive = uiState.currentTrack?.id == track.id
                            TrackItem(
                                track = activeTrack,
                                isPlaying = uiState.isPlaying,
                                isActive = isActive,
                                onClick = {
                                    viewModel.playTrack(activeTrack, uiState.mostPlayedTracks)
                                },
                                onLongClick = {
                                    viewModel.showTrackContextMenu(activeTrack)
                                },
                                onArtistClick = { artist ->
                                    viewModel.openArtistDetail(artist)
                                },
                                onAddToPlaylistClick = {
                                    viewModel.showAddToPlaylist(activeTrack)
                                },
                                onToggleLikeClick = {
                                    viewModel.toggleLike(activeTrack)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // 2. Artis Populer Untukmu (Carousel Avatar Lingkaran Musisi Top)
                    if (uiState.popularArtists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Artis Populer Untukmu", icon = Icons.Default.Person)
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.popularArtists, key = { it.id }) { artist ->
                                    ArtistSearchCard(
                                        artist = artist,
                                        onClick = { viewModel.openArtistDetail(artist) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // 3. Album & Single Rilisan Terbaru (Carousel Cover Persegi)
                    if (uiState.newReleases.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Album & Single Terbaru", icon = Icons.Default.Album)
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(uiState.newReleases, key = { it.id }) { album ->
                                    AlbumSearchCard(
                                        album = album,
                                        onClick = { viewModel.openAlbum(album) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // 4. Top Trending di Indonesia (Lagu-lagu Hits Terpopuler)
                    if (uiState.trendingTracks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Top Trending di Indonesia", icon = Icons.Default.Whatshot)
                        }

                        items(uiState.trendingTracks, key = { it.id }) { track ->
                            val isLiked = uiState.likedTrackIds.contains(track.id)
                            val activeTrack = track.copy(isLiked = isLiked)
                            val isActive = uiState.currentTrack?.id == track.id
                            TrackItem(
                                track = activeTrack,
                                isPlaying = uiState.isPlaying,
                                isActive = isActive,
                                onClick = {
                                    viewModel.playTrack(activeTrack, uiState.trendingTracks)
                                },
                                onLongClick = {
                                    viewModel.showTrackContextMenu(activeTrack)
                                },
                                onArtistClick = { artist ->
                                    viewModel.openArtistDetail(artist)
                                },
                                onAddToPlaylistClick = {
                                    viewModel.showAddToPlaylist(activeTrack)
                                },
                                onToggleLikeClick = {
                                    viewModel.toggleLike(activeTrack)
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
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ArtistSearchCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh)
                .border(1.5.dp, BrandGreen.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!artist.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artist.avatarUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist.name,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Artis",
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun AlbumSearchCard(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                if (album.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.artistName}${if (!album.year.isNullOrBlank()) " • ${album.year}" else ""}",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
