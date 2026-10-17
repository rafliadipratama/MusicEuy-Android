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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafliadipratama.musiceuy.ui.components.BottomNavBar
import com.rafliadipratama.musiceuy.ui.components.MiniPlayer
import com.rafliadipratama.musiceuy.ui.components.NavigationTab
import com.rafliadipratama.musiceuy.ui.components.QueueBottomSheet
import com.rafliadipratama.musiceuy.ui.screens.HomeScreen
import com.rafliadipratama.musiceuy.ui.screens.LibraryScreen
import com.rafliadipratama.musiceuy.ui.screens.PlayerScreen
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

                BackHandler(enabled = uiState.isQueueVisible) {
                    viewModel.dismissQueue()
                }

                BackHandler(enabled = !uiState.isQueueVisible && uiState.isPlayerExpanded) {
                    viewModel.collapsePlayer()
                }

                BackHandler(enabled = !uiState.isQueueVisible && !uiState.isPlayerExpanded && selectedTab != NavigationTab.HOME) {
                    selectedTab = NavigationTab.HOME
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBackground)
                        .statusBarsPadding()
                ) {
                    when (selectedTab) {
                        NavigationTab.HOME -> HomeScreen(viewModel = viewModel)
                        NavigationTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                        NavigationTab.PLUGINS -> PluginsScreen(viewModel = viewModel)
                    }

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

                    AnimatedVisibility(
                        visible = uiState.isPlayerExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        PlayerScreen(viewModel = viewModel)
                    }

                    // Queue Bottom Sheet
                    AnimatedVisibility(
                        visible = uiState.isQueueVisible,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.align(Alignment.BottomCenter)
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
        }
    }
}
