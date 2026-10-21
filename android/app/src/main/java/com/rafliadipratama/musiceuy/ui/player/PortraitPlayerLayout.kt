package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.lyrics.LyricRow
import com.rafliadipratama.musiceuy.lyrics.LyricsSearchMatch
import com.rafliadipratama.musiceuy.ui.components.LyricsSearchBar
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary

@Composable
internal fun PortraitPlayerLayout(
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
    onToggleVideoMode: (Boolean) -> Unit = {},
    queueActions: QueuePlayerActions
) {
    val dominantColor = uiState.dominantColor?.let { Color(it.toInt()) } ?: AccentEmerald
    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = if (isTablet) 560.dp else 1000.dp)
            .padding(horizontal = 24.dp, vertical = 10.dp),
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
                    text = if (uiState.isQueueVisible) "ANTREAN" else if (uiState.isLyricsVisible) "LIRIK LAGU" else "SEDANG MEMUTAR",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = if (uiState.isLyricsVisible) track.title else track.artist.name,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isLyricsVisible && uiState.lyrics.hasSyncedLyrics) {
                    IconButton(
                        onClick = onToggleSearch,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari Lirik",
                            tint = if (isSearchOpen) dominantColor else TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenLyricsOffset,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Sinkronisasi Waktu Lirik",
                            tint = if (uiState.lyricsOffsetMs != 0L) dominantColor else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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

                    // Indicator if sleep timer or custom lyrics offset is active
                    val isTimerActive = uiState.sleepTimerRemainingSeconds != null || uiState.isSleepTimerEndOfTrack
                    val isOffsetActive = uiState.lyricsOffsetMs != 0L
                    if (isTimerActive || isOffsetActive) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dominantColor)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }

        // --- 1.2. Floating In-Document Search Bar (Apple Notes UX) or Audio/Video Switch ---
        if (uiState.isLyricsVisible && isSearchOpen) {
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
                    .padding(vertical = 4.dp)
            )
        } else {
            // --- 1.5. Audio / Video Switch Pill ---
            AudioVideoToggleSwitch(
                isVideoMode = uiState.isVideoMode,
                onToggle = onToggleVideoMode
            )
        }


        PortraitPlayerStage(
            track = track, uiState = uiState, currentMs = currentMs,
            lyricsListState = lyricsListState, rows = rows, rowOfLine = rowOfLine,
            staggerLags = staggerLags, searchMatches = searchMatches, currentMatchIndex = currentMatchIndex,
            actions = queueActions, onOpenArtist = onOpenArtist, onToggleLike = onToggleLike,
            onDownload = onDownload, onSeekToLine = onSeekToLine,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        PlaybackSeekbar(currentMs, durationMs, isUserSeeking, seekProgress, uiState.isPlaying,
            onSeekChange, onSeekFinished)
        Spacer(Modifier.height(12.dp))
        PrimaryControlsRow(isPlaying = uiState.isPlaying, onPrevious = onPrevious,
            onTogglePlayPause = onTogglePlayPause, onNext = onNext)
        Spacer(Modifier.height(14.dp))
        BottomUtilityRow(uiState.isLyricsVisible, uiState.isQueueVisible, onToggleLyrics, onToggleQueue)
        Spacer(Modifier.height(6.dp))
    }
}
