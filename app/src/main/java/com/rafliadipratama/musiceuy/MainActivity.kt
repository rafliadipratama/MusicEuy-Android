package com.rafliadipratama.musiceuy

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
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
import com.rafliadipratama.musiceuy.ui.components.SleepTimerBottomSheet
import com.rafliadipratama.musiceuy.ui.components.TrackContextMenuBottomSheet
import com.rafliadipratama.musiceuy.ui.screens.ArtistDetailScreen
import com.rafliadipratama.musiceuy.ui.screens.DownloadedTracksScreen
import com.rafliadipratama.musiceuy.ui.screens.HomeScreen
import com.rafliadipratama.musiceuy.ui.screens.LibraryScreen
import com.rafliadipratama.musiceuy.ui.screens.OnboardingScreen
import com.rafliadipratama.musiceuy.ui.screens.PlayerScreen
import com.rafliadipratama.musiceuy.ui.screens.PlaylistDetailScreen
import com.rafliadipratama.musiceuy.ui.screens.PluginsScreen
import com.rafliadipratama.musiceuy.ui.screens.SearchScreen
import com.rafliadipratama.musiceuy.ui.screens.ProfileScreen
import com.rafliadipratama.musiceuy.ui.components.LyricsShareDialog
import com.rafliadipratama.musiceuy.ui.theme.MusicEuyTheme
import com.rafliadipratama.musiceuy.ui.theme.ObsidianBackground
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MusicPlayerViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val baseContext = androidx.compose.ui.platform.LocalContext.current
            val localizedContext = remember(uiState.currentLanguageCode) {
                com.rafliadipratama.musiceuy.util.LanguageHelper.getLocalizedContext(baseContext, uiState.currentLanguageCode)
            }

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext
            ) {
                MusicEuyTheme {
                    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }

                    // Back Handlers (Highest to Lowest Priority)
                    BackHandler(enabled = uiState.isPlayerExpanded) {
                        viewModel.collapsePlayer()
                    }
                    BackHandler(enabled = uiState.isQueueVisible) {
                        viewModel.dismissQueue()
                    }
                    BackHandler(enabled = uiState.isEqualizerSheetVisible) {
                        viewModel.dismissEqualizer()
                    }
                    BackHandler(enabled = uiState.trackForContextMenu != null) {
                        viewModel.dismissTrackContextMenu()
                    }
                    BackHandler(enabled = uiState.isAddToPlaylistSheetVisible) {
                        viewModel.dismissAddToPlaylist()
                    }
                    BackHandler(enabled = uiState.isSleepTimerDialogVisible) {
                        viewModel.hideSleepTimerDialog()
                    }
                    BackHandler(enabled = uiState.isLanguageDialogVisible) {
                        viewModel.dismissLanguageDialog()
                    }
                    BackHandler(enabled = uiState.updateInfo != null) {
                        viewModel.dismissUpdateDialog()
                    }
                    BackHandler(enabled = uiState.isProfileVisible) {
                        viewModel.closeProfile()
                    }
                    BackHandler(enabled = uiState.isDownloadedScreenVisible) {
                        viewModel.closeDownloadedTracks()
                    }
                    BackHandler(enabled = uiState.selectedPlaylist != null) {
                        viewModel.closePlaylist()
                    }
                    BackHandler(enabled = uiState.selectedArtist != null) {
                        viewModel.closeArtistDetail()
                    }
                    BackHandler(enabled = !uiState.isOnboardingCompleted) {
                        viewModel.skipOnboarding()
                    }

                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                    ) { /* Handled */ }

                    LaunchedEffect(Unit) {
                        val prefs = getSharedPreferences("musiceuy_app_prefs", MODE_PRIVATE)
                        val hasRequested = prefs.getBoolean("has_requested_media_permission", false)
                        if (!hasRequested) {
                            val perms = mutableListOf<String>()
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                perms.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                perms.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            prefs.edit().putBoolean("has_requested_media_permission", true).apply()
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    }

                    LaunchedEffect(uiState.downloadNotificationMessage) {
                        uiState.downloadNotificationMessage?.let { msg ->
                            com.rafliadipratama.musiceuy.ui.components.TopToaster.show(msg)
                            viewModel.clearDownloadMessage()
                        }
                    }


                if (!uiState.isOnboardingCompleted) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = { artists ->
                            viewModel.completeOnboarding(artists)
                        },
                        onSkip = {
                            viewModel.skipOnboarding()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBackground)
                            .statusBarsPadding()
                    ) {
                    // Main Tabs
                    when (selectedTab) {
                        NavigationTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToSearch = { selectedTab = NavigationTab.SEARCH }
                        )
                        NavigationTab.SEARCH -> SearchScreen(viewModel = viewModel)
                        NavigationTab.LIBRARY -> LibraryScreen(
                            viewModel = viewModel,
                            onNavigateToHome = { selectedTab = NavigationTab.HOME }
                        )
                        NavigationTab.PLUGINS -> PluginsScreen(viewModel = viewModel)
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

                    // Downloaded Tracks Screen Overlay
                    AnimatedVisibility(
                        visible = uiState.isDownloadedScreenVisible,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        DownloadedTracksScreen(
                            viewModel = viewModel,
                            onBackClick = { viewModel.closeDownloadedTracks() }
                        )
                    }

                    // User Profile Screen Overlay (Musicfy Style)
                    AnimatedVisibility(
                        visible = uiState.isProfileVisible,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        ProfileScreen(
                            viewModel = viewModel,
                            onBackClick = { viewModel.closeProfile() }
                        )
                    }

                    // STICKY BOTTOM DOCK (MiniPlayer + BottomNavBar on ALL screens, hidden only in Fullscreen Player)
                    val showBottomDock = !uiState.isPlayerExpanded

                    if (showBottomDock) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = Color(0xFF161B22),
                            shadowElevation = 16.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 840.dp)
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                ) {
                                    // MiniPlayer - Sticky on ALL screens when a track is active
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
                                                dominantColor = uiState.dominantColor,
                                                onPlayPause = { viewModel.togglePlayPause() },
                                                onNext = { viewModel.nextTrack() },
                                                onQueueClick = { viewModel.toggleQueue() },
                                                onClick = { viewModel.expandPlayer() },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Bottom Navigation Bar - Sticky on ALL screens
                                    BottomNavBar(
                                        selectedTab = selectedTab,
                                        onTabSelected = { tab ->
                                            selectedTab = tab
                                            if (uiState.selectedArtist != null) viewModel.closeArtistDetail()
                                            if (uiState.selectedPlaylist != null) viewModel.closePlaylist()
                                            if (uiState.isDownloadedScreenVisible) viewModel.closeDownloadedTracks()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Full-screen Player Overlay (100% Opaque, covers screen when expanded)
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
                                    onReorderUpcoming = { from, to -> viewModel.reorderUpcomingTracks(from, to) },
                                    onMoveTrackToNext = { trackId -> viewModel.moveTrackToNext(trackId) },
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

                    // Track Context Menu Bottom Sheet (Long Press shortcut)
                    AnimatedVisibility(
                        visible = uiState.trackForContextMenu != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.dismissTrackContextMenu() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .clickable(enabled = false) {}
                            ) {
                                uiState.trackForContextMenu?.let { track ->
                                    TrackContextMenuBottomSheet(
                                        track = track,
                                        isLiked = uiState.likedTrackIds.contains(track.id),
                                        isDownloaded = uiState.downloadedTrackIds.contains(track.id),
                                        onPlayNow = {
                                            viewModel.playTrack(track)
                                            viewModel.dismissTrackContextMenu()
                                        },
                                        onStartRadio = {
                                            viewModel.dismissTrackContextMenu()
                                            viewModel.startSongRadio(track)
                                        },
                                        onPlayNext = { viewModel.playNext(track) },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                        onAddToPlaylist = {
                                            viewModel.dismissTrackContextMenu()
                                            viewModel.showAddToPlaylist(track)
                                        },
                                        onToggleLike = {
                                            viewModel.toggleLike(track)
                                            viewModel.dismissTrackContextMenu()
                                        },
                                        onOpenArtist = {
                                            viewModel.dismissTrackContextMenu()
                                            viewModel.openArtistDetail(track.artist)
                                        },
                                        onDownload = {
                                            viewModel.dismissTrackContextMenu()
                                            viewModel.downloadTrack(track)
                                        },
                                        onNotInterested = {
                                            viewModel.dismissTrackContextMenu()
                                            viewModel.markTrackNotInterested(track)
                                        },
                                        onDismiss = { viewModel.dismissTrackContextMenu() }
                                    )
                                }
                            }
                        }
                    }

                    // Sleep Timer Bottom Sheet
                    AnimatedVisibility(
                        visible = uiState.isSleepTimerDialogVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.hideSleepTimerDialog() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .clickable(enabled = false) {}
                            ) {
                                SleepTimerBottomSheet(
                                    remainingSeconds = uiState.sleepTimerRemainingSeconds,
                                    isEndOfTrack = uiState.isSleepTimerEndOfTrack,
                                    onSetTimerMinutes = { mins -> viewModel.setSleepTimer(mins) },
                                    onSetEndOfTrack = { viewModel.setSleepTimerEndOfTrack() },
                                    onCancelTimer = { viewModel.cancelSleepTimer() },
                                    onDismiss = { viewModel.hideSleepTimerDialog() }
                                )
                            }
                        }
                    }

                    // In-App Multi-Language Selection Dialog
                    if (uiState.isLanguageDialogVisible) {
                        com.rafliadipratama.musiceuy.ui.components.LanguageSelectionDialog(
                            currentLanguageCode = uiState.currentLanguageCode,
                            onSelectLanguage = { lang ->
                                viewModel.setLanguage(lang)
                            },
                            onDismiss = { viewModel.dismissLanguageDialog() }
                        )
                    }

                    // Lyrics Share Dialog
                    if (uiState.isLyricsShareDialogVisible && uiState.currentTrack != null) {
                        LyricsShareDialog(
                            track = uiState.currentTrack!!,
                            lyricSnippet = uiState.selectedLyricForShare,
                            dominantColor = uiState.dominantColor,
                            onDismiss = { viewModel.closeLyricsShare() }
                        )
                    }

                    // App Update Notification Bottom Sheet
                    uiState.updateInfo?.let { updateInfo ->
                        if (updateInfo.isUpdateAvailable) {
                            com.rafliadipratama.musiceuy.ui.components.AppUpdateBottomSheet(
                                updateInfo = updateInfo,
                                onDismiss = { viewModel.dismissUpdateDialog() }
                            )
                        }
                    }

                    // Floating Dynamic Island Top Toast Host
                    com.rafliadipratama.musiceuy.ui.components.TopToastHost()
                }
            }
        }
    }
}
}
}
