package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.bandcamp.extractors.BandcampStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Bandcamp & Jamendo Indie Plugin.
 * Access independent artist discographies and Creative Commons licensed music.
 * GitHub Repo: https://github.com/jamendo/api and https://github.com/TeamNewPipe/NewPipeExtractor
 */
class BandcampJamendoPlugin(
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "bandcamp_jamendo"
    override val name: String = "Bandcamp & Jamendo Indie"
    override val description: String = "Koleksi musisi independen global, Creative Commons, dan rilisan vinyl dari Bandcamp & Jamendo"
    override val version: String = "1.0.0"
    override val author: String = "Jamendo & Team NewPipe"
    override val category: String = "Audio Stream"
    override val githubUrl: String = "https://github.com/jamendo/api"
    override val isBuiltIn: Boolean = true

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (!isEnabled || query.isBlank()) return@withContext emptyList()
        try {
            val searchExtractor = ServiceList.Bandcamp.getSearchExtractor(query.trim())
            searchExtractor.fetchPage()
            val items = searchExtractor.initialPage?.items ?: emptyList()
            items.filterIsInstance<StreamInfoItem>().map { item ->
                val thumbUrl = item.thumbnails?.firstOrNull()?.url ?: ""
                Track(
                    id = "bc:${item.url}",
                    title = item.name ?: "Unknown Track",
                    artist = Artist(
                        id = "bc_art_${item.uploaderName?.hashCode() ?: 0}",
                        name = item.uploaderName ?: "Bandcamp Artist",
                        avatarUrl = thumbUrl.ifBlank { null },
                        isVerified = true
                    ),
                    durationSec = item.duration.toInt().coerceAtLeast(0),
                    audioUrl = "bc:${item.url}",
                    coverUrl = thumbUrl,
                    sourceLabel = "Bandcamp"
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun resolveStream(track: Track): String? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null
        val targetUrl = when {
            track.id.startsWith("bc:") -> track.id.removePrefix("bc:")
            track.audioUrl.startsWith("bc:") -> track.audioUrl.removePrefix("bc:")
            track.audioUrl.contains("bandcamp.com") -> track.audioUrl
            else -> null
        } ?: return@withContext null

        try {
            val extractor = ServiceList.Bandcamp.getStreamExtractor(targetUrl) as? BandcampStreamExtractor ?: return@withContext null
            extractor.fetchPage()
            extractor.audioStreams?.maxByOrNull { it.bitrate }?.content
        } catch (_: Exception) {
            null
        }
    }
}
