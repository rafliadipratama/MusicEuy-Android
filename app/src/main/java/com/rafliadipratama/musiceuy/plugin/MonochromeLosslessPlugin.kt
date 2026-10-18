package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Monochrome Lossless FLAC Audio Provider (Adopted from Musicfy).
 * Delivers pure 16-bit and 24-bit Hi-Res FLAC audio without requiring an account or token.
 */
class MonochromeLosslessPlugin(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : MusicStreamPlugin {

    override val id: String = "monochrome_lossless"
    override val name: String = "Monochrome Hi-Res FLAC"
    override val description: String = "Stream Lossless FLAC murni 16-bit / 24-bit tanpa akun langsung dari Monochrome Engine."
    override val version: String = "v2.0"
    override val author: String = "Monochrome & Musicfy"
    override val category: String = "Audio Stream"
    override val githubUrl: String = "https://github.com/monochrome-music/monochrome"
    override val isBuiltIn: Boolean = true
    override var isEnabled: Boolean = true

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    private val baseUrl = "https://tracks.monochrome.st"

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (!isEnabled || query.isBlank()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/search/tracks?q=$encodedQuery&limit=10")
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val tracksArray = json.optJSONArray("tracks") ?: return@withContext emptyList()

            val result = mutableListOf<Track>()
            for (i in 0 until tracksArray.length()) {
                val item = tracksArray.getJSONObject(i)
                val id = item.optString("id")
                val title = item.optString("title")
                val artist = item.optString("artist")
                val album = item.optString("album")
                val durationSec = item.optLong("durationMs", 0L) / 1000L
                val coverUrl = item.optString("coverUrl")

                if (id.isNotBlank() && title.isNotBlank()) {
                    result.add(
                        Track(
                            id = "mono_$id",
                            title = title,
                            artist = Artist(id = "mono_art_${artist.hashCode()}", name = artist),
                            albumTitle = album,
                            durationSec = durationSec.toInt(),
                            audioUrl = "https://tracks.monochrome.st/stream/$id.flac",
                            coverUrl = coverUrl,
                            sourceLabel = "Monochrome Hi-Res FLAC"
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun resolveStream(track: Track): String? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null

        try {
            val trackId = if (track.id.startsWith("mono_")) {
                track.id.removePrefix("mono_")
            } else {
                val query = "${track.artist} ${track.title}".trim()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val searchReq = Request.Builder()
                    .url("$baseUrl/search/tracks?q=$encodedQuery&limit=3")
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .build()

                val searchResp = client.newCall(searchReq).execute()
                if (!searchResp.isSuccessful) return@withContext null
                val searchBody = searchResp.body?.string() ?: return@withContext null
                val searchJson = JSONObject(searchBody)
                val items = searchJson.optJSONArray("tracks") ?: return@withContext null
                if (items.length() == 0) return@withContext null
                items.getJSONObject(0).optString("id")
            }

            if (trackId.isBlank()) return@withContext null

            // Monochrome direct FLAC stream endpoint
            "$baseUrl/track/$trackId"
        } catch (_: Exception) {
            null
        }
    }
}
