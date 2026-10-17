package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Custom External Plugin Loader inspired by Audion and Spotube.
 * Allows users to register dynamic external music resolvers via custom API or webhook endpoints.
 */
class CustomScriptPlugin(
    override val id: String,
    override val name: String,
    override val description: String,
    override val version: String = "1.0.0",
    override val author: String = "Komunitas",
    val endpointUrl: String = "",
    override var isEnabled: Boolean = true,
    override val isBuiltIn: Boolean = false
) : MusicStreamPlugin {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (!isEnabled || endpointUrl.isBlank()) return@withContext emptyList()
        // If an external endpoint is configured, query it
        val list = mutableListOf<Track>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$endpointUrl/search?q=$encoded")
                .header("User-Agent", "MusicEuy-Plugin/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    // Parse custom response if supported
                }
            }
        } catch (_: Exception) {
            // Ignored for custom plugin
        }
        list
    }

    override suspend fun resolveStream(track: Track): String? = withContext(Dispatchers.IO) {
        if (!isEnabled || endpointUrl.isBlank()) return@withContext null
        try {
            val request = Request.Builder()
                .url("$endpointUrl/resolve?id=${track.id}&title=${URLEncoder.encode(track.title, "UTF-8")}&artist=${URLEncoder.encode(track.artist.name, "UTF-8")}")
                .header("User-Agent", "MusicEuy-Plugin/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    return@use if (json.has("streamUrl") && !json.isNull("streamUrl")) json.getString("streamUrl") else null
                }
            }
        } catch (_: Exception) {
            null
        }
        null
    }
}
