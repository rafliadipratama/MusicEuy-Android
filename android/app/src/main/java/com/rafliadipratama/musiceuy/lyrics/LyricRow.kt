package com.rafliadipratama.musiceuy.lyrics

import com.rafliadipratama.musiceuy.data.model.LyricLine

/**
 * Alignment for lyric lines and countdown interlude dots.
 * Supports duets (agent:v1 on START, agent:v2 on END, chorus/background on CENTER).
 */
enum class LyricsAlignment {
    START,
    CENTER,
    END
}

/**
 * One row of the synchronized lyrics list.
 * Instrumental breaks are first-class rows rather than conditionally rendered inner content,
 * preventing layout shifting and list jumping when interludes appear or collapse.
 */
sealed interface LyricRow {
    val key: Long

    data class Line(val index: Int, val line: LyricLine) : LyricRow {
        override val key get() = index.toLong()
    }

    data class Interlude(val afterIndex: Int, val startMs: Long, val endMs: Long) : LyricRow {
        override val key get() = -1_000_000L - afterIndex
    }
}

/** Minimum gap duration in milliseconds to be considered an instrumental solo / interlude. */
const val MinInterludeGapMs = 4000L

/**
 * Calculates when a line finishes: the end of its last word if word-synced,
 * or the start of the next line, or a sensible fallback duration.
 */
fun lineEndMs(lines: List<LyricLine>, index: Int): Long {
    val line = lines.getOrNull(index) ?: return 0L
    line.words.lastOrNull()?.let { return it.endMs }
    lines.getOrNull(index + 1)?.let { return it.timestampMs }
    return line.timestampMs + 3500L
}

/**
 * Constructs rows for LazyColumn by interleaving lyric lines with instrumental breaks.
 */
fun buildLyricRows(lines: List<LyricLine>): List<LyricRow> {
    if (lines.isEmpty()) return emptyList()
    val rows = ArrayList<LyricRow>(lines.size + 4)

    // Opening instrumental intro
    if (lines.first().timestampMs >= MinInterludeGapMs) {
        rows.add(LyricRow.Interlude(afterIndex = -1, startMs = 0L, endMs = lines.first().timestampMs))
    }

    for (index in lines.indices) {
        val current = lines[index]
        rows.add(LyricRow.Line(index, current))
        val next = lines.getOrNull(index + 1) ?: continue
        val gapStart = lineEndMs(lines, index)
        if (next.timestampMs - gapStart >= MinInterludeGapMs) {
            rows.add(LyricRow.Interlude(afterIndex = index, startMs = gapStart, endMs = next.timestampMs))
        }
    }
    return rows
}

fun findActiveLyricIndex(lines: List<LyricLine>, currentMs: Long): Int {
    if (lines.isEmpty() || currentMs < lines.first().timestampMs) return -1
    if (currentMs >= lineEndMs(lines, lines.lastIndex)) return lines.size
    var low = 0
    var high = lines.lastIndex
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (lines[middle].timestampMs <= currentMs) low = middle + 1 else high = middle - 1
    }
    return high
}
