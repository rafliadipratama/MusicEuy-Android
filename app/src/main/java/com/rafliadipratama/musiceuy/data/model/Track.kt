package com.rafliadipratama.musiceuy.data.model

data class Artist(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val isVerified: Boolean = true
)

data class Track(
    val id: String,
    val title: String,
    val artist: Artist,
    val albumTitle: String? = null,
    val durationSec: Int = 0,
    val audioUrl: String,
    val coverUrl: String,
    val isLiked: Boolean = false,
    val isVerified: Boolean = true,
    val isDownloaded: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val playCount: String? = null,
    val sourceLabel: String = "Lagu Resmi"
) {
    val durationFormatted: String
        get() {
            val mins = durationSec / 60
            val secs = durationSec % 60
            return String.format("%02d:%02d", mins, secs)
        }

    val fileSizeFormatted: String
        get() {
            if (fileSizeBytes <= 0) return ""
            val mb = fileSizeBytes.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }
}
