package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.network.ImportedPlaylistResult
import com.rafliadipratama.musiceuy.data.network.PlaylistImporter

/**
 * Spotify Playlist Importer Plugin.
 * Translates public Spotify playlist embeds into local playlists without requiring Spotify Premium.
 * GitHub Repo: https://github.com/KRTirtho/Spotube
 */
class SpotifyImporterPlugin(
    private val playlistImporter: PlaylistImporter,
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "spotify_importer"
    override val name: String = "Spotify Playlist Importer"
    override val description: String = "Ekstrak & sinkronisasi playlist publik Spotify ke koleksi MusicEuy tanpa akun Premium"
    override val version: String = "1.0.0"
    override val author: String = "Spotube Open Spec"
    override val category: String = "Alat & Impor"
    override val githubUrl: String = "https://github.com/KRTirtho/Spotube"
    override val isBuiltIn: Boolean = true

    suspend fun importPlaylist(url: String, customName: String? = null): ImportedPlaylistResult {
        if (!isEnabled) {
            throw IllegalStateException("Plugin Spotify Importer sedang dinonaktifkan di menu Ekstensi.")
        }
        return playlistImporter.importFromUrl(url, customName)
    }
}
