package com.rafliadipratama.musiceuy.data.model

data class SyllableWord(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val words: List<SyllableWord> = emptyList(),
    val agent: String? = null,
    val isBackground: Boolean = false,
    val translation: String? = null
)

data class Lyrics(
    val plainLyrics: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList()
) {
    val hasSyncedLyrics: Boolean
        get() = syncedLyrics.isNotEmpty()
}
