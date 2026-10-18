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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.rafliadipratama.musiceuy.R
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.data.model.Album
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.ui.components.TrackItem
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.BrandGreenBright
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainer
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerLow
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

private data class ExploreGenre(
    val title: String,
    val query: String,
    val gradientColors: List<Color>
)

@Composable
fun SearchScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchCategoryPills = listOf("Semua", "Lagu", "Artis", "Album")

    val trendingKeywords = remember {
        listOf(
            "Bernadya", "Sheila On 7", "Hindia", "Denny Caknan",
            "Tulus", "Mahalini", "Coldplay", "NIKI", "Sal Priadi"
        )
    }

    val exploreGenres = remember {
        listOf(
            ExploreGenre("Pop Indonesia", "Pop Indonesia Hits", listOf(Color(0xFFE91E63), Color(0xFFFF6090))),
            ExploreGenre("Indie Senja", "Indie Indonesia", listOf(Color(0xFF7E57C2), Color(0xFFB39DDB))),
            ExploreGenre("Dangdut Koplo", "Dangdut Koplo Viral", listOf(Color(0xFFFF9800), Color(0xFFFFB74D))),
            ExploreGenre("Rock 2000-an", "Rock Indonesia 2000an", listOf(Color(0xFFD32F2F), Color(0xFFEF5350))),
            ExploreGenre("Akustik Santai", "Akustik Indonesia", listOf(Color(0xFF009688), Color(0xFF4DB6AC))),
            ExploreGenre("Lo-Fi Chill", "Lofi Hip Hop Beats", listOf(Color(0xFF3F51B5), Color(0xFF7986CB)))
        )
    }

    // Trigger search with small debounce when typing or when category changes
    LaunchedEffect(searchInput) {
        if (searchInput.isNotBlank()) {
            kotlinx.coroutines.delay(400)
            viewModel.search(searchInput.trim())
        }
    }

    val isSearchActive = searchInput.isNotBlank() || uiState.searchResults.isNotEmpty() ||
            uiState.searchResultArtists.isNotEmpty() || uiState.searchResultAlbums.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eksplorasi & Pencarian",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }

            // Search Bar Input (Ergonomic, High contrast, clear button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = {
                        Text(
                            text = if (uiState.isOnline) stringResource(id = R.string.search_placeholder) else stringResource(id = R.string.offline_search_hint),
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.nav_search),
                            tint = if (isSearchActive) BrandGreen else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchInput.isNotBlank()) {
                            IconButton(onClick = {
                                searchInput = ""
                                viewModel.loadTrending()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = SurfaceContainerHigh,
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedContainerColor = SurfaceContainerLow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            if (searchInput.isNotBlank()) {
                                viewModel.search(searchInput.trim())
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!uiState.isOnline) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFB74D).copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.offline_search_banner),
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Pills (Hanya saat online)
            if (uiState.isOnline) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp),
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main Content: Exploration mode OR Search results mode
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 220.dp)
            ) {
                // Loading Indicator
                if (uiState.isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
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

                if (!isSearchActive) {
                    if (!uiState.isOnline) {
                        // =========================================================
                        // OFFLINE READY-TO-PLAY SONGS
                        // =========================================================
                        if (uiState.downloadedTracks.isNotEmpty()) {
                            item {
                                SearchSectionHeader(
                                    title = stringResource(id = R.string.offline_downloaded_section_title),
                                    icon = Icons.Default.DownloadDone
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            items(uiState.downloadedTracks, key = { "offline_search_${it.id}" }) { track ->
                                val isLiked = uiState.likedTrackIds.contains(track.id)
                                val activeTrack = track.copy(isLiked = isLiked)
                                val isActive = uiState.currentTrack?.id == track.id
                                TrackItem(
                                    track = activeTrack,
                                    isPlaying = uiState.isPlaying,
                                    isActive = isActive,
                                    onClick = {
                                        viewModel.playTrack(activeTrack, uiState.downloadedTracks)
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
                        } else {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(id = R.string.offline_no_songs_title),
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.offline_no_songs_desc),
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // =========================================================
                        // EXPLORATION & DISCOVERY STATE (Query is blank & Online)
                        // =========================================================

                    // 0. Riwayat Pencarian Terakhir (Search History)
                    if (uiState.searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.search_history_title),
                                        color = TextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = stringResource(id = R.string.search_clear_all),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { viewModel.clearSearchHistory() }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.searchHistory, key = { "hist_$it" }) { historyItem ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(SurfaceContainerHigh)
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                                            .clickable {
                                                searchInput = historyItem
                                                viewModel.search(historyItem)
                                            }
                                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = historyItem,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.removeSearchHistory(historyItem) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus",
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // 1. Paling Sering Dicari / Trending Searches
                    item {
                        SearchSectionHeader(title = "Paling Sering Dicari", icon = Icons.AutoMirrored.Filled.TrendingUp)
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(trendingKeywords) { keyword ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(SurfaceContainerHigh)
                                        .border(1.dp, BrandGreen.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                                        .clickable {
                                            searchInput = keyword
                                            viewModel.search(keyword)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = keyword,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // 2. Eksplorasi Genre & Mood
                    item {
                        SearchSectionHeader(title = "Jelajahi Berdasarkan Genre", icon = Icons.Default.Explore)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 2-column genre grid cards
                    items(exploreGenres.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (genre in pair) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(95.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(genre.gradientColors))
                                        .clickable {
                                            searchInput = genre.query
                                            viewModel.search(genre.query)
                                        }
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = genre.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.align(Alignment.TopStart)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // =========================================================
                // SEARCH RESULTS MODE
                    // =========================================================

                    // 1. Musisi / Artis Hasil Pencarian
                    if (uiState.searchCategory == "Semua" || uiState.searchCategory == "Artis") {
                        if (uiState.searchResultArtists.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Artis Terkait", icon = Icons.Default.Person)
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.searchResultArtists, key = { it.id }) { artist ->
                                        ArtistSearchItemCard(
                                            artist = artist,
                                            onClick = { viewModel.openArtistDetail(artist) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    // 2. Album Hasil Pencarian
                    if (uiState.searchCategory == "Semua" || uiState.searchCategory == "Album") {
                        if (uiState.searchResultAlbums.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Album & Single", icon = Icons.Default.Album)
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.searchResultAlbums, key = { it.id }) { album ->
                                        AlbumSearchItemCard(
                                            album = album,
                                            onClick = { viewModel.openAlbum(album) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    // 3. Lagu Hasil Pencarian
                    if (uiState.searchCategory == "Semua" || uiState.searchCategory == "Lagu") {
                        if (uiState.searchResults.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Lagu", icon = Icons.Default.MusicNote)
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
                    }

                    // Empty results state
                    if (!uiState.isSearching && uiState.searchResults.isEmpty() &&
                        uiState.searchResultArtists.isEmpty() && uiState.searchResultAlbums.isEmpty()
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Lagu atau artis tidak ditemukan",
                                        color = TextSecondary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Coba gunakan kata kunci lain atau periksa ejaanmu",
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
}

@Composable
private fun SearchSectionHeader(
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
private fun ArtistSearchItemCard(
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(BrandGreen.copy(alpha = 0.25f), SurfaceContainerHigh)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artist.name.take(2).uppercase(),
                    color = BrandGreenBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val effectiveAvatar = artist.avatarUrl?.takeIf { it.isNotBlank() }
                ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(artist.name)
            if (!effectiveAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = effectiveAvatar,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
private fun AlbumSearchItemCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHigh)
        ) {
            AsyncImage(
                model = album.coverUrl,
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artistName,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
