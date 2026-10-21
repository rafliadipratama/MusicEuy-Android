package com.rafliadipratama.musiceuy.lyrics

import com.rafliadipratama.musiceuy.data.model.LyricLine

/**
 * A single match occurrence within a lyric line, mirroring the Notes App architecture
 * documented in NOTES_SEARCH_AND_LYRIC_SYSTEM_UX.md.
 */
data class LyricsSearchMatch(
    val lineIndex: Int,
    val line: LyricLine,
    val matchRanges: List<IntRange> = emptyList()
)

/**
 * In-memory search engine for lyrics with case-insensitive token & substring scanning.
 * Extracts character offset ranges for character-level active/passive highlighting.
 */
object LyricsSearchEngine {

    fun search(lines: List<LyricLine>, rawQuery: String): List<LyricsSearchMatch> {
        val query = rawQuery.trim()
        if (query.isEmpty() || lines.isEmpty()) return emptyList()

        val results = ArrayList<LyricsSearchMatch>()
        for (index in lines.indices) {
            val line = lines[index]
            val ranges = findMatchRanges(line.text, query)
            if (ranges.isNotEmpty()) {
                results.add(
                    LyricsSearchMatch(
                        lineIndex = index,
                        line = line,
                        matchRanges = ranges
                    )
                )
            }
        }
        return results
    }

    fun findMatchRanges(text: String, query: String): List<IntRange> {
        if (text.isEmpty() || query.isEmpty() || query.length > text.length) return emptyList()
        val ranges = ArrayList<IntRange>()
        var startIndex = 0
        while (startIndex <= text.length - query.length) {
            val foundIndex = text.indexOf(query, startIndex, ignoreCase = true)
            if (foundIndex < 0) break
            ranges.add(foundIndex until (foundIndex + query.length))
            startIndex = foundIndex + maxOf(1, query.length)
        }
        return ranges
    }
}
