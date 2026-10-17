package com.rafliadipratama.musiceuy.data.network

import android.util.Base64
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MusicStreamSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    // JioSaavn DES 320kbps media decryption key
    private val desKey = "38346591".toByteArray(StandardCharsets.US_ASCII)

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<Track>()

        // 1. Primary: JioSaavn (Full length 320kbps / 160kbps songs, no 30s preview limit)
        val directSaavn = fetchSaavn(query.trim())
        if (directSaavn.isNotEmpty()) {
            return@withContext directSaavn
        }

        // 2. Secondary: Deezer search with automatic JioSaavn full-stream resolution
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.deezer.com/search?q=$encoded&limit=15"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val data = json.optJSONArray("data")
                        if (data != null) {
                            for (i in 0 until minOf(data.length(), 10)) {
                                val item = data.optJSONObject(i) ?: continue
                                val id = item.optLong("id", 0L).toString()
                                val title = item.optString("title", "Unknown Track")
                                val artistObj = item.optJSONObject("artist")
                                val artistName = artistObj?.optString("name", "Unknown Artist") ?: "Unknown Artist"
                                val artistAvatar = artistObj?.optString("picture_medium", "")

                                val albumObj = item.optJSONObject("album")
                                val albumTitle = albumObj?.optString("title", "")
                                val coverUrl = albumObj?.optString("cover_big", "")
                                    ?: albumObj?.optString("cover_medium", "")
                                    ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"

                                // Attempt to resolve full stream for this corrected title from JioSaavn
                                val saavnMatches = fetchSaavn("$artistName $title", limit = 3).ifEmpty {
                                    fetchSaavn(title, limit = 3)
                                }

                                if (saavnMatches.isNotEmpty()) {
                                    val matched = saavnMatches.first()
                                    results.add(
                                        matched.copy(
                                            coverUrl = if (matched.coverUrl.isNotBlank()) matched.coverUrl else coverUrl,
                                            albumTitle = if (matched.albumTitle.isNullOrBlank()) albumTitle else matched.albumTitle
                                        )
                                    )
                                } else {
                                    val streamUrl = item.optString("preview", "")
                                    val duration = item.optInt("duration", 0)
                                    if (streamUrl.isNotBlank()) {
                                        results.add(
                                            Track(
                                                id = "dz:$id",
                                                title = title,
                                                artist = Artist(id = id, name = artistName, avatarUrl = artistAvatar),
                                                albumTitle = albumTitle,
                                                durationSec = duration,
                                                audioUrl = streamUrl,
                                                coverUrl = coverUrl
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore fallback error
        }

        results
    }

    fun fetchSaavn(query: String, limit: Int = 25): List<Track> {
        if (query.isBlank()) return emptyList()
        val list = mutableListOf<Track>()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val saavnUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&n=$limit&p=1&q=$encoded&_marker=0&ctx=android"
            val request = Request.Builder()
                .url(saavnUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
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

                                val title = unescapeHtml(item.optString("song", item.optString("title", "Track")))
                                val artistName = unescapeHtml(item.optString("singers", item.optString("primary_artists", "Artist")))
                                val album = unescapeHtml(item.optString("album", ""))
                                val duration = item.optString("duration", "0").toIntOrNull() ?: 0

                                val rawImage = item.optString("image", "")
                                val highResImage = rawImage
                                    .replace("150x150", "500x500")
                                    .replace("50x50", "500x500")
                                    .replace("http://", "https://")

                                val encryptedUrl = item.optString("encrypted_media_url", "")
                                val fullStreamUrl = decryptFullStreamUrl(encryptedUrl)

                                if (!fullStreamUrl.isNullOrBlank()) {
                                    list.add(
                                        Track(
                                            id = "js:$id",
                                            title = title,
                                            artist = Artist(id = id, name = artistName),
                                            albumTitle = album,
                                            durationSec = duration,
                                            audioUrl = fullStreamUrl,
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
            // Error fetching from JioSaavn
        }
        return list
    }

    suspend fun fetchTrending(): List<Track> = withContext(Dispatchers.IO) {
        val trendingQueries = listOf("Top Hits Indonesia", "Hindia", "Coldplay", "Tulus", "Sheila on 7", "Bruno Mars")
        searchTracks(trendingQueries.random())
    }

    fun decryptFullStreamUrl(encryptedMediaUrl: String?): String? {
        if (encryptedMediaUrl.isNullOrBlank()) return null
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            val keySpec = SecretKeySpec(desKey, "DES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val decodedBytes = Base64.decode(encryptedMediaUrl.trim(), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val decrypted = String(decryptedBytes, StandardCharsets.UTF_8).trim()

            // Remove trailing media data
            val cleanUrl = decrypted.replace(Regex("(\\.(?:mp4|m4a)).*$", RegexOption.IGNORE_CASE), "$1")

            // Upgrade to 320kbps / 160kbps high quality
            val hqUrl = cleanUrl
                .replace(Regex("_(?:96|160|320)(?=\\.[^/?#]+(?:[?#]|$))"), "_320")
                .replace("http://", "https://")

            if (hqUrl.startsWith("https://")) {
                hqUrl
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
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
