package com.rafliadipratama.musiceuy.data.network

import com.rafliadipratama.musiceuy.data.model.LyricLine
import com.rafliadipratama.musiceuy.data.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern

class LrcLibClient(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val timestampPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    suspend fun fetchLyrics(artistName: String, trackName: String): Lyrics = withContext(Dispatchers.IO) {
        try {
            val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
            val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
            val url = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTrack"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MusicEuyAndroid/1.0.0 (https://github.com/rafliadipratama)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Lyrics()
                val body = response.body?.string() ?: return@withContext Lyrics()
                val json = JSONObject(body)

                val plainLyrics = json.optString("plainLyrics", "").ifBlank { null }
                val rawSynced = json.optString("syncedLyrics", "")

                val syncedLines = parseLrc(rawSynced)

                Lyrics(
                    plainLyrics = plainLyrics,
                    syncedLyrics = syncedLines
                )
            }
        } catch (_: Exception) {
            Lyrics()
        }
    }

    private fun parseLrc(rawLrc: String): List<LyricLine> {
        if (rawLrc.isBlank()) return emptyList()

        val lines = mutableListOf<LyricLine>()
        for (line in rawLrc.lines()) {
            val matcher = timestampPattern.matcher(line.trim())
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
