package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.ui.components.QueueBottomSheet
import com.rafliadipratama.musiceuy.ui.components.SmoothCornerShape
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary

@Composable
internal fun LandscapeQueueLayout(
    track: Track,
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    currentMs: Long,
    durationMs: Long,
    isUserSeeking: Boolean,
    seekProgress: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    actions: QueuePlayerActions,
    onShowOptions: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 960.dp)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = { actions.collapse() }) {
                Icon(Icons.Default.KeyboardArrowDown, "Tutup pemutar", tint = TextPrimary)
            }
            AsyncImage(
                model = track.coverUrl, contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(SmoothCornerShape(12.dp))
            )
            Box(Modifier.weight(1f)) {
                TrackInfoRow(track, uiState,
                    onOpenArtist = { actions.openArtist() },
                    onToggleLike = { actions.toggleLike() },
                    onDownload = { actions.download() }, compact = true)
            }
            IconButton(onClick = onShowOptions) {
                Icon(Icons.Default.MoreVert, "Opsi lagu", tint = TextPrimary)
            }
        }

        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QueueModeButtons(uiState, actions)
                QueuePlaybackControls(uiState, currentMs, durationMs, isUserSeeking, seekProgress,
                    onSeekChange, onSeekFinished, actions)
            }
            QueueBottomSheet(
                currentTrack = track, queue = uiState.queue, isPlaying = uiState.isPlaying,
                onTrackClick = actions.playTrack, onRemoveTrack = actions.removeTrack,
                onClearQueue = actions.clearQueue, onReorderUpcoming = actions.reorderUpcoming,
                onMoveTrackToNext = actions.moveToNext, onDismiss = actions.closeQueue,
                modifier = Modifier.weight(1f), embedded = true)
        }

        BottomUtilityRow(
            isLyricsVisible = false,  isQueueVisible = true,
            onToggleLyrics = actions.toggleLyrics,
            onToggleQueue = actions.toggleQueue)
    }
}

@Composable
private fun QueuePlaybackControls(
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    currentMs: Long, durationMs: Long, isUserSeeking: Boolean, seekProgress: Float,
    onSeekChange: (Float) -> Unit, onSeekFinished: () -> Unit, actions: QueuePlayerActions
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlaybackSeekbar(currentMs, durationMs, isUserSeeking, seekProgress, uiState.isPlaying,
            onSeekChange, onSeekFinished)
        PrimaryControlsRow(
            isPlaying = uiState.isPlaying,
            onPrevious = actions.previous,
            onTogglePlayPause = actions.togglePlayPause, onNext = actions.next
        )
    }
}
