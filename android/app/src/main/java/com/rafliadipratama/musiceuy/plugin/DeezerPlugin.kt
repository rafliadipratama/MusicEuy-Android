package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Deezer Hi-Fi & Metadata Plugin.
 * Searches the official Deezer global library and extracts previews and stream URLs.
 * GitHub Repo: https://github.com/RemixDev/deezload
 */
class DeezerPlugin(
    private val client: OkHttpClient = OkHttpClient(),
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "deezer"
    override val name: String = "Deezer Hi-Fi Resolver"
    override val description: String = "Katalog musik global Deezer resmi dengan metadata jernih dan cover beresolusi tinggi 500x500"
    override val version: String = "1.0.0"
    override val author: String = "Deezer Open Community"
    override val category: String = "Audio Stream"
    override val githubUrl: String = "https://github.com/RemixDev/deezload"
    override val isBuiltIn: Boolean = true

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (!isEnabled || query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<Track>()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val req = Request.Builder()
                .url("https://api.deezer.com/search?q=$encoded&limit=25")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val data = json.optJSONArray("data") ?: return@withContext emptyList()
                        for (i in 0 until data.length()) {
                            val item = data.optJSONObject(i) ?: continue
                            val id = item.optLong("id", 0L)
                            val title = item.optString("title", "Track")
                            val duration = item.optInt("duration", 0)
                            val preview = item.optString("preview", "")
                            val artistObj = item.optJSONObject("artist")
                            val artistName = artistObj?.optString("name", "Unknown") ?: "Unknown"
                            val artistAvatar = artistObj?.optString("picture_big", "")
                                ?: artistObj?.optString("picture_medium", "")
                            val albumObj = item.optJSONObject("album")
                            val cover = albumObj?.optString("cover_big", "")
                                ?: albumObj?.optString("cover_medium", "")

                            results.add(
                                Track(
                                    id = "dz:$id",
                                    title = title,
                                    artist = Artist(
                                        id = "dz_art_${artistObj?.optLong("id", 0L)}",
                                        name = artistName,
                                        avatarUrl = artistAvatar,
                                        isVerified = true
                                    ),
                                    albumTitle = albumObj?.optString("title", ""),
                                    durationSec = duration,
                                    audioUrl = if (preview.isNotBlank()) preview else "dz:$id",
                                    coverUrl = cover ?: "",
                                    sourceLabel = "Deezer"
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        results
    }

    override suspend fun resolveStream(track: Track): String? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null
        if (track.audioUrl.startsWith("http") && track.audioUrl.contains("dzcdn.net")) {
            return@withContext track.audioUrl
        }
        val dzId = when {
            track.id.startsWith("dz:") -> track.id.removePrefix("dz:")
            track.audioUrl.startsWith("dz:") -> track.audioUrl.removePrefix("dz:")
            else -> null
        } ?: return@withContext null

        try {
            val req = Request.Builder()
                .url("https://api.deezer.com/track/$dzId")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val obj = JSONObject(body)
                        val preview = obj.optString("preview", "").takeIf { it.isNotBlank() }
                        if (preview != null) return@withContext preview
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }
}
