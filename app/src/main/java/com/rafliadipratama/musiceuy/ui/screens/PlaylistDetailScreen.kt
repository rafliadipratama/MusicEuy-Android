package com.rafliadipratama.musiceuy.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.rafliadipratama.musiceuy.data.model.Playlist
import com.rafliadipratama.musiceuy.ui.components.TrackItem
import com.rafliadipratama.musiceuy.ui.theme.BrandGreen
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.theme.SurfaceContainerHigh
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: MusicPlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPlaylist = uiState.playlists.find { it.id == playlist.id } ?: playlist

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // Header with Gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BrandGreen.copy(alpha = 0.25f),
                                    Color(0xFF161B22),
                                    ObsidianBackground
                                )
                            )
                        )
                ) {
                    // Top Bar Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = {
                            viewModel.deletePlaylist(currentPlaylist.id)
                            onBackClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus Playlist",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Playlist Info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!currentPlaylist.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentPlaylist.coverUrl,
                                contentDescription = currentPlaylist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentPlaylist.name,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (currentPlaylist.description.isNotBlank())
                                currentPlaylist.description
                            else
                                "${currentPlaylist.tracks.size} lagu • Playlist Lokal",
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
                            if (currentPlaylist.tracks.isNotEmpty()) {
                                viewModel.playTrack(currentPlaylist.tracks.first(), currentPlaylist.tracks)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(50.dp),
                        enabled = currentPlaylist.tracks.isNotEmpty(),
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
                            if (currentPlaylist.tracks.isNotEmpty()) {
                                val shuffled = currentPlaylist.tracks.shuffled()
                                viewModel.playTrack(shuffled.first(), shuffled)
                            }
                        },
                        shape = RoundedCornerShape(50.dp),
                        enabled = currentPlaylist.tracks.isNotEmpty(),
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

            // Track List Header
            item {
                Text(
                    text = "Daftar Lagu (${currentPlaylist.tracks.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (currentPlaylist.tracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Belum Ada Lagu",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cari lagu di Beranda dan ketuk ikon tambah playlist untuk memasukkannya ke sini.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            } else {
                items(currentPlaylist.tracks, key = { it.id }) { track ->
                    val isActive = uiState.currentTrack?.id == track.id
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        TrackItem(
                            track = track,
                            isPlaying = uiState.isPlaying,
                            isActive = isActive,
                            onClick = {
                                viewModel.playTrack(track, currentPlaylist.tracks)
                            },
                            onArtistClick = { artist ->
                                viewModel.openArtistDetail(artist)
                            }
                        )
                    }
                }
            }
        }
    }
}
