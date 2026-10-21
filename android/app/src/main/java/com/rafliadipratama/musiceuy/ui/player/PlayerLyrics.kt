package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.lyrics.LyricRow
import com.rafliadipratama.musiceuy.lyrics.LyricsAlignment
import com.rafliadipratama.musiceuy.lyrics.LyricsSearchMatch
import com.rafliadipratama.musiceuy.lyrics.findActiveLyricIndex
import com.rafliadipratama.musiceuy.ui.components.GlowLyricsLine
import com.rafliadipratama.musiceuy.ui.components.LyricsInterludeDots
import com.rafliadipratama.musiceuy.ui.components.LyricsLoadingDots
import com.rafliadipratama.musiceuy.ui.components.LyricsRecenterEasing
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
internal fun SyncedLyricsView(
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    currentMs: Long,
    lyricsListState: androidx.compose.foundation.lazy.LazyListState,
    rows: List<LyricRow>,
    rowOfLine: IntArray,
    staggerLags: Map<Int, Float>,
    dominantColor: Color = AccentEmerald,
    searchMatches: List<LyricsSearchMatch> = emptyList(),
    currentMatchIndex: Int = 0,
    onSeekToLine: (Long) -> Unit = {}
) {
    if (uiState.isLoadingLyrics) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            LyricsLoadingDots(color = dominantColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Menyelaraskan lirik...",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else if (uiState.lyrics.hasSyncedLyrics) {
        val synced = uiState.lyrics.syncedLyrics
        val clock = rememberLyricsPlaybackClock(currentMs, uiState.isPlaying, uiState.currentTrack?.id)
        val adjustedPosition = remember(clock, uiState.lyricsOffsetMs) {
            derivedStateOf { clock.value + uiState.lyricsOffsetMs }
        }
        val activeIndex by remember(synced, adjustedPosition) {
            derivedStateOf { findActiveLyricIndex(synced, adjustedPosition.value) }
        }

        // Duet multi-voice alignment detection
        val useAgentAlignment = remember(synced) {
            synced.filter { !it.isBackground }
                .map { it.agent ?: "v1" }
                .distinct()
                .size > 1
        }

        fun alignmentFor(line: com.rafliadipratama.musiceuy.data.model.LyricLine): LyricsAlignment = when {
            !useAgentAlignment -> LyricsAlignment.START
            line.isBackground -> LyricsAlignment.CENTER
            line.agent == "v2" -> LyricsAlignment.END
            line.agent == "v1000" -> LyricsAlignment.CENTER
            else -> LyricsAlignment.START
        }

        val activeInterlude by remember(synced, rows, adjustedPosition) {
            derivedStateOf {
                val pos = adjustedPosition.value
                rows.firstOrNull {
                    it is LyricRow.Interlude && pos >= it.startMs &&
                        pos < it.endMs
                }?.let { (it as LyricRow.Interlude).afterIndex } ?: Int.MIN_VALUE
            }
        }
        val isInterludeActive = activeInterlude != Int.MIN_VALUE

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val listHeight = maxHeight

            // Viewport edge gradient fade mask (Apple Music style)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.08f to Color.Black,
                                0.72f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                LazyColumn(
                    state = lyricsListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = maxOf(listHeight * 0.055f, 72.dp),
                        bottom = listHeight * 0.55f
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    itemsIndexed(
                        items = rows,
                        key = { _, row -> row.key },
                        contentType = { _, row -> if (row is LyricRow.Interlude) "interlude" else "lyric" }
                    ) { listIndex, row ->
                        when (row) {
                            is LyricRow.Interlude -> {
                                val nextLine = synced.getOrNull(row.afterIndex + 1)
                                val nextAlignment = nextLine?.let { alignmentFor(it) } ?: LyricsAlignment.CENTER
                                LyricsInterludeDots(
                                    modifier = Modifier.graphicsLayer {
                                        translationY = staggerLags[listIndex] ?: 0f
                                    },
                                    startMs = row.startMs,
                                    endMs = row.endMs,
                                    positionProvider = { adjustedPosition.value },
                                    accentColor = dominantColor,
                                    visible = activeInterlude == row.afterIndex,
                                    alignment = nextAlignment,
                                    collapseEasing = LyricsRecenterEasing
                                )
                            }
                            is LyricRow.Line -> {
                                val index = row.index
                                val line = row.line
                                val isActive = index == activeIndex && !isInterludeActive
                                val distance = if (activeIndex >= 0) abs(index - activeIndex) else (index + 1)
                                val match = searchMatches.firstOrNull { it.lineIndex == index }
                                val isActiveMatch = searchMatches.getOrNull(currentMatchIndex)?.lineIndex == index
                                val lineAlignment = alignmentFor(line)

                                GlowLyricsLine(
                                    modifier = Modifier.graphicsLayer {
                                        translationY = staggerLags[listIndex] ?: 0f
                                    },
                                    text = line.text,
                                    isActive = isActive,
                                    words = line.words,
                                    positionProvider = { adjustedPosition.value },
                                    distanceFromActive = distance,
                                    glowColor = dominantColor,
                                    alignment = lineAlignment,
                                    searchMatch = match,
                                    isActiveSearchMatch = isActiveMatch,
                                    translation = line.translation,
                                    onTap = { onSeekToLine((line.timestampMs - uiState.lyricsOffsetMs).coerceAtLeast(0L)) }
                                )
                            }
                        }
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
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
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
