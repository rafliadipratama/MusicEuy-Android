package com.rafliadipratama.musiceuy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafliadipratama.musiceuy.ui.components.AddToPlaylistBottomSheet
import com.rafliadipratama.musiceuy.ui.components.BottomNavBar
import com.rafliadipratama.musiceuy.ui.components.MiniPlayer
import com.rafliadipratama.musiceuy.ui.components.NavigationTab
import com.rafliadipratama.musiceuy.ui.components.QueueBottomSheet
import com.rafliadipratama.musiceuy.ui.screens.ArtistDetailScreen
import com.rafliadipratama.musiceuy.ui.screens.HomeScreen
import com.rafliadipratama.musiceuy.ui.screens.LibraryScreen
import com.rafliadipratama.musiceuy.ui.screens.PlayerScreen
import com.rafliadipratama.musiceuy.ui.screens.PlaylistDetailScreen
import com.rafliadipratama.musiceuy.ui.screens.PluginsScreen
import com.rafliadipratama.musiceuy.ui.theme.MusicEuyTheme
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MusicEuyTheme {
                val viewModel: MusicPlayerViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }

                BackHandler(enabled = uiState.selectedPlaylist != null) {
                    viewModel.closePlaylist()
                }

                BackHandler(enabled = uiState.selectedPlaylist == null && uiState.selectedArtist != null) {
                    viewModel.closeArtistDetail()
                }

                BackHandler(enabled = uiState.selectedPlaylist == null && uiState.selectedArtist == null && uiState.isAddToPlaylistSheetVisible) {
                    viewModel.dismissAddToPlaylist()
                }

                BackHandler(enabled = uiState.selectedPlaylist == null && uiState.selectedArtist == null && !uiState.isAddToPlaylistSheetVisible && uiState.isQueueVisible) {
                    viewModel.dismissQueue()
                }

                BackHandler(enabled = uiState.selectedPlaylist == null && uiState.selectedArtist == null && !uiState.isAddToPlaylistSheetVisible && !uiState.isQueueVisible && uiState.isPlayerExpanded) {
                    viewModel.collapsePlayer()
                }

                BackHandler(enabled = uiState.selectedPlaylist == null && uiState.selectedArtist == null && !uiState.isAddToPlaylistSheetVisible && !uiState.isQueueVisible && !uiState.isPlayerExpanded && selectedTab != NavigationTab.HOME) {
                    selectedTab = NavigationTab.HOME
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBackground)
                        .statusBarsPadding()
                ) {
                    // Main Tabs
                    when (selectedTab) {
                        NavigationTab.HOME -> HomeScreen(viewModel = viewModel)
                        NavigationTab.LIBRARY -> LibraryScreen(
                            viewModel = viewModel,
                            onNavigateToHome = { selectedTab = NavigationTab.HOME }
                        )
                        NavigationTab.PLUGINS -> PluginsScreen(viewModel = viewModel)
                    }

                    // Floating MiniPlayer + Bottom Navigation Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    ) {
                        AnimatedVisibility(
                            visible = uiState.currentTrack != null,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                        ) {
                            uiState.currentTrack?.let { track ->
                                MiniPlayer(
                                    track = track,
                                    isPlaying = uiState.isPlaying,
                                    currentPosMs = uiState.currentPositionMs,
                                    durationMs = uiState.durationMs,
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onNext = { viewModel.nextTrack() },
                                    onQueueClick = { viewModel.toggleQueue() },
                                    onClick = { viewModel.expandPlayer() },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        BottomNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    // Artist Detail Screen Overlay
                    AnimatedVisibility(
                        visible = uiState.selectedArtist != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        uiState.selectedArtist?.let { artist ->
                            ArtistDetailScreen(
                                artist = artist,
                                viewModel = viewModel,
                                onBackClick = { viewModel.closeArtistDetail() }
                            )
                        }
                    }

                    // Playlist Detail Screen Overlay
                    AnimatedVisibility(
                        visible = uiState.selectedPlaylist != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        uiState.selectedPlaylist?.let { playlist ->
                            PlaylistDetailScreen(
                                playlist = playlist,
                                viewModel = viewModel,
                                onBackClick = { viewModel.closePlaylist() }
                            )
                        }
                    }

                    // Full-screen Player Overlay
                    AnimatedVisibility(
                        visible = uiState.isPlayerExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        PlayerScreen(viewModel = viewModel)
                    }

                    // Queue Bottom Sheet with Scrim
                    AnimatedVisibility(
                        visible = uiState.isQueueVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.dismissQueue() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .clickable(enabled = false) {}
                            ) {
                                QueueBottomSheet(
                                    currentTrack = uiState.currentTrack,
                                    queue = uiState.queue,
                                    isPlaying = uiState.isPlaying,
                                    onTrackClick = { track -> viewModel.playFromQueue(track) },
                                    onRemoveTrack = { trackId -> viewModel.removeFromQueue(trackId) },
                                    onClearQueue = { viewModel.clearQueue() },
                                    onDismiss = { viewModel.dismissQueue() }
                                )
                            }
                        }
                    }

                    // Add to Playlist Bottom Sheet with Scrim
                    AnimatedVisibility(
                        visible = uiState.isAddToPlaylistSheetVisible && uiState.trackToAddToPlaylist != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.dismissAddToPlaylist() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .clickable(enabled = false) {}
                            ) {
                                uiState.trackToAddToPlaylist?.let { track ->
                                    AddToPlaylistBottomSheet(
                                        track = track,
                                        playlists = uiState.playlists,
                                        onSelectPlaylist = { playlistId ->
                                            viewModel.addTrackToPlaylist(playlistId, track)
                                        },
                                        onCreatePlaylist = { name, desc ->
                                            viewModel.createPlaylist(name, desc, track)
                                        },
                                        onDismiss = { viewModel.dismissAddToPlaylist() }
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
