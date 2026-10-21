package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.lyrics.LyricRow
import com.rafliadipratama.musiceuy.lyrics.LyricsSearchMatch
import com.rafliadipratama.musiceuy.ui.components.DoubleTapHeartOverlay
import com.rafliadipratama.musiceuy.ui.components.QueueBottomSheet
import com.rafliadipratama.musiceuy.ui.components.SmoothCornerShape
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState

@Composable
internal fun PortraitPlayerStage(
    track: Track,
    uiState: PlayerUiState,
    currentMs: Long,
    lyricsListState: LazyListState,
    rows: List<LyricRow>,
    rowOfLine: IntArray,
    staggerLags: Map<Int, Float>,
    searchMatches: List<LyricsSearchMatch>,
    currentMatchIndex: Int,
    actions: QueuePlayerActions,
    onOpenArtist: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val liked by rememberUpdatedState(track.id in uiState.likedTrackIds)
    val progress = animateFloatAsState(
        if (uiState.isQueueVisible) 1f else 0f,
        PlayerMotion.queueMorph,
        label = "queueArtworkMorph"
    )
    BoxWithConstraints(modifier) {
        val coverSize = minOf(maxWidth * 0.88f, (maxHeight - 112.dp).coerceAtLeast(64.dp))
        val coverX = (maxWidth - coverSize) / 2
        val coverY = ((maxHeight - 100.dp - coverSize) / 2).coerceAtLeast(0.dp)
        val infoY = (maxHeight - 88.dp).coerceAtLeast(76.dp)
        val p = progress.value
        val showArtwork = uiState.isQueueVisible || (!uiState.isLyricsVisible && !uiState.isVideoMode)

        if (uiState.isQueueVisible || p > 0.001f) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 88.dp, bottom = 8.dp)
                    .graphicsLayer { alpha = ((progress.value - 0.2f) / 0.8f).coerceIn(0f, 1f) },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QueueModeButtons(uiState, actions)
                QueueBottomSheet(
                    currentTrack = track, queue = uiState.queue, isPlaying = uiState.isPlaying,
                    onTrackClick = actions.playTrack, onRemoveTrack = actions.removeTrack,
                    onClearQueue = actions.clearQueue, onReorderUpcoming = actions.reorderUpcoming,
                    onMoveTrackToNext = actions.moveToNext, onDismiss = actions.closeQueue,
                    modifier = Modifier.weight(1f), embedded = true
                )
            }
        }
        if (!uiState.isQueueVisible) {
            Box(Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                if (uiState.isLyricsVisible) {
                    SyncedLyricsView(uiState, currentMs, lyricsListState, rows, rowOfLine, staggerLags,
                        dominantColor = uiState.dominantColor?.let { Color(it.toInt()) } ?: AccentEmerald,
                        searchMatches = searchMatches, currentMatchIndex = currentMatchIndex,
                        onSeekToLine = onSeekToLine)
                } else if (uiState.isVideoMode) {
                    PlayerVideoSurface(uiState.currentVideoStreamUrl, uiState.isLoadingVideo,
                        uiState.isPlaying, currentMs, track, track.id in uiState.likedTrackIds,
                        onToggleLike, Modifier.fillMaxWidth())
                }
            }
        }
        if (showArtwork || p > 0.001f) {
            val heart = remember(track.id) { mutableStateOf(false) }
            Box(
                modifier = Modifier.requiredSize(coverSize)
                    .graphicsLayer {
                        val fraction = progress.value
                        translationX = lerp(coverX, 0.dp, fraction).toPx()
                        translationY = lerp(coverY, 4.dp, fraction).toPx()
                        val scale = lerp(coverSize, 64.dp, fraction) / coverSize
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }.clip(SmoothCornerShape(20.dp))
                    .pointerInput(track.id) {
                        detectTapGestures(onDoubleTap = {
                            if (!liked) onToggleLike()
                            heart.value = true
                        })
                    }
            ) {
                AsyncImage(track.coverUrl, track.title, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
                DoubleTapHeartOverlay(heart.value, { heart.value = false })
            }
        }
        Box(
            modifier = Modifier.offset(x = lerp(0.dp, 76.dp, p), y = lerp(infoY, 8.dp, p))
                .width(lerp(maxWidth, maxWidth - 76.dp, p))
        ) {
            TrackInfoRow(track, uiState, onOpenArtist, onToggleLike, onDownload,
                compact = uiState.isQueueVisible, compactProgress = p)
        }
    }
}
