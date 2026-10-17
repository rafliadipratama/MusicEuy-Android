package com.rafliadipratama.musiceuy.data.network

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MusicStreamSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<Track>()

        // 1. Search via Deezer API (Fast, high-quality audio & covers worldwide)
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.deezer.com/search?q=$encoded&limit=25"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val data = json.optJSONArray("data")
                        if (data != null) {
                            for (i in 0 until data.length()) {
                                val item = data.optJSONObject(i) ?: continue
                                val id = item.optLong("id", 0L).toString()
                                val title = item.optString("title", "Unknown Track")
                                val duration = item.optInt("duration", 0)
                                val previewUrl = item.optString("preview", "")

                                val artistObj = item.optJSONObject("artist")
                                val artistId = artistObj?.optLong("id", 0L)?.toString() ?: "0"
                                val artistName = artistObj?.optString("name", "Unknown Artist") ?: "Unknown Artist"
                                val artistAvatar = artistObj?.optString("picture_medium", "")

                                val albumObj = item.optJSONObject("album")
                                val albumTitle = albumObj?.optString("title", "")
                                val coverUrl = albumObj?.optString("cover_big", "")
                                    ?: albumObj?.optString("cover_medium", "")
                                    ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"

                                if (previewUrl.isNotBlank()) {
                                    results.add(
                                        Track(
                                            id = "dz:$id",
                                            title = title,
                                            artist = Artist(
                                                id = artistId,
                                                name = artistName,
                                                avatarUrl = artistAvatar
                                            ),
                                            albumTitle = albumTitle,
                                            durationSec = duration,
                                            audioUrl = previewUrl,
                                            coverUrl = coverUrl
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Deezer error, try fallback
        }

        // 2. JioSaavn fallback / supplemental search if Deezer returned few results
        if (results.size < 5) {
            try {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                val saavnUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&n=15&p=1&q=$encoded&_marker=0&ctx=android"
                val request = Request.Builder()
                    .url(saavnUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val saavnResults = json.optJSONArray("results")
                            if (saavnResults != null) {
                                for (i in 0 until saavnResults.length()) {
                                    val item = saavnResults.optJSONObject(i) ?: continue
                                    val id = item.optString("id", "")
                                    if (id.isBlank()) continue
                                    val title = unescapeHtml(item.optString("song", "Track"))
                                    val artistName = unescapeHtml(item.optString("singers", item.optString("primary_artists", "Artist")))
                                    val album = unescapeHtml(item.optString("album", ""))
                                    val duration = item.optString("duration", "0").toIntOrNull() ?: 0
                                    val rawImage = item.optString("image", "")
                                    val highResImage = rawImage.replace("150x150", "500x500").replace("http://", "https://")
                                    val preview = item.optString("media_preview_url", "")
                                        .replace("http://", "https://")

                                    if (preview.isNotBlank()) {
                                        results.add(
                                            Track(
                                                id = "js:$id",
                                                title = title,
                                                artist = Artist(id = id, name = artistName),
                                                albumTitle = album,
                                                durationSec = duration,
                                                audioUrl = preview,
                                                coverUrl = highResImage
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore fallback error
            }
        }

        results
    }

    suspend fun fetchTrending(): List<Track> = withContext(Dispatchers.IO) {
        val trendingQueries = listOf("Top Hits", "Trending Indonesia", "Coldplay", "Taylor Swift", "Hindia")
        searchTracks(trendingQueries.random())
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
