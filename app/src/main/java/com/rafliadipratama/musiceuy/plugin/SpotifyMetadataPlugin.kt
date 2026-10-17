package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.data.network.SpotifyPublicSource

/**
 * Spotify Public Metadata Plugin.
 * Provides official Spotify charts and playlists (Top 50 Indonesia, Viral, Pop Hits)
 * without requiring any Spotify account or user login.
 * Audio playback is automatically resolved via YouTubeMusicPlugin.
 */
class SpotifyMetadataPlugin(
    private val spotifySource: SpotifyPublicSource = SpotifyPublicSource(),
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "spotify_public"
    override val name: String = "Spotify Charts & Playlists"
    override val description: String = "Metadata tangga lagu resmi Spotify (Top 50 Indonesia, Global Hits, Viral) tanpa login. Audio di-stream via YouTube Music."
    override val version: String = "1.0.0"
    override val author: String = "Spotify Public & MusicEuy"
    override val isBuiltIn: Boolean = true

    override suspend fun search(query: String): List<Track> {
        if (!isEnabled) return emptyList()

        val lower = query.lowercase().trim()

        // 1. Direct Spotify playlist link or URI
        if (lower.contains("open.spotify.com/playlist") || lower.startsWith("spotify:playlist")) {
            return spotifySource.fetchPlaylist(query)
        }

        // 2. Query for Indonesian charts or top 50
        if (lower.contains("top 50") || lower.contains("spotify") || lower.contains("tangga lagu") || lower.contains("viral spotify")) {
            return spotifySource.getTop50Indonesia()
        }

        // 3. Today's Top Hits
        if (lower.contains("top hits") || lower.contains("global hits")) {
            return spotifySource.getTodaysTopHits()
        }

        // 4. Chill hits
        if (lower.contains("chill")) {
            return spotifySource.getChillHits()
        }

        return emptyList()
    }

    override suspend fun resolveStream(track: Track): String? {
        // Pure metadata plugin (same as Spotube's Spotify plugin).
        // Audio stream resolution is handled by YouTubeMusicPlugin.
        return null
    }

    /**
     * Direct accessor for Top 50 Indonesia
     */
    suspend fun getTop50Indonesia(): List<Track> {
        return spotifySource.getTop50Indonesia()
    }

    /**
     * Direct accessor for Today's Top Hits
     */
    suspend fun getTodaysTopHits(): List<Track> {
        return spotifySource.getTodaysTopHits()
    }
}
