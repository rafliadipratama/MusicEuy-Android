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
    val sourceLabel: String = "YouTube Music Verified"
) {
    val durationFormatted: String
        get() {
            val mins = durationSec / 60
            val secs = durationSec % 60
            return String.format("%02d:%02d", mins, secs)
        }
}
