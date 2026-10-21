package com.rafliadipratama.musiceuy.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.rafliadipratama.musiceuy.ui.components.EqualizerBottomSheet
import com.rafliadipratama.musiceuy.ui.components.LyricsOffsetBottomSheet
import com.rafliadipratama.musiceuy.ui.components.PlayerOptionsBottomSheet
import com.rafliadipratama.musiceuy.ui.player.LandscapePlayerLayout
import com.rafliadipratama.musiceuy.ui.player.PortraitPlayerLayout
import com.rafliadipratama.musiceuy.ui.player.QueuePlayerActions
import com.rafliadipratama.musiceuy.ui.player.LandscapeQueueLayout
import com.rafliadipratama.musiceuy.ui.player.rememberLyricsFollow
import com.rafliadipratama.musiceuy.ui.theme.DarkBackground
import com.rafliadipratama.musiceuy.ui.theme.DarkSurface
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel

@Composable
fun PlayerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack ?: return

    val queueActions = remember(viewModel, track) {
        QueuePlayerActions(
            collapse = viewModel::collapsePlayer,
            openArtist = { viewModel.openArtistDetail(track.artist) },
            toggleLike = { viewModel.toggleLike(track) },
            download = { viewModel.downloadTrack(track) },
            playTrack = viewModel::playFromQueue,
            removeTrack = viewModel::removeFromQueue,
            clearQueue = viewModel::clearQueue,
            reorderUpcoming = viewModel::reorderUpcomingTracks,
            moveToNext = viewModel::moveTrackToNext,
            closeQueue = viewModel::dismissQueue,
            toggleLyrics = viewModel::toggleLyrics,
            toggleQueue = viewModel::toggleQueue,
            previous = viewModel::previousTrack,
            togglePlayPause = viewModel::togglePlayPause,
            next = viewModel::nextTrack,
            toggleShuffle = viewModel::toggleShuffle,
            toggleRepeat = viewModel::toggleRepeatMode,
            toggleAutoplay = viewModel::toggleAutoplay
        )
    }

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var isOptionsSheetVisible by remember { mutableStateOf(false) }

    val currentMs = if (isUserSeeking) (seekProgress * uiState.durationMs).toLong()
        else uiState.currentPositionMs
    val durationMs = uiState.durationMs

    val lyricsFollow = rememberLyricsFollow(
        uiState.lyrics.syncedLyrics, track.id, currentMs, uiState.lyricsOffsetMs,
        uiState.isLyricsVisible, uiState.isQueueVisible
    )
    with(lyricsFollow) {
        val dominantColor = uiState.dominantColor?.let { Color(it.toInt()) } ?: DarkSurface

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTablet = configuration.screenWidthDp >= 600

        // Root Container - Apple Music Inspired Fluid Ambient Glow Backdrop
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.55f),
                            dominantColor.copy(alpha = 0.20f),
                            DarkBackground
                        ),
                        radius = 1600f
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkBackground.copy(alpha = 0.65f),
                            DarkBackground
                        )
                    )
                )
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = uiState.isQueueVisible && isLandscape,
                transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(160)) },
                label = "playerQueueMode"
            ) { queueVisible ->
                if (queueVisible) {
                    LandscapeQueueLayout(
                        track = track, uiState = uiState,
                        currentMs = currentMs, durationMs = durationMs,
                        isUserSeeking = isUserSeeking, seekProgress = seekProgress,
                        onSeekChange = { isUserSeeking = true; seekProgress = it },
                        onSeekFinished = {
                            isUserSeeking = false
                            viewModel.seekTo((seekProgress * durationMs).toLong())
                        },
                        actions = queueActions,
                        onShowOptions = { isOptionsSheetVisible = true }
                    )
                } else if (isLandscape) {

                    // ==================== LANDSCAPE / DUAL-PANE LAYOUT ====================
                    LandscapePlayerLayout(
                        track = track,
                        uiState = uiState,
                        currentMs = currentMs,
                        durationMs = durationMs,
                        isUserSeeking = isUserSeeking,
                        seekProgress = seekProgress,
                        onSeekChange = {
                            isUserSeeking = true
                            seekProgress = it
                        },
                        onSeekFinished = {
                            isUserSeeking = false
                            viewModel.seekTo((seekProgress * durationMs).toLong())
                        },
                        lyricsListState = lyricsListState,
                        rows = rows,
                        rowOfLine = rowOfLine,
                        staggerLags = staggerLags,
                        searchMatches = searchMatches,
                        currentMatchIndex = currentMatchIndex,
                        isSearchOpen = isSearchOpen,
                        searchQuery = searchQuery,
                        onToggleSearch = lyricsFollow::toggleSearch,
                        onQueryChange = lyricsFollow::updateQuery,
                        onNextMatch = lyricsFollow::nextMatch,
                        onPrevMatch = lyricsFollow::previousMatch,
                        onCloseSearch = lyricsFollow::closeSearch,
                        onOpenLyricsOffset = { viewModel.showLyricsOffsetDialog() },
                        onCollapse = { viewModel.collapsePlayer() },
                        onShowOptions = { isOptionsSheetVisible = true },
                        onToggleLyrics = { viewModel.toggleLyrics() },
                        onToggleQueue = { viewModel.toggleQueue() },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.previousTrack() },
                        onNext = { viewModel.nextTrack() },
                        onToggleLike = { viewModel.toggleLike(track) },
                        onDownload = { viewModel.downloadTrack(track) },
                        onOpenArtist = { viewModel.openArtistDetail(track.artist) },
                        onSeekToLine = { posMs -> viewModel.seekTo(posMs) },
                        onToggleVideoMode = { viewModel.setVideoMode(it) }
                    )
                } else {
                    // ==================== PORTRAIT LAYOUT (Mobile & Tablet) ====================
                    PortraitPlayerLayout(
                        track = track,
                        uiState = uiState,
                        currentMs = currentMs,
                        durationMs = durationMs,
                        isUserSeeking = isUserSeeking,
                        seekProgress = seekProgress,
                        isTablet = isTablet,
                        onSeekChange = {
                            isUserSeeking = true
                            seekProgress = it
                        },
                        onSeekFinished = {
                            isUserSeeking = false
                            viewModel.seekTo((seekProgress * durationMs).toLong())
                        },
                        lyricsListState = lyricsListState,
                        rows = rows,
                        rowOfLine = rowOfLine,
                        staggerLags = staggerLags,
                        searchMatches = searchMatches,
                        currentMatchIndex = currentMatchIndex,
                        isSearchOpen = isSearchOpen,
                        searchQuery = searchQuery,
                        onToggleSearch = lyricsFollow::toggleSearch,
                        onQueryChange = lyricsFollow::updateQuery,
                        onNextMatch = lyricsFollow::nextMatch,
                        onPrevMatch = lyricsFollow::previousMatch,
                        onCloseSearch = lyricsFollow::closeSearch,
                        onOpenLyricsOffset = { viewModel.showLyricsOffsetDialog() },
                        onCollapse = { viewModel.collapsePlayer() },
                        onShowOptions = { isOptionsSheetVisible = true },
                        onToggleLyrics = { viewModel.toggleLyrics() },
                        onToggleQueue = { viewModel.toggleQueue() },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.previousTrack() },
                        onNext = { viewModel.nextTrack() },
                        onToggleLike = { viewModel.toggleLike(track) },
                        onDownload = { viewModel.downloadTrack(track) },
                        onOpenArtist = { viewModel.openArtistDetail(track.artist) },
                        onSeekToLine = { posMs -> viewModel.seekTo(posMs) },
                        onToggleVideoMode = { viewModel.setVideoMode(it) },
                        queueActions = queueActions
                    )
                }

        }


            // Options Bottom Sheet (Sleep timer, Share story, Playlist, EQ, Artist, Lyrics Sync)
            if (isOptionsSheetVisible) {
                val isTimerActive = uiState.sleepTimerRemainingSeconds != null || uiState.isSleepTimerEndOfTrack
                PlayerOptionsBottomSheet(
                    track = track,
                    isSleepTimerActive = isTimerActive,
                    sleepTimerRemainingSeconds = uiState.sleepTimerRemainingSeconds,
                    currentLyricsOffsetMs = uiState.lyricsOffsetMs,
                    onDismiss = { isOptionsSheetVisible = false },
                    onStartRadioClick = { viewModel.startSongRadio(track) },
                    onLyricsOffsetClick = { viewModel.showLyricsOffsetDialog() },
                    onSleepTimerClick = { viewModel.showSleepTimerDialog() },
                    onShareStoryClick = { viewModel.shareCurrentTrackStory() },
                    onAddToPlaylistClick = { viewModel.showAddToPlaylist(track) },
                    onEqualizerClick = { viewModel.showEqualizer() },
                    onOpenArtistClick = { viewModel.openArtistDetail(track.artist) },
                    onNotInterestedClick = { viewModel.markTrackNotInterested(track) }
                )
            }

            // Dedicated Lyrics Offset Calibration Sheet (ShowOffsetDialog pattern)
            if (uiState.isLyricsOffsetSheetVisible) {
                LyricsOffsetBottomSheet(
                    currentOffsetMs = uiState.lyricsOffsetMs,
                    onOffsetChange = { viewModel.setLyricsOffset(it) },
                    onResetOffset = { viewModel.resetLyricsOffset() },
                    onDismiss = { viewModel.dismissLyricsOffsetDialog() },
                    dominantColor = dominantColor
                )
            }

            // Equalizer & Audio Effects Sheet
            if (uiState.isEqualizerSheetVisible) {
                EqualizerBottomSheet(onDismiss = { viewModel.dismissEqualizer() })
            }
        }
    }
}
