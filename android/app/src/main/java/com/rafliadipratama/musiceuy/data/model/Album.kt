package com.rafliadipratama.musiceuy.data.model

data class Album(
    val id: String, // YouTube Music browseId, e.g. MPREb_ofw3blsbYvx
    val title: String,
    val artistName: String,
    val year: String = "",
    val coverUrl: String = "",
    val tracks: List<Track> = emptyList()
)
