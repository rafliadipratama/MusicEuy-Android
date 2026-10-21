package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.rafliadipratama.musiceuy.data.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFollowStateTest {
    private fun state() = LyricsFollowState(
        lines = listOf(LyricLine(1000, "Hello"), LyricLine(2000, "Hello again")),
        lyricsListState = LazyListState(), rows = emptyList(), rowOfLine = intArrayOf(),
        staggerLags = mutableStateMapOf(), searchOpen = mutableStateOf(false),
        query = mutableStateOf(""), matchIndex = mutableIntStateOf(0)
    )

    @Test
    fun `search navigation wraps and scrolls to the current match`() {
        val state = state()
        val visited = mutableListOf<Int>()
        state.scrollToMatch = { visited.add(it) }
        state.updateQuery("hello")
        state.nextMatch()
        state.nextMatch()
        state.previousMatch()
        assertEquals(listOf(0, 1, 0, 1), visited)
        assertEquals(1, state.currentMatchIndex)
        state.updateQuery("missing")
        state.nextMatch()
        state.previousMatch()
        assertEquals(0, state.currentMatchIndex)
    }

    @Test
    fun `closing search clears query and resumes following playback`() {
        val state = state()
        state.toggleSearch()
        assertTrue(state.isSearchOpen)
        assertFalse(state.followPlayback)
        state.updateQuery("hello")
        state.closeSearch()
        assertFalse(state.isSearchOpen)
        assertEquals("", state.searchQuery)
        assertTrue(state.followPlayback)
    }
}
