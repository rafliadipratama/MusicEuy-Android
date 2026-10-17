package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Track

/**
 * Modular Music Stream Plugin contract inspired by Audion and Spotube.
 * Allows MusicEuy to decouple audio extraction, streaming sources, and metadata providers.
 */
interface MusicStreamPlugin {
    val id: String
    val name: String
    val description: String
    val version: String
    val author: String
    val isBuiltIn: Boolean
    var isEnabled: Boolean

    /**
     * Searches for tracks using this plugin's provider.
     */
    suspend fun search(query: String): List<Track>

    /**
     * Resolves an active, playable direct audio stream URL for the given track.
     * Returns null if the plugin cannot resolve this track.
     */
    suspend fun resolveStream(track: Track): String?
}
