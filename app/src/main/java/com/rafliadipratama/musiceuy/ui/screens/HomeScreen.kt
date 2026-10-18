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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rafliadipratama.musiceuy.recognition.RecognitionState
import com.rafliadipratama.musiceuy.recognition.ShazamRecognitionService
import kotlinx.coroutines.launch
import com.rafliadipratama.musiceuy.ui.components.AccountSyncBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.data.model.Album
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.ui.components.SmoothCornerShape
import com.rafliadipratama.musiceuy.ui.components.TrackItem
import com.rafliadipratama.musiceuy.ui.components.press3D
import com.rafliadipratama.musiceuy.ui.components.pressBounce
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.BrandGreenBright
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainer
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

private data class OfficialChart(
    val title: String,
    val subtitle: String,
    val browseId: String,
    val gradientColors: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicPlayerViewModel,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val homeFilterPills = listOf("Semua", "Fokus / Belajar", "Santai & Chill", "Workout", "Pesta", "Pop Indonesia", "Indie", "Rock", "Dangdut")

    val officialCharts = remember {
        listOf(
            OfficialChart("Top 50 Indonesia", "Charts Resmi • YouTube Music", "VLPL4fGSI1pDJn5QPpj0R4vVgRWk8sSq549G", listOf(Color(0xFF007991), Color(0xFF78FFD6))),
            OfficialChart("Top 50 Global", "Charts Global • YouTube Music", "VLPLOHoVaTp8R7dqEEhhaOG7yfDoj_y2BrZ4", listOf(Color(0xFF8A2387), Color(0xFFE94057))),
            OfficialChart("Viral Hits Indonesia", "Trending Viral • YouTube Music", "VLOLAK5uy_n7Ig_LAUbKE6_ZeQ1pwHmJcEhwX7BekBo", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
            OfficialChart("Rilisan Baru 2026", "Rilis Terbaru • YouTube Music", "VLPL4fGSI1pDJn4lW6MJA2q37CfnraqAizqE", listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
        )
    }

    var isAccountSyncVisible by remember { mutableStateOf(false) }
    var isShazamDialogVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: MusicEuy Brand Logo & Action Buttons (Shazam, Sync, Profile)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Shazam In-App Music Recognition Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .clickable { isShazamDialogVisible = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Kenali Lagu",
                            tint = Color(0xFF00C9FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 2. Apple Style Account & Sync Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(SurfaceContainerHigh)
                            .clickable { isAccountSyncVisible = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Akun",
                            tint = BrandGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Akun",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3. Dedicated User Profile Avatar Button (Musicfy Style)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandGreen, Color(0xFF00C9FF))
                                )
                            )
                            .clickable { viewModel.openProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.userProfile.name.take(1).uppercase(),
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Home Quick Filter Pills (Hanya saat online)
            if (uiState.isOnline) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(homeFilterPills) { pill ->
                        val isSelected = uiState.selectedHomeFilter == pill
                        Box(
                            modifier = Modifier
                                .clip(SmoothCornerShape(20.dp))
                                .background(if (isSelected) BrandGreen else SurfaceContainerHigh)
                                .pressBounce(pressedScale = 0.92f)
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
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Pull to Refresh Box Wrapping Home Content
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshingHome,
                onRefresh = { viewModel.refreshHomeContent() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 200.dp)
                ) {
                    // Error Message
                    if (uiState.isOnline) {
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
                    }

                    if (!uiState.isOnline) {
                        // ==================== OFFLINE MODE LAYOUT ====================
                        // 1. Offline Mode Banner Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                                border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFB74D).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WifiOff,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(id = R.string.offline_mode_title),
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(id = R.string.offline_mode_desc),
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Downloaded Songs directly playable
                        if (uiState.downloadedTracks.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader(
                                        title = stringResource(id = R.string.offline_downloaded_section_title),
                                        icon = Icons.Default.DownloadDone
                                    )
                                    Button(
                                        onClick = { viewModel.playAllDownloadedTracks() },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF0C0E11),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(id = R.string.offline_play_all),
                                            color = Color(0xFF0C0E11),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            items(uiState.downloadedTracks, key = { "offline_home_${it.id}" }) { track ->
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

                            item {
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                        } else {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(SurfaceContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
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
                        }
                    } else {
                        // ==================== ONLINE YOUTUBE MUSIC LAYOUT ====================
                        // Hero Carousel Editorial Banner (Musicfy Style)
                        item {
                            HeroCarouselCard(
                                title = "Top Hits 2026 Lossless",
                                subtitle = "Koleksi Terpopuler Audio Fidelitas Tinggi",
                                gradient = listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
                                onPlayClick = {
                                    if (officialCharts.isNotEmpty()) {
                                        val chart = officialCharts[0]
                                        viewModel.openOfficialChart(chart.title, chart.browseId, chart.subtitle)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 0. Tangga Lagu & Charts Resmi
                        item {
                            SectionHeader(title = stringResource(id = R.string.home_top_50), icon = Icons.Default.Leaderboard)
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
                                            .clip(SmoothCornerShape(16.dp))
                                            .background(Brush.linearGradient(chart.gradientColors))
                                            .press3D(maxTilt = 6f, pressedScale = 0.96f)
                                            .clickable { viewModel.openOfficialChart(chart.title, chart.browseId, chart.subtitle) }
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
                                                    .background(Color.Black.copy(alpha = 0.45f))
                                                    .pressBounce(pressedScale = 0.88f),
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
                    }

                    // 1. Putar Cepat / Favoritmu (Jika ada history dengar, aktif online maupun offline)
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

                    // Online-Only Sections
                    if (uiState.isOnline) {
                        // 2. Artis Populer Untukmu (Carousel Avatar Lingkaran Musisi Top)
                        if (uiState.popularArtists.isNotEmpty()) {
                            item {
                                SectionHeader(title = stringResource(id = R.string.home_popular_artists), icon = Icons.Default.Person)
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
                                SectionHeader(title = stringResource(id = R.string.home_albums_singles), icon = Icons.Default.Album)
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

        if (isAccountSyncVisible) {
            AccountSyncBottomSheet(
                viewModel = viewModel,
                onDismiss = { isAccountSyncVisible = false }
            )
        }

        if (isShazamDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    isShazamDialogVisible = false
                    ShazamRecognitionService.reset()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF00C9FF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengenal Musik Shazam", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    val recogState by ShazamRecognitionService.state.collectAsState()
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            scope.launch {
                                ShazamRecognitionService.startListening(context) { recognized ->
                                    recognized?.let { track ->
                                        viewModel.playTrack(track)
                                        isShazamDialogVisible = false
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (ShazamRecognitionService.hasRecordPermission(context)) {
                            ShazamRecognitionService.startListening(context) { recognized ->
                                recognized?.let { track ->
                                    viewModel.playTrack(track)
                                    isShazamDialogVisible = false
                                }
                            }
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (recogState) {
                            is RecognitionState.Listening -> {
                                CircularProgressIndicator(color = Color(0xFF00C9FF), modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Mendengarkan audio sekitar...", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Dekatkan smartphone ke sumber lagu", color = TextMuted, fontSize = 11.sp)
                            }
                            is RecognitionState.Analyzing -> {
                                CircularProgressIndicator(color = BrandGreen, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Mencocokkan sidik jari lagu...", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            is RecognitionState.Success -> {
                                val t = (recogState as RecognitionState.Success).track
                                Text("Ditemukan: ${t.title} • ${t.artist}", color = BrandGreen, fontWeight = FontWeight.Bold)
                            }
                            is RecognitionState.Error -> {
                                val msg = (recogState as RecognitionState.Error).message
                                Text(msg, color = Color(0xFFFF5252), fontSize = 13.sp)
                            }
                            else -> {}
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        isShazamDialogVisible = false
                        ShazamRecognitionService.reset()
                    }) {
                        Text("Tutup", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF161B22)
            )
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
    val initials = remember(artist.name) {
        val words = artist.name.trim().split("\\s+".toRegex())
        when {
            words.size >= 2 -> "${words[0].firstOrNull()?.uppercaseChar() ?: ""}${words[1].firstOrNull()?.uppercaseChar() ?: ""}"
            words.isNotEmpty() && words[0].isNotEmpty() -> words[0].take(2).uppercase()
            else -> "A"
        }
    }

    val avatarGradient = remember(artist.name) {
        val hash = kotlin.math.abs(artist.name.hashCode())
        val gradients = listOf(
            listOf(Color(0xFF007991), Color(0xFF78FFD6)),
            listOf(Color(0xFF8A2387), Color(0xFFE94057)),
            listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
            listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
            listOf(Color(0xFF536976), Color(0xFF292E49)),
            listOf(Color(0xFF1A2980), Color(0xFF26D0CE)),
            listOf(Color(0xFFAA076B), Color(0xFF61045F))
        )
        gradients[hash % gradients.size]
    }

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
                .background(Brush.linearGradient(avatarGradient))
                .border(2.dp, BrandGreen.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Elegant initials fallback behind image
            Text(
                text = initials,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            val effectiveAvatar = remember(artist.name, artist.avatarUrl) {
                artist.avatarUrl?.takeIf { it.isNotBlank() }
                    ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(artist.name)
            }
            // AsyncImage overlays smoothly
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
private fun AlbumSearchCard(
    album: Album,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(SmoothCornerShape(14.dp))
            .background(SurfaceContainerHigh)
            .pressBounce(pressedScale = 0.95f)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .clip(SmoothCornerShape(10.dp))
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
                text = "${album.artistName} • ${if (!album.year.isNullOrBlank() && album.year >= "2025") album.year else "2026"}",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroCarouselCard(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(SmoothCornerShape(20.dp))
            .background(Brush.linearGradient(gradient))
            .press3D(maxTilt = 5f, pressedScale = 0.97f)
            .clickable { onPlayClick() }
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(SmoothCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FEATURED SPOTLIGHT",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(SmoothCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Hi-Res FLAC",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .pressBounce(pressedScale = 0.88f)
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

