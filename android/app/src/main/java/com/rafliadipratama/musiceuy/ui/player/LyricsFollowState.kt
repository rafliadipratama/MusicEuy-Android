package com.rafliadipratama.musiceuy.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rafliadipratama.musiceuy.lyrics.LyricRow
import com.rafliadipratama.musiceuy.lyrics.LyricsSearchEngine
import com.rafliadipratama.musiceuy.lyrics.buildLyricRows
import com.rafliadipratama.musiceuy.lyrics.findActiveLyricIndex
import com.rafliadipratama.musiceuy.ui.components.LyricsRecenterDurationMs
import com.rafliadipratama.musiceuy.ui.components.LyricsRecenterEasing
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun rememberLyricsFollow(
    syncedLines: List<com.rafliadipratama.musiceuy.data.model.LyricLine>,
    trackId: String,
    currentMs: Long,
    offsetMs: Long,
    lyricsVisible: Boolean,
    queueVisible: Boolean
): LyricsFollowState {
    val synced = syncedLines
    val rows = remember(synced) { buildLyricRows(synced) }
    val rowOfLine = remember(rows, synced) {
        IntArray(synced.size).also { map ->
            rows.forEachIndexed { rowIndex, row ->
                if (row is LyricRow.Line) map[row.index] = rowIndex
            }
        }
    }

    val lyricsListState = rememberLazyListState()
    val searchOpen = rememberSaveable { mutableStateOf(false) }
    val query = rememberSaveable { mutableStateOf("") }
    val matchIndex = rememberSaveable { mutableIntStateOf(0) }
    val staggerLags = remember(synced) { androidx.compose.runtime.mutableStateMapOf<Int, Float>() }
    val state = remember(synced, lyricsListState) {
        LyricsFollowState(synced, lyricsListState, rows, rowOfLine, staggerLags, searchOpen, query, matchIndex)
    }
    val coroutineScope = rememberCoroutineScope()
    with(state) {
        LaunchedEffect(searchMatches) {
            if (currentMatchIndex >= searchMatches.size) currentMatchIndex = (searchMatches.size - 1).coerceAtLeast(0)
        }
        BackHandler(enabled = isSearchOpen && !queueVisible) {
            isSearchOpen = false
            searchQuery = ""
            followPlayback = true
        }

        val density = LocalDensity.current
        var isAutoScrolling by remember { mutableStateOf(false) }

        // Trailing stagger lag state map for Apple Music-style fluid waterfall cascade
        var staggerAnchorRow by remember(synced) { androidx.compose.runtime.mutableIntStateOf(-1) }
        val staggerVelocities = remember(synced) { HashMap<Int, Float>() }

        LaunchedEffect(staggerLags) {
            while (true) {
                snapshotFlow { staggerLags.isNotEmpty() }.first { it }
                var lastNanos = 0L
                while (staggerLags.isNotEmpty()) {
                    withFrameNanos { now ->
                        val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.064f)
                        lastNanos = now
                        val anchor = staggerAnchorRow
                        val done = ArrayList<Int>()
                        for ((row, lag) in staggerLags.toMap()) {
                            val k = (row - anchor).coerceIn(1, 9)
                            // Critically damped spring toward 0, exact solution (smooth at any refresh rate)
                            val omega = 1000f / (55f * k)
                            val v = staggerVelocities[row] ?: 0f
                            val e = kotlin.math.exp(-omega * dt)
                            val tmp = (v + omega * lag) * dt
                            val nextLag = (lag + tmp) * e
                            val nextV = (v - omega * tmp) * e
                            if (kotlin.math.abs(nextLag) < 0.3f && kotlin.math.abs(nextV) < 4f) {
                                done += row
                            } else {
                                staggerLags[row] = nextLag
                                staggerVelocities[row] = nextV
                            }
                        }
                        done.forEach {
                            staggerLags.remove(it)
                            staggerVelocities.remove(it)
                        }
                    }
                }
            }
        }

        scrollToMatch = { matchIdx ->
            val match = searchMatches.getOrNull(matchIdx)
            if (match != null) {
                val targetRow = rowOfLine.getOrNull(match.lineIndex) ?: match.lineIndex
                followPlayback = false
                coroutineScope.launch {
                    val layoutInfo = lyricsListState.layoutInfo
                    val minOffsetPx = with(density) { 72.dp.toPx() }
                    val restingOffset = maxOf((layoutInfo.viewportSize.height * 0.055f).roundToInt(), minOffsetPx.roundToInt())
                    val visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetRow }
                    try {
                        isAutoScrolling = true
                        if (visible != null) {
                            val current = visible.offset - layoutInfo.viewportStartOffset
                            val distance = (current - restingOffset).toFloat()
                            lyricsListState.scroll {
                                var previous = 0f
                                androidx.compose.animation.core.animate(
                                    initialValue = 0f,
                                    targetValue = distance,
                                    animationSpec = tween(
                                        durationMillis = 650,
                                        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
                                    )
                                ) { value, _ ->
                                    val delta = value - previous
                                    val consumed = scrollBy(delta)
                                    previous += consumed
                                }
                            }
                        } else {
                            lyricsListState.animateScrollToItem(targetRow, -restingOffset)
                        }
                    } finally {
                        isAutoScrolling = false
                    }
                }
            }
        }

        val activeIndex = remember(currentMs, synced, offsetMs) {
            if (synced.isNotEmpty()) {
                val adjustedMs = currentMs + offsetMs
                findActiveLyricIndex(synced, adjustedMs)
            } else -1
        }

        // Only actual user gestures (drag/fling) suspend auto-follow; programmatic auto-scroll does NOT suspend followPlayback
        val userScrolling by remember {
            derivedStateOf { lyricsListState.isScrollInProgress && !isAutoScrolling }
        }

        LaunchedEffect(userScrolling) {
            if (userScrolling) {
                followPlayback = false
            } else if (!isSearchOpen) {
                delay(3500L)
                followPlayback = true
            }
        }

        // Auto-scroll synchronized lyrics with Apple Music-grade fluid physics, stagger cascade, and CubicBezierEasing
        LaunchedEffect(activeIndex, lyricsVisible, followPlayback) {
            if (lyricsVisible && followPlayback && synced.isNotEmpty()) {
                val targetLine = if (activeIndex in synced.indices) {
                    activeIndex
                } else if (activeIndex >= synced.size) {
                    synced.lastIndex
                } else {
                    0 // Rest on first line during intro
                }
                val targetRow = rowOfLine.getOrNull(targetLine) ?: targetLine

                val layoutInfo = lyricsListState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height
                if (viewportHeight > 0) {
                    val minOffsetPx = with(density) { 72.dp.toPx() }
                    val restingOffset = maxOf((viewportHeight * 0.055f).roundToInt(), minOffsetPx.roundToInt())
                    val visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetRow }

                    try {
                        isAutoScrolling = true
                        if (visible != null) {
                            val current = visible.offset - layoutInfo.viewportStartOffset
                            // Account for collapsing interlude dot row above the target to avoid overshoot
                            val collapsing = layoutInfo.visibleItemsInfo
                                .filter { it.index < targetRow && rows.getOrNull(it.index) is LyricRow.Interlude }
                                .sumOf { it.size }
                            val distance = (current - restingOffset - collapsing).toFloat()

                            staggerAnchorRow = targetRow
                            lyricsListState.scroll {
                                var previous = 0f
                                androidx.compose.animation.core.animate(
                                    initialValue = 0f,
                                    targetValue = distance,
                                    animationSpec = tween(
                                        durationMillis = LyricsRecenterDurationMs,
                                        easing = LyricsRecenterEasing
                                    )
                                ) { value, _ ->
                                    val delta = value - previous
                                    val consumed = scrollBy(delta)
                                    previous += consumed
                                    if (consumed != 0f) {
                                        for (item in lyricsListState.layoutInfo.visibleItemsInfo) {
                                            if (item.index > targetRow) {
                                                staggerLags[item.index] = (staggerLags[item.index] ?: 0f) + consumed
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            lyricsListState.animateScrollToItem(targetRow, -restingOffset)
                        }
                    } finally {
                        isAutoScrolling = false
                    }
                } else {
                    try {
                        isAutoScrolling = true
                        lyricsListState.animateScrollToItem(targetRow, -180)
                    } finally {
                        isAutoScrolling = false
                    }
                }
            }
        }

        // Reset lyrics scroll to top whenever track changes
        LaunchedEffect(trackId) {
            followPlayback = true
            isSearchOpen = false
            searchQuery = ""
            lyricsListState.scrollToItem(0)
        }

    }
    return state
}

internal class LyricsFollowState(
    private val lines: List<com.rafliadipratama.musiceuy.data.model.LyricLine>,
    val lyricsListState: androidx.compose.foundation.lazy.LazyListState,
    val rows: List<LyricRow>,
    val rowOfLine: IntArray,
    val staggerLags: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Float>,
    searchOpen: androidx.compose.runtime.MutableState<Boolean>,
    query: androidx.compose.runtime.MutableState<String>,
    matchIndex: androidx.compose.runtime.MutableIntState
) {
    var isSearchOpen by searchOpen
    var searchQuery by query
    var currentMatchIndex by matchIndex
    var followPlayback by mutableStateOf(true)
    val searchMatches by derivedStateOf {
        if (searchQuery.isBlank()) emptyList() else LyricsSearchEngine.search(lines, searchQuery)
    }
    var scrollToMatch: (Int) -> Unit = {}

    fun toggleSearch() {
        isSearchOpen = !isSearchOpen
        if (isSearchOpen) followPlayback = false else searchQuery = ""
    }

    fun updateQuery(value: String) {
        searchQuery = value
        currentMatchIndex = 0
        if (value.isNotBlank()) scrollToMatch(0)
    }

    fun nextMatch() {
        if (searchMatches.isEmpty()) return
        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
        scrollToMatch(currentMatchIndex)
    }

    fun previousMatch() {
        if (searchMatches.isEmpty()) return
        currentMatchIndex = if (currentMatchIndex == 0) searchMatches.lastIndex else currentMatchIndex - 1
        scrollToMatch(currentMatchIndex)
    }

    fun closeSearch() {
        isSearchOpen = false
        searchQuery = ""
        followPlayback = true
    }
}
