package com.rafliadipratama.musiceuy.lyrics

import com.rafliadipratama.musiceuy.data.model.LyricLine
import com.rafliadipratama.musiceuy.data.model.SyllableWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSystemTest {

    @Test
    fun `active line changes exactly at timestamp including seek backwards`() {
        val lines = listOf(
            LyricLine(1000L, "First"),
            LyricLine(2000L, "Second", listOf(SyllableWord("Second", 2000L, 2800L)))
        )
        assertEquals(-1, findActiveLyricIndex(emptyList(), 1000L))
        assertEquals(-1, findActiveLyricIndex(lines, 999L))
        assertEquals(0, findActiveLyricIndex(lines, 1000L))
        assertEquals(0, findActiveLyricIndex(lines, 1999L))
        assertEquals(1, findActiveLyricIndex(lines, 2000L))
        assertEquals(1, findActiveLyricIndex(lines, 2799L))
        assertEquals(2, findActiveLyricIndex(lines, 2800L))
        assertEquals(0, findActiveLyricIndex(lines, 1500L))
    }

    @Test
    fun `findMatchRanges finds multiple case-insensitive occurrences`() {
        val text = "Never gonna give you up, never gonna let you down"
        val ranges = LyricsSearchEngine.findMatchRanges(text, "never")
        assertEquals(2, ranges.size)
        assertEquals(0 until 5, ranges[0])
        assertEquals(25 until 30, ranges[1])
    }

    @Test
    fun `findMatchRanges handles empty or invalid inputs gracefully`() {
        assertTrue(LyricsSearchEngine.findMatchRanges("", "test").isEmpty())
        assertTrue(LyricsSearchEngine.findMatchRanges("test", "").isEmpty())
        assertTrue(LyricsSearchEngine.findMatchRanges("abc", "abcdef").isEmpty())
    }

    @Test
    fun `search matches across multiple lines and captures correct line indices`() {
        val lines = listOf(
            LyricLine(timestampMs = 1000L, text = "Hello darkness my old friend"),
            LyricLine(timestampMs = 4000L, text = "I've come to talk with you again"),
            LyricLine(timestampMs = 8000L, text = "Because a vision softly creeping"),
            LyricLine(timestampMs = 12000L, text = "Left its seeds while I was sleeping")
        )

        val matches = LyricsSearchEngine.search(lines, "with")
        assertEquals(1, matches.size)
        assertEquals(1, matches[0].lineIndex)
        assertEquals(listOf(18 until 22), matches[0].matchRanges)

        val multiMatches = LyricsSearchEngine.search(lines, "e")
        assertEquals(4, multiMatches.size)
    }

    @Test
    fun `buildLyricRows inserts instrumental interlude rows for intro and gaps`() {
        // Song with 8s intro, 2s gap between line 1 & 2, and 6s gap between line 2 & 3
        val lines = listOf(
            LyricLine(
                timestampMs = 8000L,
                text = "First line after long intro",
                words = listOf(SyllableWord("First", 8000L, 8500L), SyllableWord("line", 8500L, 10000L))
            ),
            LyricLine(
                timestampMs = 12000L,
                text = "Second line short gap",
                words = listOf(SyllableWord("Second", 12000L, 12500L), SyllableWord("line", 12500L, 13000L))
            ),
            LyricLine(
                timestampMs = 21000L, // 8s gap after line 2 (13000 to 21000)
                text = "Third line after guitar solo",
                words = listOf(SyllableWord("Third", 21000L, 21500L), SyllableWord("line", 21500L, 23000L))
            )
        )

        val rows = buildLyricRows(lines)

        // Expected:
        // 1. Intro interlude (0..8000)
        // 2. Line 0
        // 3. Line 1 (gap was only 2s < 4s, so no interlude)
        // 4. Interlude after line 1 (13000..21000)
        // 5. Line 2
        assertEquals(5, rows.size)

        assertTrue(rows[0] is LyricRow.Interlude)
        val intro = rows[0] as LyricRow.Interlude
        assertEquals(-1, intro.afterIndex)
        assertEquals(0L, intro.startMs)
        assertEquals(8000L, intro.endMs)

        assertTrue(rows[1] is LyricRow.Line)
        assertEquals(0, (rows[1] as LyricRow.Line).index)

        assertTrue(rows[2] is LyricRow.Line)
        assertEquals(1, (rows[2] as LyricRow.Line).index)

        assertTrue(rows[3] is LyricRow.Interlude)
        val solo = rows[3] as LyricRow.Interlude
        assertEquals(1, solo.afterIndex)
        assertEquals(13000L, solo.startMs)
        assertEquals(21000L, solo.endMs)

        assertTrue(rows[4] is LyricRow.Line)
        assertEquals(2, (rows[4] as LyricRow.Line).index)
    }

    @Test
    fun `agent detection correctly classifies multi-voice duets`() {
        val lines = listOf(
            LyricLine(timestampMs = 1000L, text = "Line 1", agent = "v1"),
            LyricLine(timestampMs = 4000L, text = "Line 2", agent = "v2")
        )
        val hasMultipleVoices = lines.filter { !it.isBackground }
            .map { it.agent ?: "v1" }
            .distinct()
            .size > 1
        assertTrue(hasMultipleVoices)
    }
}
