package com.rafliadipratama.musiceuy.data.model

import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val coverUrl: String? = null,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
