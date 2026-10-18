package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.data.network.MusicStreamSource

/**
 * Secondary JioSaavn Plugin for direct high-quality AAC streams (320kbps).
 * Protected by strict Indian language filtering and strict title/artist matching.
 */
class JioSaavnPlugin(
    private val streamSource: MusicStreamSource,
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "jiosaavn"
    override val name: String = "JioSaavn Direct Stream"
    override val description: String = "Penyedia audio langsung berkecepatan tinggi (320kbps AAC) dengan filter verifikasi ketat judul & artis"
    override val version: String = "1.0.0"
    override val author: String = "MusicEuy Community"
    override val category: String = "Audio Stream"
    override val githubUrl: String = "https://github.com/sumitkolhe/jiosaavn-api"
    override val isBuiltIn: Boolean = true

    override suspend fun search(query: String): List<Track> {
        if (!isEnabled) return emptyList()
        return streamSource.fetchSaavn(query)
    }

    override suspend fun resolveStream(track: Track): String? {
        if (!isEnabled) return null

        if (track.id.startsWith("js:") && track.audioUrl.startsWith("http")) {
            return track.audioUrl
        }

        // Search for strictly matched track
        val candidates = streamSource.fetchSaavn("${track.artist.name} ${track.title}", limit = 5)
        val matched = candidates.firstOrNull { candidate ->
            streamSource.isStrictMatch(
                expectedTitle = track.title,
                expectedArtist = track.artist.name,
                candidateTitle = candidate.title,
                candidateArtist = candidate.artist.name
            )
        }

        return if (matched != null && matched.audioUrl.startsWith("http")) {
            matched.audioUrl
        } else {
            null
        }
    }
}
