package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.lyrics.LyricRow
import com.rafliadipratama.musiceuy.lyrics.LyricsSearchMatch
import com.rafliadipratama.musiceuy.ui.components.DoubleTapHeartOverlay
import com.rafliadipratama.musiceuy.ui.components.LyricsSearchBar
import com.rafliadipratama.musiceuy.ui.components.SmoothCornerShape
import com.rafliadipratama.musiceuy.ui.components.press3D
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary

@Composable
internal fun LandscapePlayerLayout(
    track: Track,
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    currentMs: Long,
    durationMs: Long,
    isUserSeeking: Boolean,
    seekProgress: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    lyricsListState: androidx.compose.foundation.lazy.LazyListState,
    rows: List<LyricRow> = emptyList(),
    rowOfLine: IntArray = IntArray(0),
    staggerLags: Map<Int, Float> = emptyMap(),
    searchMatches: List<LyricsSearchMatch> = emptyList(),
    currentMatchIndex: Int = 0,
    isSearchOpen: Boolean = false,
    searchQuery: String = "",
    onToggleSearch: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onNextMatch: () -> Unit = {},
    onPrevMatch: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onOpenLyricsOffset: () -> Unit = {},
    onCollapse: () -> Unit,
    onShowOptions: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onOpenArtist: () -> Unit,
    onSeekToLine: (Long) -> Unit = {},
    onToggleVideoMode: (Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT PANE (Artwork + Track Details) ---
        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Collapse button & Label + Audio/Video Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCollapse, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Tutup",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SEDANG MEMUTAR",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }

                AudioVideoToggleSwitch(
                    isVideoMode = uiState.isVideoMode,
                    onToggle = onToggleVideoMode
                )
            }

            // Artwork or Video with Double-Tap to Like
            val isLiked = uiState.likedTrackIds.contains(track.id)
            var showDoubleTapHeart by remember(track.id) { mutableStateOf(false) }

            if (uiState.isVideoMode) {
                PlayerVideoSurface(
                    streamUrl = uiState.currentVideoStreamUrl,
                    isLoading = uiState.isLoadingVideo,
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = currentMs,
                    track = track,
                    isLiked = isLiked,
                    onToggleLike = onToggleLike,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(16f / 10f)
                )
            } else {
                Box(
                    modifier = Modifier

                        .weight(1f)
                        .aspectRatio(1f)
                        .press3D(maxTilt = 6f, pressedScale = 0.98f)
                        .clip(SmoothCornerShape(18.dp))
                        .pointerInput(track.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (!isLiked) {
                                        onToggleLike()
                                    }
                                    showDoubleTapHeart = true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    DoubleTapHeartOverlay(
                        show = showDoubleTapHeart,
                        onAnimationEnd = { showDoubleTapHeart = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Track Details & Like/Download
            TrackInfoRow(
                track = track,
                uiState = uiState,
                onOpenArtist = onOpenArtist,
                onToggleLike = onToggleLike,
                onDownload = onDownload
            )
        }

        // --- RIGHT PANE (Lyrics / Controls & Seekbar) ---
        Column(
            modifier = Modifier
                .weight(0.56f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar on Right Pane
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isLyricsVisible) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleLyrics)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Lirik",
                            tint = AccentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TUTUP LIRIK",
                            color = AccentEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleSearch, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari Lirik",
                                tint = if (isSearchOpen) (uiState.dominantColor?.let { Color(it.toInt()) } ?: AccentEmerald) else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = onOpenLyricsOffset, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Sinkronisasi Waktu Lirik",
                                tint = if (uiState.lyricsOffsetMs != 0L) (uiState.dominantColor?.let { Color(it.toInt()) } ?: AccentEmerald) else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box {
                            IconButton(onClick = onShowOptions, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opsi",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            val isTimerActive = uiState.sleepTimerRemainingSeconds != null || uiState.isSleepTimerEndOfTrack
                            val isOffsetActive = uiState.lyricsOffsetMs != 0L
                            if (isTimerActive || isOffsetActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(uiState.dominantColor?.let { Color(it.toInt()) } ?: AccentEmerald)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "KONTROL PEMUTAR",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )

                    Box {
                        IconButton(onClick = onShowOptions, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opsi",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        val isTimerActive = uiState.sleepTimerRemainingSeconds != null || uiState.isSleepTimerEndOfTrack
                        if (isTimerActive) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(uiState.dominantColor?.let { Color(it.toInt()) } ?: AccentEmerald)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }

            // Center: Lyrics (if active) or Spaced Controls
            if (uiState.isLyricsVisible) {
                val dominantColor = uiState.dominantColor?.let { Color(it.toInt()) } ?: AccentEmerald
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    SyncedLyricsView(
                        uiState = uiState,
                        currentMs = currentMs,
                        lyricsListState = lyricsListState,
                        rows = rows,
                        rowOfLine = rowOfLine,
                        staggerLags = staggerLags,
                        dominantColor = dominantColor,
                        searchMatches = searchMatches,
                        currentMatchIndex = currentMatchIndex,
                        onSeekToLine = onSeekToLine
                    )

                    if (isSearchOpen) {
                        LyricsSearchBar(
                            query = searchQuery,
                            onQueryChange = onQueryChange,
                            totalMatches = searchMatches.size,
                            currentMatchIndex = currentMatchIndex,
                            onNextMatch = onNextMatch,
                            onPrevMatch = onPrevMatch,
                            onClose = onCloseSearch,
                            accentColor = dominantColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom Area: Seekbar + Playback Controls + Utility Row
            Column(modifier = Modifier.fillMaxWidth()) {
                PlaybackSeekbar(
                    currentMs = currentMs,
                    durationMs = durationMs,
                    isUserSeeking = isUserSeeking,
                    seekProgress = seekProgress,
                    isPlaying = uiState.isPlaying,
                    onSeekChange = onSeekChange,
                    onSeekFinished = onSeekFinished
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrimaryControlsRow(
                    isPlaying = uiState.isPlaying,
                    playButtonSize = 56.dp,
                    playIconSize = 32.dp,
                    onPrevious = onPrevious,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                )

                Spacer(modifier = Modifier.height(10.dp))

                BottomUtilityRow(
                    isLyricsVisible = uiState.isLyricsVisible,
                    isQueueVisible = uiState.isQueueVisible,
                    onToggleLyrics = onToggleLyrics,
                    onToggleQueue = onToggleQueue
                )
            }
        }
    }
}
