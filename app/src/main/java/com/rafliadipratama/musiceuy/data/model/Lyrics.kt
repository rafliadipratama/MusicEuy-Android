package com.rafliadipratama.musiceuy.data.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class Lyrics(
    val plainLyrics: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList()
) {
    val hasSyncedLyrics: Boolean
        get() = syncedLyrics.isNotEmpty()
}
