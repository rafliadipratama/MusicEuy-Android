package com.rafliadipratama.musiceuy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.data.model.Album
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.ui.components.PlayingIndicator
import com.rafliadipratama.musiceuy.ui.components.SmoothCornerShape
import com.rafliadipratama.musiceuy.ui.components.TrackItem
import com.rafliadipratama.musiceuy.ui.components.pressBounce
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

    val searchCategoryPills = listOf("Semua", "Lagu", "Video", "Artis", "Album")

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
            ExploreGenre("Lo-Fi Chill", "Lofi Hip Hop Beats", listOf(Color(0xFF3F51B5), Color(0xFF7986CB))),
            ExploreGenre("Hip Hop & R&B", "Hip Hop Indonesia", listOf(Color(0xFF1A237E), Color(0xFF00BCD4))),
            ExploreGenre("Nostalgia 90-an", "Lagu Nostalgia 90an", listOf(Color(0xFF5D4037), Color(0xFF8D6E63)))
        )
    }

    // Debounced search when user types
    LaunchedEffect(searchInput) {
        if (searchInput.isNotBlank()) {
            kotlinx.coroutines.delay(350)
            viewModel.search(searchInput.trim())
        }
    }

    val isSearchActive = searchInput.isNotBlank() || uiState.searchResults.isNotEmpty() ||
            uiState.searchResultArtists.isNotEmpty() || uiState.searchResultAlbums.isNotEmpty() ||
            uiState.searchResultVideos.isNotEmpty()

    // Determine Top Result Track
    val topResultTrack = remember(uiState.searchResults, searchInput) {
        uiState.searchResults.firstOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pencarian",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.6).sp
                )
            }

            // High-Performance Apple Music / Spotify Search Bar
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
                            fontSize = 14.5.sp
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
                            IconButton(
                                onClick = {
                                    searchInput = ""
                                    viewModel.loadTrending()
                                },
                                modifier = Modifier.pressBounce(pressedScale = 0.85f)
                            ) {
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
                    shape = SmoothCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = SurfaceContainerHigh,
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

            // Offline Indicator Banner if no internet
            if (!uiState.isOnline) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(SmoothCornerShape(10.dp))
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

            // Category Filter Pills
            if (isSearchActive) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchCategoryPills) { category ->
                        val isSelected = uiState.searchCategory == category
                        Box(
                            modifier = Modifier
                                .clip(SmoothCornerShape(20.dp))
                                .background(
                                    if (isSelected) BrandGreen else SurfaceContainerHigh
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BrandGreen else Color.White.copy(alpha = 0.06f),
                                    shape = SmoothCornerShape(20.dp)
                                )
                                .pressBounce(pressedScale = 0.92f)
                                .clickable { viewModel.setSearchCategory(category) }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 110.dp, start = 18.dp, end = 18.dp)
            ) {
                // Loading Indicator
                if (uiState.isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = BrandGreen,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // =========================================================================
                // 1. DISCOVERY STATE (Query is Blank)
                // =========================================================================
                if (!isSearchActive) {
                    // Search History
                    if (uiState.searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pencarian Terakhir",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Hapus Semua",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { viewModel.clearSearchHistory() }
                                        .padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.searchHistory) { historyItem ->
                                    Row(
                                        modifier = Modifier
                                            .clip(SmoothCornerShape(20.dp))
                                            .background(SurfaceContainerHigh)
                                            .border(1.dp, Color.White.copy(alpha = 0.06f), SmoothCornerShape(20.dp))
                                            .pressBounce(pressedScale = 0.94f)
                                            .clickable {
                                                searchInput = historyItem
                                                viewModel.search(historyItem)
                                            }
                                            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = historyItem,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus item",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeSearchHistory(historyItem) }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // Trending Keywords
                    item {
                        SearchSectionHeader(
                            title = "Sedang Tren",
                            icon = Icons.AutoMirrored.Filled.TrendingUp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(trendingKeywords) { keyword ->
                                Box(
                                    modifier = Modifier
                                        .clip(SmoothCornerShape(20.dp))
                                        .background(SurfaceContainerLow)
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), SmoothCornerShape(20.dp))
                                        .pressBounce(pressedScale = 0.92f)
                                        .clickable {
                                            searchInput = keyword
                                            viewModel.search(keyword)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = keyword,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Browse Categories / Genres
                    item {
                        SearchSectionHeader(
                            title = "Jelajahi Kategori",
                            icon = Icons.Default.MusicNote
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 2-Column Grid for Genres
                    val chunkedGenres = exploreGenres.chunked(2)
                    items(chunkedGenres) { rowGenres ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (genre in rowGenres) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(84.dp)
                                        .clip(SmoothCornerShape(14.dp))
                                        .background(Brush.linearGradient(genre.gradientColors))
                                        .pressBounce(pressedScale = 0.95f)
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

                // =========================================================================
                // 2. SEARCH RESULTS STATE (Query entered)
                // =========================================================================
                if (isSearchActive) {
                    // TOP RESULT CARD (Apple Music / Spotify Style)
                    if (topResultTrack != null && (uiState.searchCategory == "Semua" || uiState.searchCategory == "Lagu")) {
                        item {
                            SearchSectionHeader(title = "Hasil Terbaik")
                            Spacer(modifier = Modifier.height(8.dp))
                            TopResultCard(
                                track = topResultTrack,
                                isActive = uiState.currentTrack?.id == topResultTrack.id,
                                isPlaying = uiState.isPlaying,
                                onClick = {
                                    if (uiState.currentTrack?.id == topResultTrack.id) {
                                        viewModel.togglePlayPause()
                                    } else {
                                        viewModel.playTrack(topResultTrack, uiState.searchResults)
                                    }
                                },
                                onLongClick = {
                                    viewModel.showTrackContextMenu(topResultTrack)
                                }
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                        }
                    }

                    // ARTISTS RESULTS
                    if (uiState.searchCategory == "Semua" || uiState.searchCategory == "Artis") {
                        if (uiState.searchResultArtists.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Artis Terkait", icon = Icons.Default.Person)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.searchResultArtists, key = { it.id }) { artist ->
                                        ArtistSearchItemCard(
                                            artist = artist,
                                            onClick = { viewModel.openArtistDetail(artist) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // ALBUMS RESULTS
                    if (uiState.searchCategory == "Semua" || uiState.searchCategory == "Album") {
                        if (uiState.searchResultAlbums.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Album & Single", icon = Icons.Default.Album)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.searchResultAlbums, key = { it.id }) { album ->
                                        AlbumSearchItemCard(
                                            album = album,
                                            onClick = { viewModel.openAlbum(album) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // VIDEO RESULTS (When in "Semua")
                    if (uiState.searchCategory == "Semua") {
                        if (uiState.searchResultVideos.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Video", icon = Icons.Default.PlayArrow)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.searchResultVideos, key = { "search_vid_${it.id}" }) { video ->
                                        val isActive = uiState.currentTrack?.id == video.id
                                        SearchVideoCard(
                                            track = video,
                                            isActive = isActive,
                                            isPlaying = uiState.isPlaying,
                                            onClick = { viewModel.playTrack(video, uiState.searchResultVideos) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // EXCLUSIVE VIDEO TAB
                    if (uiState.searchCategory == "Video") {
                        if (uiState.searchResultVideos.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Video", icon = Icons.Default.PlayArrow)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(uiState.searchResultVideos, key = { "tab_vid_${it.id}" }) { video ->
                                val isActive = uiState.currentTrack?.id == video.id
                                SearchVideoRow(
                                    track = video,
                                    isActive = isActive,
                                    isPlaying = uiState.isPlaying,
                                    onClick = { viewModel.playTrack(video, uiState.searchResultVideos) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // SONGS LIST
                    if (uiState.searchCategory == "Semua" || uiState.searchCategory == "Lagu") {
                        // Skip the first one if already shown in Top Result Card when in "Semua"
                        val songRows = if (uiState.searchCategory == "Semua" && topResultTrack != null) {
                            uiState.searchResults.filter { it.id != topResultTrack.id }
                        } else {
                            uiState.searchResults
                        }

                        if (songRows.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Lagu", icon = Icons.Default.MusicNote)
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            items(songRows, key = { it.id }) { track ->
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

                    // EMPTY STATE
                    if (!uiState.isSearching && uiState.searchResults.isEmpty() &&
                        uiState.searchResultArtists.isEmpty() && uiState.searchResultAlbums.isEmpty() &&
                        uiState.searchResultVideos.isEmpty()
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
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Tidak ada hasil untuk '$searchInput'",
                                        color = TextSecondary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Periksa ejaan kata kunci atau coba artis lain",
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

/**
 * Apple Music & Spotify inspired Top Result Card.
 * Prominently presents the highest-matching song with blurred ambient artwork
 * and an instant white circular Play Button.
 */
@Composable
private fun TopResultCard(
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmoothCornerShape(18.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color.White.copy(alpha = 0.08f), SmoothCornerShape(18.dp))
            .pressBounce(pressedScale = 0.98f)
            .clickable { onClick() }
    ) {
        // Blurred ambient backdrop
        if (track.coverUrl.isNotBlank()) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.25f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color(0xFF101012).copy(alpha = 0.92f)
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork with squircle curvature
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(SmoothCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${track.fullArtistDisplay} ${if (track.durationFormatted.isNotBlank()) "• ${track.durationFormatted}" else ""}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Big Circular Play Button (Apple Music style)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pressBounce(pressedScale = 0.88f)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Putar",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
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
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
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
            .pressBounce(pressedScale = 0.94f)
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
            .pressBounce(pressedScale = 0.94f)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(SmoothCornerShape(14.dp))
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

@Composable
private fun SearchVideoCard(
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val videoId = track.id.removePrefix("yt:")
    val cardWidth = 260.dp
    val cardHeight = 146.dp // 16:9 ratio
    var useFallback by remember(videoId) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .clip(SmoothCornerShape(14.dp))
            .background(SurfaceContainerHigh)
            .pressBounce(pressedScale = 0.95f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = if (useFallback) track.coverUrl else "https://i.ytimg.com/vi/$videoId/hq720.jpg",
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            onError = { if (!useFallback) useFallback = true },
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Top Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE50914).copy(alpha = 0.90f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "VIDEO",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (track.durationFormatted.isNotBlank() && track.durationFormatted != "0:00") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.durationFormatted,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Center playing indicator if active
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.60f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = BrandGreenBright,
                        isPlaying = true,
                        bars = 4,
                        barWidth = 2.dp,
                        maxHeight = 14.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Title & artist info at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = track.cleanTitle,
                color = if (isActive) BrandGreenBright else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.fullArtistDisplay,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchVideoRow(
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val videoId = track.id.removePrefix("yt:")
    var useFallback by remember(videoId) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) SurfaceContainerHigh else SurfaceContainer.copy(alpha = 0.45f))
            .pressBounce(pressedScale = 0.98f)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(118.dp)
                .height(66.dp) // 16:9 ratio
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainer)
        ) {
            AsyncImage(
                model = if (useFallback) track.coverUrl else "https://i.ytimg.com/vi/$videoId/hq720.jpg",
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                onError = { if (!useFallback) useFallback = true },
                modifier = Modifier.fillMaxSize()
            )

            // Duration badge at bottom right
            if (track.durationFormatted.isNotBlank() && track.durationFormatted != "0:00") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = track.durationFormatted,
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = BrandGreenBright,
                            isPlaying = true,
                            bars = 3,
                            barWidth = 2.dp,
                            maxHeight = 14.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.cleanTitle,
                color = if (isActive) BrandGreen else TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = track.fullArtistDisplay,
                color = TextSecondary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
