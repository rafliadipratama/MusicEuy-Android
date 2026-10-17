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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.ui.components.FeaturedMix
import com.rafliadipratama.musiceuy.ui.components.FeaturedMixCard
import com.rafliadipratama.musiceuy.ui.components.MiniPlayer
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
import com.rafliadipratama.musiceuy.ui.theme.VerifiedBadge
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

@Composable
fun HomeScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val featuredMixes = remember {
        listOf(
            FeaturedMix(
                title = "Pop Indo Viral",
                subtitle = "Bernadya, Nadin, Hindia",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500",
                searchQuery = "Pop Indonesia Viral"
            ),
            FeaturedMix(
                title = "Indie Pop Indo",
                subtitle = "Hindia, Nadin, Sal Priadi",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                searchQuery = "Indie Pop Indonesia"
            ),
            FeaturedMix(
                title = "Top Hits Global",
                subtitle = "Coldplay, Bruno Mars, Taylor",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500",
                searchQuery = "Coldplay Bruno Mars Hits"
            ),
            FeaturedMix(
                title = "Akustik Senja",
                subtitle = "Feby Putri, Fiersa Besari",
                coverUrl = "https://images.unsplash.com/photo-1445985543470-41fdd6ce388d?w=500",
                searchQuery = "Akustik Indonesia Senja Santai"
            ),
            FeaturedMix(
                title = "Nostalgia 2000-an",
                subtitle = "Sheila On 7, Dewa 19",
                coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500",
                searchQuery = "Sheila On 7 Dewa 19"
            ),
            FeaturedMix(
                title = "Lo-Fi Beats",
                subtitle = "Relaksasi • Santai • Kopi",
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500",
                searchQuery = "Lofi Hip Hop Chill Beats"
            )
        )
    }

    val filterPills = listOf("Semua", "Sering Diputar", "Pop Indonesia", "Artis Terverifikasi", "Top Hits")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: MusicEuy Brand Logo & Live Badge
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

                // Live Stream Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BrandGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Koleksi Musik",
                        color = BrandGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
                    Text("Cari musik, artis, atau album...", color = TextMuted, fontSize = 14.sp)
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
                        IconButton(onClick = { searchInput = "" }) {
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

            // Quick Filter Pills (Horizontal Scroll)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterPills) { pill ->
                    val isSelected = selectedFilter == pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) BrandGreen else SurfaceContainerHigh)
                            .clickable {
                                selectedFilter = pill
                                when (pill) {
                                    "Semua" -> viewModel.loadTrending()
                                    "Sering Diputar" -> viewModel.showMostPlayed()
                                    "Pop Indonesia" -> viewModel.search("Pop Indonesia Hits")
                                    "Artis Terverifikasi" -> viewModel.search("Bernadya Hindia Tulus Nadin")
                                    "Top Hits" -> viewModel.search("Top Hits Musik Indonesia")
                                }
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

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 150.dp)
            ) {
                // If not in search mode, show Featured Mixes section
                if (uiState.searchResults.isEmpty() && !uiState.isSearching) {
                    item {
                        Text(
                            text = "Akses Cepat & Pilihan Tren",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }

                    // 2x2 Grid of Featured Mixes
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in featuredMixes.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        FeaturedMixCard(
                                            mix = featuredMixes[i],
                                            onClick = {
                                                viewModel.search(featuredMixes[i].searchQuery)
                                            }
                                        )
                                    }
                                    if (i + 1 < featuredMixes.size) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            FeaturedMixCard(
                                                mix = featuredMixes[i + 1],
                                                onClick = {
                                                    viewModel.search(featuredMixes[i + 1].searchQuery)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Search Status or Loading
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

                // Section Title for Tracks
                val tracks = if (uiState.searchResults.isNotEmpty()) uiState.searchResults else uiState.trendingTracks
                val title = if (uiState.searchResults.isNotEmpty()) {
                    if (selectedFilter == "Sering Diputar") "Lagu Sering Kamu Putar" else "Hasil Pencarian Musik"
                } else if (uiState.mostPlayedTracks.isNotEmpty()) {
                    "Rekomendasi Untuk Kamu"
                } else {
                    "Lagu Populer Indonesia"
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = VerifiedBadge,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                items(tracks, key = { it.id }) { track ->
                    val isActive = uiState.currentTrack?.id == track.id
                    TrackItem(
                        track = track,
                        isPlaying = uiState.isPlaying,
                        isActive = isActive,
                        onClick = {
                            viewModel.playTrack(track, tracks)
                        }
                    )
                }
            }
        }
    }
}
