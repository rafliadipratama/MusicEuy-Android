package com.rafliadipratama.musiceuy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.rafliadipratama.musiceuy.data.model.Playlist
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
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

private data class OfficialChart(
    val title: String,
    val subtitle: String,
    val browseId: String,
    val gradientColors: List<Color>,
    val coverUrl: String? = null
)

private data class TopCategory(
    val title: String,
    val filterPill: String,
    val icon: ImageVector,
    val gradient: List<Color>
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
            OfficialChart(
                "Top 50 Indonesia",
                "Charts Resmi Indonesia",
                "VLPL4fGSI1pDJn5QPpj0R4vVgRWk8sSq549G",
                listOf(Color(0xFF007991), Color(0xFF78FFD6)),
                "https://yt3.googleusercontent.com/wHZJ1dYK2lbb3ocVJMe1fE_FWf_pZI2ydIOwgSV9YeZcJpYSc8vAHHjt6_o4bRU9InAGeomx0v4n=s576"
            ),
            OfficialChart(
                "Top 50 Global",
                "Charts Global Terpopuler",
                "VLPLOHoVaTp8R7dqEEhhaOG7yfDoj_y2BrZ4",
                listOf(Color(0xFF8A2387), Color(0xFFE94057)),
                "https://yt3.googleusercontent.com/t4PqTAJLy66M235aV912vM8pEKnAC0QM_zfhSomgdnQYYmBDJxoOzh1l0gWA7CRf9r_XyyEn0Go=s576"
            ),
            OfficialChart(
                "Viral Hits Indonesia",
                "Trending Viral Indonesia",
                "VLOLAK5uy_n7Ig_LAUbKE6_ZeQ1pwHmJcEhwX7BekBo",
                listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                "https://yt3.googleusercontent.com/2qHUJ20mtWoAZBU80LpQkNfu7EXSKKNs8K-MFFV5vIuFKDseRNHb1cVbmuzmfHvNJKKZGJ-cJKI=s576"
            ),
            OfficialChart(
                "Rilisan Baru 2026",
                "Rilisan Musik Terbaru",
                "VLPL4fGSI1pDJn4lW6MJA2q37CfnraqAizqE",
                listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                "https://yt3.googleusercontent.com/DF1qlviHbfG6kvfw1FBVtGGP-QHA-vpDs4-JZ-h51QCmPbW6XEzRIzcAkanyTzw9k4hopm62hJk=s576"
            )
        )
    }

    val topCategories = remember {
        listOf(
            TopCategory("Santai & Chill", "Santai & Chill", Icons.Default.SelfImprovement, listOf(Color(0xFF134E5E), Color(0xFF71B280))),
            TopCategory("Fokus & Belajar", "Fokus / Belajar", Icons.Default.School, listOf(Color(0xFF1E3C72), Color(0xFF2A5298))),
            TopCategory("Workout & Energi", "Workout", Icons.Default.FitnessCenter, listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
            TopCategory("Pesta & Dance", "Pesta", Icons.Default.Celebration, listOf(Color(0xFF8A2387), Color(0xFFE94057))),
            TopCategory("Pop Indonesia", "Pop Indonesia", Icons.Default.Headphones, listOf(Color(0xFF007991), Color(0xFF78FFD6))),
            TopCategory("Indie Vibe", "Indie", Icons.Default.Nightlife, listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))),
            TopCategory("Rock Alternatif", "Rock", Icons.Default.Whatshot, listOf(Color(0xFFE65C00), Color(0xFFF9D423))),
            TopCategory("Dangdut Koplo", "Dangdut", Icons.Default.Celebration, listOf(Color(0xFFD31027), Color(0xFFEA384D)))
        )
    }

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

                // Shazam In-App Music Recognition Button (Official Icon)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .pressBounce(pressedScale = 0.90f)
                        .clickable { isShazamDialogVisible = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shazam),
                        contentDescription = "Kenali Lagu",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
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
                        // ==================== ONLINE YOUTUBE MUSIC & MUSICFY STYLE LAYOUT ====================
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
                                    val chartInteractionSource = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(width = 175.dp, height = 118.dp)
                                            .clip(SmoothCornerShape(16.dp))
                                            .background(Brush.linearGradient(chart.gradientColors))
                                            .pressBounce(interactionSource = chartInteractionSource, pressedScale = 0.95f)
                                            .clickable(
                                                interactionSource = chartInteractionSource,
                                                indication = null
                                            ) { viewModel.openOfficialChart(chart.title, chart.browseId, chart.subtitle, chart.coverUrl) }
                                    ) {
                                        // Official Cover Artwork (from Google CDN)
                                        if (!chart.coverUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = chart.coverUrl,
                                                contentDescription = chart.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            // Glassmorphic / Scrim Gradient Overlay for high text contrast
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Black.copy(alpha = 0.35f),
                                                                Color.Black.copy(alpha = 0.85f)
                                                            )
                                                        )
                                                    )
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(14.dp),
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
                                                    .background(Color.Black.copy(alpha = 0.55f))
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

                        // 1. Kategori Teratas (YouTube Music Style)
                        item {
                            SectionHeader(title = "Kategori Teratas", icon = Icons.Default.Category)
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(topCategories) { category ->
                                    val isSelected = uiState.selectedHomeFilter == category.filterPill
                                    val catInteractionSource = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(width = 145.dp, height = 70.dp)
                                            .clip(SmoothCornerShape(16.dp))
                                            .background(Brush.linearGradient(category.gradient))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) BrandGreenBright else Color.Transparent,
                                                shape = SmoothCornerShape(16.dp)
                                            )
                                            .pressBounce(interactionSource = catInteractionSource, pressedScale = 0.95f)
                                            .clickable(
                                                interactionSource = catInteractionSource,
                                                indication = null
                                            ) {
                                                viewModel.selectHomeFilter(if (isSelected) "Semua" else category.filterPill)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = category.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // 2. Putar Cepat Favoritmu (Swipe ke kiri 4-lagu per kolom style Musicfy & YouTube Music)
                    if (uiState.mostPlayedTracks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Putar Cepat Favoritmu", icon = Icons.AutoMirrored.Filled.TrendingUp)
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalSongColumns(
                                tracks = uiState.mostPlayedTracks.take(16),
                                currentTrackId = uiState.currentTrack?.id,
                                isPlaying = uiState.isPlaying,
                                onTrackClick = { track, allTracks ->
                                    viewModel.playTrack(track, allTracks)
                                },
                                onTrackLongClick = { track ->
                                    viewModel.showTrackContextMenu(track)
                                }
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    // Online-Only Sections
                    if (uiState.isOnline) {
                        // 3. Artis Populer Untukmu (Carousel Avatar Lingkaran Musisi Top)
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

                        // 4. Playlist Populer YouTube Music (Carousel Cover Persegi Style Musicfy & YouTube Music)
                        if (uiState.communityPlaylists.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Playlist Populer YouTube Music", icon = Icons.Default.Headphones)
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(uiState.communityPlaylists, key = { it.id }) { playlist ->
                                        PlaylistHomeCard(
                                            playlist = playlist,
                                            onClick = { viewModel.openPlaylistFromHome(playlist) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                        }

                        // 5. Album & Single Rilisan Terbaru (Carousel Cover Persegi)
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

                        // 5. Top Trending di Indonesia (Swipe ke kiri 4-lagu per kolom style Musicfy & YouTube Music)
                        if (uiState.trendingTracks.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Top Trending di Indonesia", icon = Icons.Default.Whatshot)
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalSongColumns(
                                    tracks = uiState.trendingTracks,
                                    currentTrackId = uiState.currentTrack?.id,
                                    isPlaying = uiState.isPlaying,
                                    onTrackClick = { track, allTracks ->
                                        viewModel.playTrack(track, allTracks)
                                    },
                                    onTrackLongClick = { track ->
                                        viewModel.showTrackContextMenu(track)
                                    }
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                        }
                    }
                }
            }
        }

        if (isShazamDialogVisible) {
            val recogState by ShazamRecognitionService.state.collectAsState()
            AlertDialog(
                onDismissRequest = {
                    isShazamDialogVisible = false
                    ShazamRecognitionService.reset()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shazam),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Pengenal Musik Shazam", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
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
                                Text("Ditemukan: ${t.title} • ${t.artist.name}", color = BrandGreen, fontWeight = FontWeight.Bold)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (recogState is RecognitionState.Error) {
                            TextButton(onClick = {
                                scope.launch {
                                    ShazamRecognitionService.startListening(context) { recognized ->
                                        recognized?.let { track ->
                                            viewModel.playTrack(track)
                                            isShazamDialogVisible = false
                                        }
                                    }
                                }
                            }) {
                                Text("Coba Lagi", color = BrandGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = {
                            isShazamDialogVisible = false
                            ShazamRecognitionService.reset()
                        }) {
                            Text("Tutup", color = TextSecondary)
                        }
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
private fun PlaylistHomeCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(136.dp)
            .clip(SmoothCornerShape(14.dp))
            .background(SurfaceContainerHigh)
            .pressBounce(pressedScale = 0.95f)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(SmoothCornerShape(10.dp))
                    .background(SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!playlist.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = playlist.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = playlist.description.ifBlank { "Playlist YouTube Music" },
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HorizontalSongColumns(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onTrackLongClick: (Track) -> Unit
) {
    val chunked = remember(tracks) { tracks.chunked(4) }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        items(chunked) { columnTracks ->
            Column(
                modifier = Modifier.width(310.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                columnTracks.forEach { track ->
                    val isActive = currentTrackId == track.id
                    CompactTrackItem(
                        track = track,
                        isPlaying = isPlaying,
                        isActive = isActive,
                        onClick = { onTrackClick(track, tracks) },
                        onLongClick = { onTrackLongClick(track) },
                        onMoreClick = { onTrackLongClick(track) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactTrackItem(
    track: Track,
    isPlaying: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) SurfaceContainerHigh else SurfaceContainer.copy(alpha = 0.45f))
            .pressBounce(pressedScale = 0.98f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainer)
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isActive) BrandGreen else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist.name} • ${track.durationFormatted}",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { onMoreClick?.invoke() ?: onLongClick?.invoke() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

