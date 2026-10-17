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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // Hero Header with Gradient & Artist Avatar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
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
                    // Back Button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Artist Info in Center
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!artist.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = artist.avatarUrl,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artist.name.take(1).uppercase(),
                                    color = BrandGreen,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = artist.name,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (artist.isVerified) {
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
                            text = "Artis Musik Terverifikasi",
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
                            if (uiState.artistTracks.isNotEmpty()) {
                                viewModel.playTrack(uiState.artistTracks.first(), uiState.artistTracks)
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
                            if (uiState.artistTracks.isNotEmpty()) {
                                val shuffled = uiState.artistTracks.shuffled()
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

            // Popular Tracks Header
            item {
                Text(
                    text = "Lagu Populer",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (uiState.isLoadingArtist) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }
            } else {
                items(uiState.artistTracks, key = { it.id }) { track ->
                    val isActive = uiState.currentTrack?.id == track.id
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        TrackItem(
                            track = track,
                            isPlaying = uiState.isPlaying,
                            isActive = isActive,
                            onClick = {
                                viewModel.playTrack(track, uiState.artistTracks)
                            },
                            onAddToPlaylistClick = {
                                viewModel.showAddToPlaylist(track)
                            }
                        )
                    }
                }
            }
        }
    }
}
