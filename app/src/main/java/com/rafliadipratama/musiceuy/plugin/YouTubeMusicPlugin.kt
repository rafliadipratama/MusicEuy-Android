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
    override val category: String = "Audio Stream"
    override val githubUrl: String = "https://github.com/TeamNewPipe/NewPipeExtractor"
    override val isBuiltIn: Boolean = true

    override suspend fun search(query: String): List<Track> {
        if (!isEnabled) return emptyList()
        return streamSource.searchYouTubeMusic(query)
    }

    override suspend fun resolveStream(track: Track): String? {
        if (!isEnabled) return null

        var videoId: String? = null

        if (track.id.startsWith("yt:")) {
            videoId = track.id.removePrefix("yt:")
        } else if (track.audioUrl.startsWith("yt:")) {
            videoId = track.audioUrl.removePrefix("yt:")
        } else if (track.audioUrl.startsWith("http")) {
            return track.audioUrl
        }

        // If not a direct YouTube ID (e.g. from Spotify metadata),
        // search YouTube Music for verified song matching title + artist strictly
        if (videoId == null) {
            val query = "${track.artist.name} ${track.title}"
            android.util.Log.d("MusicEuy", "YouTubeMusicPlugin resolving track via YouTube: $query")
            val ytCandidates = streamSource.searchYouTubeMusic(query)
            val bestMatch = ytCandidates.firstOrNull { candidate ->
                streamSource.isStrictMatch(
                    expectedTitle = track.title,
                    expectedArtist = track.artist.name,
                    candidateTitle = candidate.title,
                    candidateArtist = candidate.artist.name
                )
            } ?: ytCandidates.firstOrNull()

            if (bestMatch != null) {
                videoId = bestMatch.id.removePrefix("yt:")
                android.util.Log.d("MusicEuy", "Matched to YouTube videoId=$videoId (title=${bestMatch.title})")
            }
        }

        if (videoId != null && !videoId.startsWith("http")) {
            return streamSource.resolveYouTubeAudioStream(videoId)
        }

        return null
    }
}
