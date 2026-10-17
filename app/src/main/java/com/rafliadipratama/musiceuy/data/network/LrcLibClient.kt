package com.rafliadipratama.musiceuy.data.network

import com.rafliadipratama.musiceuy.data.model.LyricLine
import com.rafliadipratama.musiceuy.data.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class LrcLibClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val timestampPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    suspend fun fetchLyrics(artistName: String, trackName: String): Lyrics = withContext(Dispatchers.IO) {
        val cleanArtist = sanitize(artistName)
        val cleanTrack = cleanTrackTitle(trackName)

        // 1. Try exact get with clean title
        var result = tryGet(cleanArtist, cleanTrack)
        if (result.hasSyncedLyrics || !result.plainLyrics.isNullOrBlank()) {
            return@withContext result
        }

        // 2. Try exact get with original trackName if different
        if (cleanTrack != trackName.trim()) {
            result = tryGet(artistName.trim(), trackName.trim())
            if (result.hasSyncedLyrics || !result.plainLyrics.isNullOrBlank()) {
                return@withContext result
            }
        }

        // 3. Fallback: Search API
        val searchResult = trySearch("$cleanArtist $cleanTrack")
        if (searchResult.hasSyncedLyrics || !searchResult.plainLyrics.isNullOrBlank()) {
            return@withContext searchResult
        }

        // 4. Fallback: Search with track title alone
        if (cleanTrack.length >= 3) {
            val trackOnlyResult = trySearch(cleanTrack)
            if (trackOnlyResult.hasSyncedLyrics || !trackOnlyResult.plainLyrics.isNullOrBlank()) {
                return@withContext trackOnlyResult
            }
        }

        Lyrics()
    }

    private fun tryGet(artist: String, track: String): Lyrics {
        try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTrack = URLEncoder.encode(track, "UTF-8")
            val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTrack"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MusicEuyAndroid/1.1.0 (https://github.com/rafliadipratama)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Lyrics()
                val body = response.body?.string() ?: return Lyrics()
                val json = JSONObject(body)

                val plain = json.optString("plainLyrics", "").ifBlank { null }
                val rawSynced = json.optString("syncedLyrics", "")
                val syncedLines = parseLrc(rawSynced)

                return Lyrics(plainLyrics = plain, syncedLyrics = syncedLines)
            }
        } catch (_: Exception) {
            return Lyrics()
        }
    }

    private fun trySearch(query: String): Lyrics {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MusicEuyAndroid/1.1.0 (https://github.com/rafliadipratama)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Lyrics()
                val body = response.body?.string() ?: return Lyrics()
                val array = JSONArray(body)

                // First pass: look for entry with synced lyrics
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val rawSynced = item.optString("syncedLyrics", "")
                    if (rawSynced.isNotBlank()) {
                        val lines = parseLrc(rawSynced)
                        if (lines.isNotEmpty()) {
                            return Lyrics(
                                plainLyrics = item.optString("plainLyrics", "").ifBlank { null },
                                syncedLyrics = lines
                            )
                        }
                    }
                }

                // Second pass: fallback to plain lyrics
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val plain = item.optString("plainLyrics", "")
                    if (plain.isNotBlank()) {
                        return Lyrics(plainLyrics = plain)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return Lyrics()
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\[.*?\\]"), "")
            .replace(Regex("(?i)\\((?:official|lyric|video|audio|mv|remastered|live|feat|ft\\.).*?\\)"), "")
            .replace(Regex("(?i)\\b(?:official|video|music video|lyric video|audio|remastered)\\b"), "")
            .replace(Regex("[-–—|/].*$"), "")
            .trim()
            .ifBlank { title.trim() }
    }

    private fun sanitize(input: String): String {
        return input.replace(Regex("(?i)[,-].*$"), "").trim().ifBlank { input.trim() }
    }

    private fun parseLrc(rawLrc: String): List<LyricLine> {
        if (rawLrc.isBlank()) return emptyList()

        val lines = mutableListOf<LyricLine>()
        for (line in rawLrc.lines()) {
            val trimmed = line.trim()
            val matcher = timestampPattern.matcher(trimmed)
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                val text = matcher.group(4)?.trim() ?: ""

                if (text.isNotBlank()) {
                    lines.add(LyricLine(timestampMs = totalMs, text = text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}
