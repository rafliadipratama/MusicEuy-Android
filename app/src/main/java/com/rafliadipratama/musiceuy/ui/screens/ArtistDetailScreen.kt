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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.rafliadipratama.musiceuy.ui.components.ParallaxCoverHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.ui.components.TrackItem
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.theme.VerifiedBadge
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

@Composable
fun ArtistDetailScreen(
    artist: Artist,
    viewModel: MusicPlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val currentArtist = uiState.selectedArtist ?: artist
    val legitimateTracks = remember(uiState.artistTracks, currentArtist.name) {
        uiState.artistTracks.filter { track ->
            com.rafliadipratama.musiceuy.data.network.ArtistCatalog.isLegitimateArtistTrack(track, currentArtist.name)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp)
        ) {
            // Hero Header with Banner Image, Gradient & Artist Avatar
            item {
                ParallaxCoverHeader(
                    coverUrl = currentArtist.bannerUrl?.takeIf { it.isNotBlank() } ?: currentArtist.avatarUrl,
                    lazyListState = listState,
                    headerHeight = 300.dp,
                    overlayColor = ObsidianBackground
                ) {
                    // Back Button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // 4. Artist Avatar & Info in Center
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val effectiveAvatar = currentArtist.avatarUrl?.takeIf { it.isNotBlank() }
                            ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(currentArtist.name)
                        if (!effectiveAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = effectiveAvatar,
                                contentDescription = currentArtist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(114.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, Color(0xFF161B22), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(114.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen.copy(alpha = 0.25f))
                                    .border(3.dp, Color(0xFF161B22), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentArtist.name.take(1).uppercase(),
                                    color = BrandGreen,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentArtist.name,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentArtist.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Artis Terverifikasi",
                                    tint = VerifiedBadge,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "Artis",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Action Buttons: Play All & Shuffle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (legitimateTracks.isNotEmpty()) {
                                viewModel.playTrack(legitimateTracks.first(), legitimateTracks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0C0E11),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Putar Semua",
                            color = Color(0xFF0C0E11),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (legitimateTracks.isNotEmpty()) {
                                val shuffled = legitimateTracks.shuffled()
                                viewModel.playTrack(shuffled.first(), shuffled)
                            }
                        },
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Acak",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 1. Lagu Populer (Swipe ke kiri 4-lagu per kolom style YouTube Music)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lagu Populer",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (legitimateTracks.isNotEmpty()) {
                        Text(
                            text = "Geser ke kiri untuk lainnya",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (uiState.isLoadingArtist) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandGreen,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else if (legitimateTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada daftar lagu resmi untuk ${currentArtist.name}.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                item {
                    HorizontalSongColumns(
                        tracks = legitimateTracks,
                        currentTrackId = uiState.currentTrack?.id,
                        isPlaying = uiState.isPlaying,
                        onTrackClick = { track, allTracks ->
                            viewModel.playTrack(track, allTracks)
                        },
                        onTrackLongClick = { track ->
                            viewModel.showTrackContextMenu(track)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 2. Video Musik Resmi (Swipe ke kiri style YouTube Music)
            if (uiState.isLoadingArtistVideos) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandGreen,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            } else if (uiState.artistVideos.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Video Musik Resmi",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.artistVideos.size} Video",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.artistVideos, key = { "art_vid_${it.id}" }) { video ->
                            HomeVideoCard(
                                track = video,
                                isActive = uiState.currentTrack?.id == video.id,
                                isPlaying = uiState.isPlaying,
                                onClick = {
                                    viewModel.playTrack(video, uiState.artistVideos)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 3. Album & Diskografi Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Album & Diskografi",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.artistAlbums.isNotEmpty()) {
                        Text(
                            text = "${uiState.artistAlbums.size} Album",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (uiState.isLoadingAlbums) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandGreen,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else if (uiState.artistAlbums.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.artistAlbums, key = { it.id }) { album ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable { viewModel.openAlbum(album) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (!album.coverUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = album.coverUrl,
                                            contentDescription = album.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(116.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(116.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF222730)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LibraryMusic,
                                                contentDescription = null,
                                                tint = BrandGreen,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = album.title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = if (album.year.isNotBlank()) "Album • ${album.year}" else "Album",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
