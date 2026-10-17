package com.rafliadipratama.musiceuy.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.ui.components.EqualizerBottomSheet
import com.rafliadipratama.musiceuy.ui.components.PlayerOptionsBottomSheet
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.DarkBackground
import com.rafliadipratama.musiceuy.ui.theme.DarkSurface
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.MusicPlayerViewModel
import com.rafliadipratama.musiceuy.ui.viewmodel.RepeatMode

@Composable
fun PlayerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack ?: return

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var isOptionsSheetVisible by remember { mutableStateOf(false) }

    val currentMs = if (isUserSeeking) (seekProgress * uiState.durationMs).toLong() else uiState.currentPositionMs
    val durationMs = uiState.durationMs

    val lyricsListState = rememberLazyListState()

    // Auto-scroll synchronized lyrics
    LaunchedEffect(currentMs, uiState.lyrics) {
        if (uiState.isLyricsVisible && uiState.lyrics.hasSyncedLyrics) {
            val activeIndex = uiState.lyrics.syncedLyrics.indexOfLast { it.timestampMs <= currentMs }
            if (activeIndex >= 0) {
                lyricsListState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
            }
        }
    }

    val dominantColor = uiState.dominantColor?.let { Color(it.toInt()) } ?: DarkSurface

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    // Root Container - 100% Solid Opaque (No background transparency)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground) // Opaque solid base
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dominantColor, // Solid 1.0f alpha
                        DarkBackground,
                        DarkBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (isLandscape) {
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
                onCollapse = { viewModel.collapsePlayer() },
                onShowOptions = { isOptionsSheetVisible = true },
                onShowEqualizer = { viewModel.showEqualizer() },
                onToggleLyrics = { viewModel.toggleLyrics() },
                onToggleAutoplay = { viewModel.toggleAutoplay() },
                onToggleQueue = { viewModel.toggleQueue() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.previousTrack() },
                onNext = { viewModel.nextTrack() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleLike = { viewModel.toggleLike(track) },
                onDownload = { viewModel.downloadTrack(track) },
                onOpenArtist = { viewModel.openArtistDetail(track.artist) }
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
                onCollapse = { viewModel.collapsePlayer() },
                onShowOptions = { isOptionsSheetVisible = true },
                onShowEqualizer = { viewModel.showEqualizer() },
                onToggleLyrics = { viewModel.toggleLyrics() },
                onToggleAutoplay = { viewModel.toggleAutoplay() },
                onToggleQueue = { viewModel.toggleQueue() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.previousTrack() },
                onNext = { viewModel.nextTrack() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleLike = { viewModel.toggleLike(track) },
                onDownload = { viewModel.downloadTrack(track) },
                onOpenArtist = { viewModel.openArtistDetail(track.artist) }
            )
        }

        // Options Bottom Sheet (Sleep timer, Share story, Playlist, EQ, Artist)
        if (isOptionsSheetVisible) {
            val isTimerActive = uiState.sleepTimerRemainingSeconds != null || uiState.isSleepTimerEndOfTrack
            PlayerOptionsBottomSheet(
                track = track,
                isSleepTimerActive = isTimerActive,
                sleepTimerRemainingSeconds = uiState.sleepTimerRemainingSeconds,
                onDismiss = { isOptionsSheetVisible = false },
                onSleepTimerClick = { viewModel.showSleepTimerDialog() },
                onShareStoryClick = { viewModel.shareCurrentTrackStory() },
                onAddToPlaylistClick = { viewModel.showAddToPlaylist(track) },
                onEqualizerClick = { viewModel.showEqualizer() },
                onOpenArtistClick = { viewModel.openArtistDetail(track.artist) }
            )
        }

        // Equalizer & Audio Effects Sheet
        if (uiState.isEqualizerSheetVisible) {
            EqualizerBottomSheet(onDismiss = { viewModel.dismissEqualizer() })
        }
    }
}

// ============================================================================
// PORTRAIT PLAYER LAYOUT
// ============================================================================
@Composable
private fun PortraitPlayerLayout(
    track: Track,
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    currentMs: Long,
    durationMs: Long,
    isUserSeeking: Boolean,
    seekProgress: Float,
    isTablet: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    lyricsListState: androidx.compose.foundation.lazy.LazyListState,
    onCollapse: () -> Unit,
    onShowOptions: () -> Unit,
    onShowEqualizer: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleQueue: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onOpenArtist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = if (isTablet) 560.dp else 1000.dp)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Clean Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Tutup Pemutar",
                    tint = TextPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SEDANG MEMUTAR",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = track.artist.name,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShowEqualizer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = if (uiState.isEqualizerSheetVisible) AccentEmerald else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = onShowOptions,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Lainnya",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Green dot indicator if sleep timer is active
                    val isTimerActive = uiState.sleepTimerRemainingSeconds != null || uiState.isSleepTimerEndOfTrack
                    if (isTimerActive) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. Center Area: Large Artwork or Realtime Lyrics ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLyricsVisible) {
                SyncedLyricsView(
                    uiState = uiState,
                    currentMs = currentMs,
                    lyricsListState = lyricsListState
                )
            } else {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(if (isTablet) 0.72f else 0.88f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(22.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 3. Track Info Row (Title, Artist, Like, Download) ---
        TrackInfoRow(
            track = track,
            uiState = uiState,
            onOpenArtist = onOpenArtist,
            onToggleLike = onToggleLike,
            onDownload = onDownload
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. Progress Seekbar & Timers ---
        PlaybackSeekbar(
            currentMs = currentMs,
            durationMs = durationMs,
            isUserSeeking = isUserSeeking,
            seekProgress = seekProgress,
            onSeekChange = onSeekChange,
            onSeekFinished = onSeekFinished
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. Primary Controls (Shuffle, Prev, Play/Pause, Next, Repeat) ---
        PrimaryControlsRow(
            isPlaying = uiState.isPlaying,
            isShuffle = uiState.isShuffle,
            repeatMode = uiState.repeatMode,
            onToggleShuffle = onToggleShuffle,
            onPrevious = onPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
            onToggleRepeat = onToggleRepeat
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. Clean Bottom Utility Bar (Lyrics, Autoplay, Queue) ---
        BottomUtilityRow(
            isLyricsVisible = uiState.isLyricsVisible,
            isAutoplayEnabled = uiState.isAutoplayEnabled,
            isQueueVisible = uiState.isQueueVisible,
            onToggleLyrics = onToggleLyrics,
            onToggleAutoplay = onToggleAutoplay,
            onToggleQueue = onToggleQueue
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ============================================================================
// LANDSCAPE PLAYER LAYOUT (Responsive 2-Pane)
// ============================================================================
@Composable
private fun LandscapePlayerLayout(
    track: Track,
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    currentMs: Long,
    durationMs: Long,
    isUserSeeking: Boolean,
    seekProgress: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    lyricsListState: androidx.compose.foundation.lazy.LazyListState,
    onCollapse: () -> Unit,
    onShowOptions: () -> Unit,
    onShowEqualizer: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleQueue: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onOpenArtist: () -> Unit
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
            // Top Bar: Collapse button & Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            // Artwork
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
            )

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
                } else {
                    Text(
                        text = "KONTROL PEMUTAR",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShowEqualizer, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = if (uiState.isEqualizerSheetVisible) AccentEmerald else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onShowOptions, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Center: Lyrics (if active) or Spaced Controls
            if (uiState.isLyricsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    SyncedLyricsView(
                        uiState = uiState,
                        currentMs = currentMs,
                        lyricsListState = lyricsListState
                    )
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
                    onSeekChange = onSeekChange,
                    onSeekFinished = onSeekFinished
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrimaryControlsRow(
                    isPlaying = uiState.isPlaying,
                    isShuffle = uiState.isShuffle,
                    repeatMode = uiState.repeatMode,
                    playButtonSize = 56.dp,
                    playIconSize = 32.dp,
                    onToggleShuffle = onToggleShuffle,
                    onPrevious = onPrevious,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                    onToggleRepeat = onToggleRepeat
                )

                Spacer(modifier = Modifier.height(10.dp))

                BottomUtilityRow(
                    isLyricsVisible = uiState.isLyricsVisible,
                    isAutoplayEnabled = uiState.isAutoplayEnabled,
                    isQueueVisible = uiState.isQueueVisible,
                    onToggleLyrics = onToggleLyrics,
                    onToggleAutoplay = onToggleAutoplay,
                    onToggleQueue = onToggleQueue
                )
            }
        }
    }
}

// ============================================================================
// SHARED SUB-COMPONENTS
// ============================================================================

@Composable
private fun TrackInfoRow(
    track: Track,
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    onOpenArtist: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit
) {
    val isLiked = uiState.likedTrackIds.contains(track.id) || track.isLiked
    val isDownloaded = uiState.downloadedTrackIds.contains(track.id)
    val downloadProgress = uiState.downloadingTrackIds[track.id]
    val isDownloading = downloadProgress != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = track.title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenArtist)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = track.artist.name,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.artist.isVerified || track.isVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Terverifikasi",
                        tint = AccentEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Download Button
            IconButton(
                onClick = onDownload,
                enabled = !isDownloading && !isDownloaded,
                modifier = Modifier.size(42.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        progress = { downloadProgress ?: 0f },
                        modifier = Modifier.size(20.dp),
                        color = AccentEmerald,
                        strokeWidth = 2.dp
                    )
                } else if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Telah Diunduh",
                        tint = AccentEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Unduh Lagu",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Like Button
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Hapus dari Favorit" else "Sukai",
                    tint = if (isLiked) Color(0xFFE91E63) else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaybackSeekbar(
    currentMs: Long,
    durationMs: Long,
    isUserSeeking: Boolean,
    seekProgress: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    val sliderValue = if (durationMs > 0) (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (isUserSeeking) seekProgress else sliderValue,
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            colors = SliderDefaults.colors(
                thumbColor = AccentEmerald,
                activeTrackColor = AccentEmerald,
                inactiveTrackColor = DarkSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(currentMs),
                color = TextMuted,
                fontSize = 12.sp
            )
            Text(
                text = formatMs(durationMs),
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PrimaryControlsRow(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    playButtonSize: androidx.compose.ui.unit.Dp = 66.dp,
    playIconSize: androidx.compose.ui.unit.Dp = 36.dp,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(42.dp)) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) AccentEmerald else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        // Previous
        IconButton(onClick = onPrevious, modifier = Modifier.size(46.dp)) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Sebelumnya",
                tint = TextPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        // Play / Pause (Large Center Button)
        Box(
            modifier = Modifier
                .size(playButtonSize)
                .clip(CircleShape)
                .background(AccentEmerald)
                .clickable(onClick = onTogglePlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.Black,
                modifier = Modifier.size(playIconSize)
            )
        }

        // Next
        IconButton(onClick = onNext, modifier = Modifier.size(46.dp)) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Berikutnya",
                tint = TextPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        // Repeat Mode
        IconButton(onClick = onToggleRepeat, modifier = Modifier.size(42.dp)) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) AccentEmerald else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun BottomUtilityRow(
    isLyricsVisible: Boolean,
    isAutoplayEnabled: Boolean,
    isQueueVisible: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onToggleQueue: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Lyrics Pill Toggle
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isLyricsVisible) AccentEmerald.copy(alpha = 0.2f) else DarkSurface)
                .clickable(onClick = onToggleLyrics)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = null,
                tint = if (isLyricsVisible) AccentEmerald else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Lirik",
                color = if (isLyricsVisible) AccentEmerald else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 2. Autoplay Pill Toggle
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isAutoplayEnabled) AccentEmerald.copy(alpha = 0.2f) else DarkSurface)
                .clickable(onClick = onToggleAutoplay)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AllInclusive,
                contentDescription = null,
                tint = if (isAutoplayEnabled) AccentEmerald else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAutoplayEnabled) "Radio Aktif" else "Radio Mati",
                color = if (isAutoplayEnabled) AccentEmerald else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 3. Queue Button
        IconButton(
            onClick = onToggleQueue,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Antrean",
                tint = if (isQueueVisible) AccentEmerald else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SyncedLyricsView(
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    currentMs: Long,
    lyricsListState: androidx.compose.foundation.lazy.LazyListState
) {
    if (uiState.isLoadingLyrics) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                color = AccentEmerald,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Menyelaraskan lirik...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    } else if (uiState.lyrics.hasSyncedLyrics) {
        val synced = uiState.lyrics.syncedLyrics
        val activeIndex = synced.indexOfLast { it.timestampMs <= currentMs }

        LazyColumn(
            state = lyricsListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(synced) { index, line ->
                val isActive = index == activeIndex
                Text(
                    text = line.text,
                    color = if (isActive) TextPrimary else TextMuted.copy(alpha = 0.45f),
                    fontSize = if (isActive) 20.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    } else if (!uiState.lyrics.plainLyrics.isNullOrBlank()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = uiState.lyrics.plainLyrics ?: "",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Lirik belum tersedia untuk lagu ini",
                color = TextMuted,
                fontSize = 14.sp
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
