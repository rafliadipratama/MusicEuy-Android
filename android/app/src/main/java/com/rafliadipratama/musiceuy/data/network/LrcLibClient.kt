package com.rafliadipratama.musiceuy.data.network

import com.rafliadipratama.musiceuy.data.model.LyricLine
import com.rafliadipratama.musiceuy.data.model.Lyrics
import com.rafliadipratama.musiceuy.data.model.SyllableWord
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
    private val cacheDir: java.io.File? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val timestampPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
    private val enhancedWordPattern = Pattern.compile("<(\\d{2}):(\\d{2})\\.(\\d{2,3})>([^<]*)")

    private fun getCacheFile(artist: String, track: String): java.io.File? {
        val dir = cacheDir ?: return null
        val lyricsFolder = java.io.File(dir, "lyrics_cache").apply { if (!exists()) mkdirs() }
        val safeName = "${sanitize(artist)}_${cleanTrackTitle(track)}"
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .lowercase()
            .take(80)
        return java.io.File(lyricsFolder, "$safeName.json")
    }

    fun loadFromCache(artist: String, track: String): Lyrics? {
        val file = getCacheFile(artist, track) ?: return null
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val jsonStr = file.readText()
            val json = JSONObject(jsonStr)
            val plain = json.optString("plainLyrics", "").ifBlank { null }
            val syncedArr = json.optJSONArray("syncedLyrics")
            val syncedList = mutableListOf<LyricLine>()
            if (syncedArr != null) {
                for (i in 0 until syncedArr.length()) {
                    val item = syncedArr.getJSONObject(i)
                    val ts = item.getLong("timestampMs")
                    val text = item.getString("text")
                    val wordsArr = item.optJSONArray("words")
                    val wordsList = mutableListOf<SyllableWord>()
                    if (wordsArr != null && wordsArr.length() > 0) {
                        for (w in 0 until wordsArr.length()) {
                            val wObj = wordsArr.getJSONObject(w)
                            wordsList.add(
                                SyllableWord(
                                    word = wObj.getString("word"),
                                    startMs = wObj.getLong("startMs"),
                                    endMs = wObj.getLong("endMs")
                                )
                            )
                        }
                    }
                    syncedList.add(LyricLine(timestampMs = ts, text = text, words = wordsList))
                }
            }
            // Ensure every line has synthesized words if words were empty
            val finalSynced = enrichLinesWithSyllables(syncedList)
            Lyrics(plainLyrics = plain, syncedLyrics = finalSynced)
        } catch (_: Exception) {
            null
        }
    }

    fun saveToCache(artist: String, track: String, lyrics: Lyrics) {
        if (!lyrics.hasSyncedLyrics && lyrics.plainLyrics.isNullOrBlank()) return
        val file = getCacheFile(artist, track) ?: return
        try {
            val json = JSONObject().apply {
                put("plainLyrics", lyrics.plainLyrics ?: "")
                val arr = JSONArray()
                lyrics.syncedLyrics.forEach { line ->
                    arr.put(JSONObject().apply {
                        put("timestampMs", line.timestampMs)
                        put("text", line.text)
                        val wordsArr = JSONArray()
                        line.words.forEach { w ->
                            wordsArr.put(JSONObject().apply {
                                put("word", w.word)
                                put("startMs", w.startMs)
                                put("endMs", w.endMs)
                            })
                        }
                        put("words", wordsArr)
                    })
                }
                put("syncedLyrics", arr)
            }
            file.writeText(json.toString())
        } catch (_: Exception) {}
    }

    suspend fun fetchLyrics(artistName: String, trackName: String): Lyrics = withContext(Dispatchers.IO) {
        // 0. Check offline disk cache first for instant load
        val cached = loadFromCache(artistName, trackName)
        if (cached != null && (cached.hasSyncedLyrics || !cached.plainLyrics.isNullOrBlank())) {
            android.util.Log.d("MusicEuy", "Lyrics loaded from offline cache for: $artistName - $trackName")
            return@withContext cached
        }

        val cleanArtist = sanitize(artistName)
        val cleanTrack = cleanTrackTitle(trackName)

        // 1. Try exact get with clean title
        var result = tryGet(cleanArtist, cleanTrack)
        if (result.hasSyncedLyrics || !result.plainLyrics.isNullOrBlank()) {
            saveToCache(artistName, trackName, result)
            return@withContext result
        }

        // 2. Try exact get with original trackName if different
        if (cleanTrack != trackName.trim()) {
            result = tryGet(artistName.trim(), trackName.trim())
            if (result.hasSyncedLyrics || !result.plainLyrics.isNullOrBlank()) {
                saveToCache(artistName, trackName, result)
                return@withContext result
            }
        }

        // 3. Fallback: Search API
        val searchResult = trySearch("$cleanArtist $cleanTrack")
        if (searchResult.hasSyncedLyrics || !searchResult.plainLyrics.isNullOrBlank()) {
            saveToCache(artistName, trackName, searchResult)
            return@withContext searchResult
        }

        // 4. Fallback: Search with track title alone
        if (cleanTrack.length >= 3) {
            val trackOnlyResult = trySearch(cleanTrack)
            if (trackOnlyResult.hasSyncedLyrics || !trackOnlyResult.plainLyrics.isNullOrBlank()) {
                saveToCache(artistName, trackName, trackOnlyResult)
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

        val parsedRawLines = mutableListOf<LyricLine>()
        for (line in rawLrc.lines()) {
            val trimmed = line.trim()
            val matcher = timestampPattern.matcher(trimmed)
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                val rawText = matcher.group(4)?.trim() ?: ""

                if (rawText.isNotBlank()) {
                    val (cleanText, extractedWords) = parseEnhancedWords(rawText, totalMs)
                    parsedRawLines.add(
                        LyricLine(
                            timestampMs = totalMs,
                            text = cleanText,
                            words = extractedWords
                        )
                    )
                }
            }
        }
        val sorted = parsedRawLines.sortedBy { it.timestampMs }
        return enrichLinesWithSyllables(sorted)
    }

    private fun parseEnhancedWords(rawText: String, lineStartMs: Long): Pair<String, List<SyllableWord>> {
        val wordMatcher = enhancedWordPattern.matcher(rawText)
        val words = mutableListOf<SyllableWord>()
        var foundAny = false

        while (wordMatcher.find()) {
            foundAny = true
            val min = wordMatcher.group(1)?.toLongOrNull() ?: 0L
            val sec = wordMatcher.group(2)?.toLongOrNull() ?: 0L
            val msStr = wordMatcher.group(3) ?: "0"
            val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
            val wordStartMs = (min * 60 * 1000) + (sec * 1000) + ms
            val wordText = wordMatcher.group(4)?.trim() ?: ""

            if (wordText.isNotBlank()) {
                words.add(SyllableWord(word = wordText, startMs = wordStartMs, endMs = wordStartMs + 400L))
            }
        }

        if (foundAny && words.isNotEmpty()) {
            for (i in 0 until words.size - 1) {
                val nextStart = words[i + 1].startMs
                if (nextStart > words[i].startMs) {
                    words[i] = words[i].copy(endMs = nextStart)
                }
            }
            val cleanText = rawText.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
            return Pair(cleanText, words)
        }

        val cleanText = rawText.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
        return Pair(cleanText, emptyList())
    }

    private fun enrichLinesWithSyllables(lines: List<LyricLine>): List<LyricLine> {
        if (lines.isEmpty()) return emptyList()

        return lines.mapIndexed { i, line ->
            if (line.words.isNotEmpty()) {
                line
            } else {
                val nextTimestamp = if (i < lines.size - 1) lines[i + 1].timestampMs else (line.timestampMs + 3800L)
                val lineDuration = (nextTimestamp - line.timestampMs).coerceIn(1200L, 6000L)
                val words = synthesizeWords(line.text, line.timestampMs, lineDuration)
                line.copy(words = words)
            }
        }
    }

    private fun synthesizeWords(text: String, lineStartMs: Long, lineDurationMs: Long): List<SyllableWord> {
        val wordTokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (wordTokens.isEmpty()) return emptyList()

        val totalWeight = wordTokens.sumOf { (it.length.coerceAtLeast(1) + 1) }
        var currentOffset = lineStartMs
        val result = mutableListOf<SyllableWord>()

        for (token in wordTokens) {
            val weight = token.length.coerceAtLeast(1) + 1
            val wordDur = ((lineDurationMs * weight) / totalWeight).coerceAtLeast(120L)
            val end = currentOffset + wordDur
            result.add(SyllableWord(word = token, startMs = currentOffset, endMs = end))
            currentOffset = end
        }
        return result
    }
}
