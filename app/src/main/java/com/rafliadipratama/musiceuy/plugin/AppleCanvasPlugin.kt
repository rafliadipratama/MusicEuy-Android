package com.rafliadipratama.musiceuy.plugin

import android.content.Context
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Apple Music & Spotify Canvas Artwork Plugin.
 * Fetches dynamic vertical looping canvas video backgrounds inspired by Apple Music & Spotify Canvas.
 * Lineage & Codebase Reference: https://github.com/realidkroo/musicfy (providers/applecanvas & providers/canvas)
 */
class AppleCanvasPlugin(
    private val context: Context,
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "apple_canvas"
    override val name: String = "Apple & Spotify Canvas"
    override val description: String = "Video latar belakang looping vertikal (Motion Art) khas Apple Music & Spotify Canvas"
    override val version: String = "1.0.0"
    override val author: String = "Musicfy & Apple Web Play"
    override val category: String = "Visual & Tampilan"
    override val githubUrl: String = "https://github.com/realidkroo/musicfy"
    override val isBuiltIn: Boolean = true

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val canvasCache = ConcurrentHashMap<String, String?>()

    /**
     * Resolves animated canvas video URL (MP4 / HLS) for a given track.
     */
    suspend fun getCanvasVideoUrl(track: Track): String? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null

        val cacheKey = "${track.artist.name} - ${track.title}".lowercase()
        if (canvasCache.containsKey(cacheKey)) {
            return@withContext canvasCache[cacheKey]
        }

        try {
            // Search iTunes / Apple Music Storefront API for Motion Artwork / Video Preview
            val query = URLEncoder.encode("${track.artist.name} ${track.title}", "UTF-8")
            val url = "https://itunes.apple.com/search?term=$query&entity=song&limit=3"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext null

            var resolvedUrl: String? = null
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val previewUrl = item.optString("previewUrl", "").takeIf { it.isNotBlank() }
                if (previewUrl != null && (previewUrl.endsWith(".m4v") || previewUrl.endsWith(".mp4") || previewUrl.contains(".mp4?"))) {
                    resolvedUrl = previewUrl
                    break
                }
            }

            canvasCache[cacheKey] = resolvedUrl
            resolvedUrl
        } catch (_: Exception) {
            canvasCache[cacheKey] = null
            null
        }
    }
}
