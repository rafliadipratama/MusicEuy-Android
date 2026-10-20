package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.soundcloud.extractors.SoundcloudStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * SoundCloud Audio Plugin powered by NewPipeExtractor.
 * Extracts independent music, remixes, DJ sets, and podcasts directly from SoundCloud.
 * GitHub Repo: https://github.com/TeamNewPipe/NewPipeExtractor
 */
class SoundCloudPlugin(
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "soundcloud"
    override val name: String = "SoundCloud Audio Stream"
    override val description: String = "Stream lagu indie, mixtape, EDM, remix, dan DJ sets langsung dari platform SoundCloud"
    override val version: String = "1.0.0"
    override val author: String = "Team NewPipe"
    override val category: String = "Audio Stream"
    override val githubUrl: String = "https://github.com/TeamNewPipe/NewPipeExtractor"
    override val isBuiltIn: Boolean = true

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (!isEnabled || query.isBlank()) return@withContext emptyList()
        try {
            val searchExtractor = ServiceList.SoundCloud.getSearchExtractor(query.trim())
            searchExtractor.fetchPage()
            val items = searchExtractor.initialPage?.items ?: emptyList()
            items.filterIsInstance<StreamInfoItem>().map { item ->
                val thumbUrl = item.thumbnails?.firstOrNull()?.url ?: ""
                Track(
                    id = "sc:${item.url}",
                    title = item.name ?: "Unknown Track",
                    artist = Artist(
                        id = "sc_art_${item.uploaderName?.hashCode() ?: 0}",
                        name = item.uploaderName ?: "SoundCloud Artist",
                        avatarUrl = thumbUrl.ifBlank { null },
                        isVerified = true
                    ),
                    durationSec = item.duration.toInt().coerceAtLeast(0),
                    audioUrl = "sc:${item.url}",
                    coverUrl = thumbUrl,
                    sourceLabel = "SoundCloud"
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun resolveStream(track: Track): String? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null
        val targetUrl = when {
            track.id.startsWith("sc:") -> track.id.removePrefix("sc:")
            track.audioUrl.startsWith("sc:") -> track.audioUrl.removePrefix("sc:")
            track.audioUrl.contains("soundcloud.com") -> track.audioUrl
            else -> null
        } ?: return@withContext null

        try {
            val extractor = ServiceList.SoundCloud.getStreamExtractor(targetUrl) as? SoundcloudStreamExtractor ?: return@withContext null
            extractor.fetchPage()
            extractor.audioStreams?.maxByOrNull { it.bitrate }?.content
        } catch (_: Exception) {
            null
        }
    }
}
