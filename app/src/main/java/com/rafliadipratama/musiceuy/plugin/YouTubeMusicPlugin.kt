package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.data.network.MusicStreamSource

/**
 * Official YouTube Music Plugin powered by InnerTube WEB_REMIX API
 * and NewPipeExtractor v0.26.5 for direct high-bitrate audio stream extraction.
 */
class YouTubeMusicPlugin(
    private val streamSource: MusicStreamSource,
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "ytmusic"
    override val name: String = "YouTube Music Resolver"
    override val description: String = "Ekstraksi stream audio resmi YouTube Music (Opus 160-320kbps) dengan NewPipeExtractor v0.26.5"
    override val version: String = "1.2.0"
    override val author: String = "Team NewPipe & MusicEuy"
    override val isBuiltIn: Boolean = true

    override suspend fun search(query: String): List<Track> {
        if (!isEnabled) return emptyList()
        return streamSource.searchYouTubeMusic(query)
    }

    override suspend fun resolveStream(track: Track): String? {
        if (!isEnabled) return null
        val videoId = if (track.id.startsWith("yt:")) {
            track.id.removePrefix("yt:")
        } else if (track.audioUrl.startsWith("yt:")) {
            track.audioUrl.removePrefix("yt:")
        } else if (!track.audioUrl.startsWith("http")) {
            track.id
        } else {
            return track.audioUrl
        }
        return streamSource.resolveYouTubeAudioStream(videoId)
    }
}
