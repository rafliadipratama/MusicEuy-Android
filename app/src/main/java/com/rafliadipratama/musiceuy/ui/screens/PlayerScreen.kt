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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.rafliadipratama.musiceuy.ui.components.LikeBurst
import com.rafliadipratama.musiceuy.ui.components.DoubleTapHeartOverlay
import com.rafliadipratama.musiceuy.ui.components.SmoothCornerShape
import com.rafliadipratama.musiceuy.ui.components.press3D
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.rafliadipratama.musiceuy.lyrics.LyricsInterludeDots
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
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
    var lastScrolledIndex by remember { mutableIntStateOf(-1) }

    // Auto-scroll synchronized lyrics
    LaunchedEffect(currentMs, uiState.lyrics, uiState.lyricsOffsetMs) {
        if (uiState.isLyricsVisible && uiState.lyrics.hasSyncedLyrics) {
            val adjustedMs = currentMs + uiState.lyricsOffsetMs
            val activeIndex = uiState.lyrics.syncedLyrics.indexOfLast { it.timestampMs <= adjustedMs }
            if (activeIndex >= 0 && activeIndex != lastScrolledIndex) {
                lastScrolledIndex = activeIndex
                lyricsListState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
            }
        }
    }

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
                onOpenArtist = { viewModel.openArtistDetail(track.artist) },
                onSeekToLine = { posMs -> viewModel.seekTo(posMs) },
                onToggleVideoMode = { viewModel.setVideoMode(it) },
                onAdjustLyricsOffset = { viewModel.adjustLyricsOffset(it) },
                onResetLyricsOffset = { viewModel.resetLyricsOffset() }
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
                onOpenArtist = { viewModel.openArtistDetail(track.artist) },
                onSeekToLine = { posMs -> viewModel.seekTo(posMs) },
                onToggleVideoMode = { viewModel.setVideoMode(it) },
                onAdjustLyricsOffset = { viewModel.adjustLyricsOffset(it) },
                onResetLyricsOffset = { viewModel.resetLyricsOffset() }
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
                onStartRadioClick = { viewModel.startSongRadio(track) },
                onSleepTimerClick = { viewModel.showSleepTimerDialog() },
                onShareStoryClick = { viewModel.shareCurrentTrackStory() },
                onAddToPlaylistClick = { viewModel.showAddToPlaylist(track) },
                onEqualizerClick = { viewModel.showEqualizer() },
                onOpenArtistClick = { viewModel.openArtistDetail(track.artist) },
                onNotInterestedClick = { viewModel.markTrackNotInterested(track) }
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
    onOpenArtist: () -> Unit,
    onSeekToLine: (Long) -> Unit = {},
    onToggleVideoMode: (Boolean) -> Unit = {},
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onResetLyricsOffset: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = if (isTablet) 560.dp else 1000.dp)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
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

        // --- 1.5. Audio / Video Switch Pill ---
        AudioVideoToggleSwitch(
            isVideoMode = uiState.isVideoMode,
            onToggle = onToggleVideoMode
        )

        // --- 2. Center Area: Large Artwork or Realtime Lyrics or Video Surface ---
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
                    lyricsListState = lyricsListState,
                    onSeekToLine = onSeekToLine,
                    onAdjustOffset = onAdjustLyricsOffset,
                    onResetOffset = onResetLyricsOffset
                )
            } else if (uiState.isVideoMode) {
                PlayerVideoSurface(
                    streamUrl = uiState.currentVideoStreamUrl,
                    isLoading = uiState.isLoadingVideo,
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = currentMs,
                    track = track,
                    isLiked = uiState.likedTrackIds.contains(track.id),
                    onToggleLike = onToggleLike,
                    modifier = Modifier
                        .fillMaxWidth(if (isTablet) 0.85f else 1f)
                        .aspectRatio(16f / 10f)
                )
            } else {
                val artworkScale by animateFloatAsState(
                    targetValue = if (uiState.isPlaying) 1.0f else 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "AppleMusicArtworkScale"
                )

                val isLiked = uiState.likedTrackIds.contains(track.id)
                var showDoubleTapHeart by remember(track.id) { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isTablet) 0.72f else 0.88f)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                        }
                        .press3D(maxTilt = 6f, pressedScale = 0.98f)
                        .clip(SmoothCornerShape(26.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), SmoothCornerShape(26.dp))
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
        }

        // --- 3. Bottom Controls Area (Track Info, Seekbar, Controls, Utilities) ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrackInfoRow(
                track = track,
                uiState = uiState,
                onOpenArtist = onOpenArtist,
                onToggleLike = onToggleLike,
                onDownload = onDownload
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlaybackSeekbar(
                currentMs = currentMs,
                durationMs = durationMs,
                isUserSeeking = isUserSeeking,
                seekProgress = seekProgress,
                onSeekChange = onSeekChange,
                onSeekFinished = onSeekFinished
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(14.dp))

            BottomUtilityRow(
                isLyricsVisible = uiState.isLyricsVisible,
                isAutoplayEnabled = uiState.isAutoplayEnabled,
                isQueueVisible = uiState.isQueueVisible,
                onToggleLyrics = onToggleLyrics,
                onToggleAutoplay = onToggleAutoplay,
                onToggleQueue = onToggleQueue
            )

            Spacer(modifier = Modifier.height(6.dp))
        }
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
    onOpenArtist: () -> Unit,
    onSeekToLine: (Long) -> Unit = {},
    onToggleVideoMode: (Boolean) -> Unit = {},
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onResetLyricsOffset: () -> Unit = {}
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
                } else {
                    Text(
                        text = "KONTROL PEMUTAR",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }

                IconButton(onClick = onShowOptions, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
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
                        lyricsListState = lyricsListState,
                        onSeekToLine = onSeekToLine,
                        onAdjustOffset = onAdjustLyricsOffset,
                        onResetOffset = onResetLyricsOffset
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
    val isLiked = uiState.likedTrackIds.contains(track.id)
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
                    text = track.fullArtistDisplay,
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

            // Like Button with Burst Animation
            LikeBurst(trigger = isLiked, key = track.id, modifier = Modifier.size(42.dp)) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isLiked) "Hapus dari Favorit" else "Sukai",
                        tint = if (isLiked) Color(0xFFFF2D55) else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
            val remainingMs = (durationMs - currentMs).coerceAtLeast(0L)
            Text(
                text = "-" + formatMs(remainingMs),
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
        // 1. Lyrics Button (Clean Icon-only like Queue)
        IconButton(
            onClick = onToggleLyrics,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lirik",
                tint = if (isLyricsVisible) AccentEmerald else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // 2. Radio / Autoplay Button (Clean Icon-only like Queue)
        IconButton(
            onClick = onToggleAutoplay,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AllInclusive,
                contentDescription = "Radio",
                tint = if (isAutoplayEnabled) AccentEmerald else TextSecondary,
                modifier = Modifier.size(22.dp)
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
    lyricsListState: androidx.compose.foundation.lazy.LazyListState,
    onSeekToLine: (Long) -> Unit = {},
    onAdjustOffset: (Long) -> Unit = {},
    onResetOffset: () -> Unit = {}
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
        val adjustedMs = currentMs + uiState.lyricsOffsetMs
        val activeIndex = synced.indexOfLast { it.timestampMs <= adjustedMs }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lyricsListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 46.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(synced) { index, line ->
                    val isActive = index == activeIndex
                    if (isActive && line.words.isNotEmpty()) {
                        val annotatedString = buildAnnotatedString {
                            line.words.forEachIndexed { wIdx, wordObj ->
                                val isWordPast = adjustedMs > wordObj.endMs
                                val isWordActive = adjustedMs in wordObj.startMs..wordObj.endMs

                                val wordColor = when {
                                    isWordActive -> Color.White
                                    isWordPast -> Color.White.copy(alpha = 0.95f)
                                    else -> Color.White.copy(alpha = 0.35f)
                                }
                                val wordWeight = when {
                                    isWordActive -> FontWeight.Black
                                    isWordPast -> FontWeight.Bold
                                    else -> FontWeight.Medium
                                }

                                withStyle(
                                    style = SpanStyle(
                                        color = wordColor,
                                        fontWeight = wordWeight,
                                        fontSize = if (isWordActive) 25.sp else 23.sp
                                    )
                                ) {
                                    append(wordObj.word)
                                }
                                if (wIdx < line.words.size - 1) {
                                    append(" ")
                                }
                            }
                        }

                        Text(
                            text = annotatedString,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSeekToLine(line.timestampMs) }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    } else {
                        Text(
                            text = line.text,
                            color = if (isActive) Color.White else TextMuted.copy(alpha = 0.35f),
                            fontSize = if (isActive) 23.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSeekToLine(line.timestampMs) }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Floating Offset Calibration Pill (Icons only + compact offset indicator)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onAdjustOffset(-250L) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "-250ms",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                val offset = uiState.lyricsOffsetMs
                val offsetText = when {
                    offset > 0 -> "+${offset}ms"
                    offset < 0 -> "${offset}ms"
                    else -> "0ms"
                }
                Text(
                    text = offsetText,
                    color = if (offset != 0L) AccentEmerald else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = { onAdjustOffset(250L) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+250ms",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (offset != 0L) {
                    IconButton(
                        onClick = onResetOffset,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Offset",
                            tint = AccentEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
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

// ============================================================================
// AUDIO / VIDEO TOGGLE SWITCH
// ============================================================================
@Composable
fun AudioVideoToggleSwitch(
    isVideoMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Option 1: Audio / Lagu
        val audioBg = if (!isVideoMode) Color.White.copy(alpha = 0.22f) else Color.Transparent
        val audioTint = if (!isVideoMode) Color.White else TextMuted
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(audioBg)
                .clickable { onToggle(false) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Audio",
                tint = audioTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Lagu",
                color = audioTint,
                fontSize = 12.sp,
                fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Medium
            )
        }

        // Option 2: Video
        val videoBg = if (isVideoMode) AccentEmerald.copy(alpha = 0.35f) else Color.Transparent
        val videoTint = if (isVideoMode) Color.White else TextMuted
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(videoBg)
                .clickable { onToggle(true) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video",
                tint = videoTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Video",
                color = videoTint,
                fontSize = 12.sp,
                fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ============================================================================
// PLAYER VIDEO SURFACE
// ============================================================================
@Composable
fun PlayerVideoSurface(
    streamUrl: String?,
    isLoading: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    track: Track,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDoubleTapHeart by remember(track.id) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
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
        if (streamUrl.isNullOrBlank() || isLoading) {
            // Loading video state with cover backdrop
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.35f }
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = AccentEmerald,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Menghubungkan video klip...",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val context = LocalContext.current
            val exoPlayer = remember(streamUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(streamUrl))
                    volume = 0f // Muted so audio foreground service handles music without echo
                    repeatMode = Player.REPEAT_MODE_ALL
                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    prepare()
                }
            }

            DisposableEffect(exoPlayer) {
                onDispose {
                    exoPlayer.release()
                }
            }

            LaunchedEffect(exoPlayer, isPlaying) {
                exoPlayer.playWhenReady = isPlaying
            }

            // Keep in sync with audio position
            LaunchedEffect(exoPlayer, currentPositionMs) {
                val duration = exoPlayer.duration
                if (duration > 0) {
                    val target = currentPositionMs % duration
                    if (kotlin.math.abs(exoPlayer.currentPosition - target) > 1000) {
                        exoPlayer.seekTo(target.coerceIn(0, duration))
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        DoubleTapHeartOverlay(
            show = showDoubleTapHeart,
            onAnimationEnd = { showDoubleTapHeart = false }
        )
    }
}
