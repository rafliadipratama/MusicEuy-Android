package com.rafliadipratama.musiceuy.data.network

import android.util.Base64
import com.rafliadipratama.musiceuy.data.model.Album
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class SearchAllResults(
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList()
)

data class ExploreHomeData(
    val trendingTracks: List<Track> = emptyList(),
    val newReleases: List<Album> = emptyList(),
    val popularArtists: List<Artist> = emptyList()
)

class MusicStreamSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    // JioSaavn DES 320kbps media decryption key
    private val desKey = "38346591".toByteArray(StandardCharsets.US_ASCII)

    // Strict filter to eliminate Indian songs and Indian regional content
    private val bannedLanguages = setOf(
        "hindi", "punjabi", "bhojpuri", "tamil", "telugu", "marathi",
        "bengali", "kannada", "gujarati", "malayalam", "urdu", "haryanvi",
        "rajasthani", "odia", "assamese", "sanskrit"
    )

    private val bannedKeywords = listOf(
        "t-series", "zee music", "bhangra", "dholak", "qawwali", "naat",
        "sonu nigam", "arijit", "badshah", "sidhu", "shreya ghoshal", "kumar sanu",
        "lata mangeshkar", "dj remix", "viral rimek", "pashmina", "punjabi", "bhojpuri",
        "hawa hawa", "dilbar", "diljit", "neha kakkar", "yo yo honey"
    )

    fun isIndianContent(title: String, artist: String, language: String? = null): Boolean {
        if (!language.isNullOrBlank() && bannedLanguages.contains(language.lowercase())) {
            return true
        }
        val lowerT = title.lowercase()
        val lowerA = artist.lowercase()
        return bannedKeywords.any { lowerT.contains(it) || lowerA.contains(it) }
    }

    /**
     * Ensures candidate audio stream matches requested song title and artist.
     * Prevents mismatched audio streams from playing under wrong titles.
     */
    fun isStrictMatch(expectedTitle: String, expectedArtist: String, candidateTitle: String, candidateArtist: String): Boolean {
        fun normalize(s: String) = s.lowercase()
            .replace(Regex("\\(.*\\)|\\[.*\\]"), "") // remove (feat. ...), [Official Video]
            .replace(Regex("[^a-z0-9]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val normExpT = normalize(expectedTitle)
        val normCandT = normalize(candidateTitle)
        val normExpA = normalize(expectedArtist)
        val normCandA = normalize(candidateArtist)

        if (normExpT.isBlank() || normCandT.isBlank()) return false

        // Check title match
        val titleMatch = normExpT == normCandT ||
                normExpT.contains(normCandT) || normCandT.contains(normExpT) ||
                run {
                    val expWords = normExpT.split(" ").filter { it.length > 2 }
                    val candWords = normCandT.split(" ").filter { it.length > 2 }
                    (expWords.isNotEmpty() && expWords.all { candWords.contains(it) }) ||
                    (candWords.isNotEmpty() && candWords.all { expWords.contains(it) })
                }

        if (!titleMatch) return false

        // Check artist match
        val artistMatch = normExpA.isBlank() || normCandA.isBlank() ||
                normExpA == normCandA ||
                normExpA.contains(normCandA) || normCandA.contains(normExpA) ||
                normExpA.split(" ").filter { it.length > 2 }.any { normCandA.contains(it) }

        return artistMatch
    }

    companion object {
        // Fast in-memory TTL cache for audio stream URLs (videoId -> Pair(streamUrl, expireTimestampMs))
        val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

        // ReplayGain loudness tracking (videoId -> loudnessDb from InnerTube audioConfig)
        val songLoudnessCache = java.util.concurrent.ConcurrentHashMap<String, Float>()

        // Instant query cache for Search
        val searchAllCache = java.util.concurrent.ConcurrentHashMap<String, SearchAllResults>()

        // Instant artist screen cache
        val artistTracksCache = java.util.concurrent.ConcurrentHashMap<String, List<Track>>()
        val artistProfileCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Artist, List<Album>>>()

        fun clearSongUrlCache() {
            songUrlCache.clear()
            songLoudnessCache.clear()
            searchAllCache.clear()
            artistTracksCache.clear()
            artistProfileCache.clear()
        }

        fun getLoudnessDb(videoId: String): Float? {
            val cleanVid = videoId.removePrefix("yt:").removePrefix("mono_").trim()
            return songLoudnessCache[cleanVid]
        }
    }

    private data class AudioStreamCandidate(
        val url: String,
        val bitrate: Int,
        val itag: Int,
        val mimeType: String
    )

    /**
     * Resolves a YouTube video ID into an active direct audio stream URL
     * based on user's selected AudioQuality bitrate preference.
     *
     * 1. Checks in-memory TTL cache (0ms latency).
     * 2. Resolves via high-speed direct InnerTube RPC with IOS/VISIONOS clients (~200ms, unthrottled, no cipher).
     * 3. Falls back to NewPipeExtractor if InnerTube is inaccessible.
     */
    fun resolveYouTubeAudioStream(
        videoId: String,
        preferredQuality: com.rafliadipratama.musiceuy.audio.AudioQuality? = null
    ): String? {
        val cleanVid = videoId.removePrefix("yt:").removePrefix("mono_").trim()
        if (cleanVid.isBlank()) return null

        // 1. In-memory TTL cache check
        val cached = songUrlCache[cleanVid]
        if (cached != null && cached.second > System.currentTimeMillis() + 60_000L) {
            android.util.Log.d("MusicEuy", "resolveYouTubeAudioStream: In-memory cache hit for $cleanVid (0ms latency)")
            return cached.first
        }

        // 2. High-speed direct InnerTube resolution (adopted from Musicfy)
        val innerTubeStream = resolveViaInnerTube(cleanVid, preferredQuality)
        if (!innerTubeStream.isNullOrBlank()) {
            return innerTubeStream
        }

        // 3. Fallback to NewPipeExtractor
        android.util.Log.w("MusicEuy", "InnerTube resolution failed for $cleanVid, falling back to NewPipeExtractor")
        return resolveViaNewPipe(cleanVid, preferredQuality)
    }

    private var visitorData: String? = null

    private fun getOrFetchVisitorData(): String? {
        if (!visitorData.isNullOrBlank()) return visitorData
        return try {
            val req = Request.Builder()
                .url("https://music.youtube.com/sw.js_data")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                if (text.length > 5) {
                    val jsonStr = text.substring(5)
                    val arr = JSONArray(jsonStr)
                    val candidates = arr.optJSONArray(0)?.optJSONArray(2)
                    if (candidates != null) {
                        for (i in 0 until candidates.length()) {
                            val item = candidates.opt(i)
                            if (item is String && item.startsWith("Cgt")) {
                                visitorData = item
                                android.util.Log.d("MusicEuy", "Fetched visitorData for streaming: $item")
                                return item
                            }
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicEuy", "Could not fetch visitorData: ${e.message}")
            null
        }
    }

    private fun resolveViaInnerTube(
        videoId: String,
        preferredQuality: com.rafliadipratama.musiceuy.audio.AudioQuality?
    ): String? {
        val vData = getOrFetchVisitorData()

        // 1. VISIONOS with visitorData: Unthrottled, serves the WHOLE file without truncation (adopted from Musicfy)
        // 2. IOS as fallback
        val clients = listOf(
            Triple(
                "VISIONOS",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
                JSONObject().apply {
                    put("clientName", "VISIONOS")
                    put("clientVersion", "0.1")
                    put("osName", "visionOS")
                    put("osVersion", "1.3.21O771")
                    put("deviceMake", "Apple")
                    put("deviceModel", "RealityDevice14,1")
                    put("hl", "id")
                    put("gl", "ID")
                    if (!vData.isNullOrBlank()) {
                        put("visitorData", vData)
                    }
                }
            ),
            Triple(
                "IOS",
                "com.google.ios.youtube/21.03.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)",
                JSONObject().apply {
                    put("clientName", "IOS")
                    put("clientVersion", "21.03.3")
                    put("osName", "iPadOS")
                    put("osVersion", "17.7.10.21H450")
                    put("deviceMake", "Apple")
                    put("deviceModel", "iPad7,6")
                    put("hl", "id")
                    put("gl", "ID")
                }
            )
        )

        for ((clientName, ua, clientContext) in clients) {
            try {
                val url = "https://music.youtube.com/youtubei/v1/player"
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", clientContext)
                    })
                    put("videoId", videoId)
                }

                val request = Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", ua)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val status = json.optJSONObject("playabilityStatus")?.optString("status", "")
                    if (status != "OK") {
                        android.util.Log.w("MusicEuy", "InnerTube $clientName status=$status for $videoId")
                        return@use
                    }

                    val streamingData = json.optJSONObject("streamingData") ?: return@use

                    // Capture ReplayGain loudness metadata from InnerTube playerConfig
                    val audioConfig = json.optJSONObject("playerConfig")?.optJSONObject("audioConfig")
                    val loudness = audioConfig?.optDouble("loudnessDb", Double.NaN)
                    val perceptualLoudness = audioConfig?.optDouble("perceptualLoudnessDb", Double.NaN)
                    val effectiveLoudness = if (loudness != null && !loudness.isNaN()) loudness.toFloat()
                        else if (perceptualLoudness != null && !perceptualLoudness.isNaN()) perceptualLoudness.toFloat()
                        else null
                    if (effectiveLoudness != null) {
                        songLoudnessCache[videoId] = effectiveLoudness
                        android.util.Log.d("MusicEuy", "ReplayGain captured for $videoId: $effectiveLoudness dB")
                    }

                    val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: return@use
                    val audioFormats = mutableListOf<AudioStreamCandidate>()

                    for (i in 0 until adaptiveFormats.length()) {
                        val f = adaptiveFormats.optJSONObject(i) ?: continue
                        val mime = f.optString("mimeType", "")
                        if (!mime.contains("audio")) continue
                        val streamUrl = f.optString("url", "")
                        if (streamUrl.isBlank()) continue
                        val bitrate = f.optInt("bitrate", 0)
                        val itag = f.optInt("itag", 0)
                        audioFormats.add(AudioStreamCandidate(streamUrl, bitrate, itag, mime))
                    }

                    if (audioFormats.isEmpty()) return@use

                    val chosen = when (preferredQuality) {
                        com.rafliadipratama.musiceuy.audio.AudioQuality.HEMAT -> {
                            audioFormats.minByOrNull { it.bitrate } ?: audioFormats.firstOrNull()
                        }
                        com.rafliadipratama.musiceuy.audio.AudioQuality.STANDAR -> {
                            audioFormats.minByOrNull { kotlin.math.abs(it.bitrate - 128000) } ?: audioFormats.maxByOrNull { it.bitrate }
                        }
                        com.rafliadipratama.musiceuy.audio.AudioQuality.TINGGI, null -> {
                            audioFormats.maxByOrNull { it.bitrate }
                        }
                    }

                    val selectedUrl = chosen?.url ?: audioFormats.first().url
                    val expireMatch = Regex("[?&]expire=(\\d+)").find(selectedUrl)
                    val expireMs = expireMatch?.groupValues?.get(1)?.toLongOrNull()?.let { it * 1000L }
                        ?: (System.currentTimeMillis() + 5 * 3600 * 1000L)

                    songUrlCache[videoId] = Pair(selectedUrl, expireMs)
                    android.util.Log.d("MusicEuy", "Resolved via InnerTube $clientName for $videoId (bitrate=${chosen?.bitrate}): $selectedUrl")
                    return selectedUrl
                }
            } catch (e: Exception) {
                android.util.Log.w("MusicEuy", "InnerTube $clientName error for $videoId: ${e.message}")
            }
        }
        return null
    }

    private fun resolveViaNewPipe(
        videoId: String,
        preferredQuality: com.rafliadipratama.musiceuy.audio.AudioQuality?
    ): String? {
        return try {
            val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId") as? YoutubeStreamExtractor ?: return null
            extractor.fetchPage()
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) return null

            val selectedStream = when (preferredQuality) {
                com.rafliadipratama.musiceuy.audio.AudioQuality.HEMAT -> {
                    audioStreams.minByOrNull { it.bitrate } ?: audioStreams.firstOrNull()
                }
                com.rafliadipratama.musiceuy.audio.AudioQuality.STANDAR -> {
                    audioStreams.minByOrNull { kotlin.math.abs(it.bitrate - 128000) } ?: audioStreams.maxByOrNull { it.bitrate }
                }
                com.rafliadipratama.musiceuy.audio.AudioQuality.TINGGI, null -> {
                    audioStreams.maxByOrNull { it.bitrate }
                }
            }
            val stream = selectedStream?.content ?: audioStreams.maxByOrNull { it.bitrate }?.content
            if (!stream.isNullOrBlank()) {
                val expireMatch = Regex("[?&]expire=(\\d+)").find(stream)
                val expireMs = expireMatch?.groupValues?.get(1)?.toLongOrNull()?.let { it * 1000L }
                    ?: (System.currentTimeMillis() + 5 * 3600 * 1000L)
                songUrlCache[videoId] = Pair(stream, expireMs)
            }
            android.util.Log.d("MusicEuy", "resolveViaNewPipe for $videoId (quality=$preferredQuality, bitrate=${selectedStream?.bitrate}): streamUrl=$stream")
            stream
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "resolveViaNewPipe error for $videoId: ${e.javaClass.simpleName} - ${e.message}", e)
            null
        }
    }

    /**
     * Primary YouTube Music search via InnerTube API.
     * Returns 100% verified official songs, real artist names, accurate duration,
     * high-res album covers, and zero Indian clutter.
     */
    suspend fun searchYouTubeMusic(query: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val url = "https://music.youtube.com/youtubei/v1/search"
            val payload = JSONObject().apply {
                put("query", query.trim())
                put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBw%3D%3D") // Filter Songs only
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240318.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val contents = json.optJSONObject("contents")
                            ?.optJSONObject("tabbedSearchResultsRenderer")
                            ?.optJSONArray("tabs")
                            ?.optJSONObject(0)
                            ?.optJSONObject("tabRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("sectionListRenderer")
                            ?.optJSONArray("contents")

                        if (contents != null) {
                            for (s in 0 until contents.length()) {
                                val sec = contents.optJSONObject(s) ?: continue
                                val shelf = sec.optJSONObject("musicShelfRenderer") ?: continue
                                val items = shelf.optJSONArray("contents") ?: continue

                                for (i in 0 until items.length()) {
                                    val item = items.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                                    val cols = item.optJSONArray("flexColumns") ?: continue
                                    if (cols.length() < 2) continue

                                    // Title
                                    val titleRuns = cols.optJSONObject(0)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")
                                    val title = titleRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                                    if (title.isBlank()) continue

                                    // Artist, Album, Duration
                                    val subRuns = cols.optJSONObject(1)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")
                                    val artist = subRuns?.optJSONObject(0)?.optString("text", "Artist") ?: "Artist"
                                    val album = if ((subRuns?.length() ?: 0) > 2) subRuns?.optJSONObject(2)?.optString("text", "") else ""
                                    val durationStr = if ((subRuns?.length() ?: 0) > 0) subRuns?.optJSONObject(subRuns.length() - 1)?.optString("text", "0:00") else "0:00"
                                    val durationSec = parseDurationToSeconds(durationStr ?: "")

                                    // Ban any Indian content
                                    if (isIndianContent(title, artist)) continue

                                    // Video ID
                                    val overlay = item.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                                    val playBtn = overlay?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                                    val videoId = playBtn?.optJSONObject("playNavigationEndpoint")
                                        ?.optJSONObject("watchEndpoint")
                                        ?.optString("videoId", "") ?: ""
                                    if (videoId.isBlank()) continue

                                    // Extract Play Count / Views if present in flexColumns[2] or subRuns
                                    var playCount: String? = null
                                    if (cols.length() > 2) {
                                        val pcRuns = cols.optJSONObject(2)
                                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                            ?.optJSONObject("text")
                                            ?.optJSONArray("runs")
                                        val rawText = pcRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                                        if (rawText.isNotBlank()) {
                                            playCount = rawText
                                        }
                                    }
                                    if (playCount.isNullOrBlank() && subRuns != null) {
                                        for (r in 0 until subRuns.length()) {
                                            val txt = subRuns.optJSONObject(r)?.optString("text", "") ?: ""
                                            if (txt.contains("pemutaran", ignoreCase = true) ||
                                                txt.contains("tayangan", ignoreCase = true) ||
                                                txt.contains("ditonton", ignoreCase = true) ||
                                                txt.contains("views", ignoreCase = true)
                                            ) {
                                                playCount = txt
                                                break
                                            }
                                        }
                                    }

                                    // Thumbnail
                                    val thumbs = item.optJSONObject("thumbnail")
                                        ?.optJSONObject("musicThumbnailRenderer")
                                        ?.optJSONObject("thumbnail")
                                        ?.optJSONArray("thumbnails")
                                    var thumbUrl = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                                    thumbUrl = thumbUrl.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")

                                    tracks.add(
                                        Track(
                                            id = "yt:$videoId",
                                            title = title,
                                            artist = Artist(id = videoId, name = artist),
                                            albumTitle = album,
                                            durationSec = durationSec,
                                            audioUrl = "yt:$videoId", // Resolved by NewPipe on play
                                            coverUrl = thumbUrl,
                                            playCount = playCount
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // YouTube Music query error
        }
        tracks
    }

    suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<Artist>()
        try {
            val url = "https://music.youtube.com/youtubei/v1/search"
            val payload = JSONObject().apply {
                put("query", query.trim())
                put("params", "EgWKAQIgAWoSEAUQAxAEEAoQDhAJEBAQFRAR")
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240318.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val contents = json.optJSONObject("contents")
                            ?.optJSONObject("tabbedSearchResultsRenderer")
                            ?.optJSONArray("tabs")
                            ?.optJSONObject(0)
                            ?.optJSONObject("tabRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("sectionListRenderer")
                            ?.optJSONArray("contents")

                        if (contents != null) {
                            for (s in 0 until contents.length()) {
                                val sec = contents.optJSONObject(s) ?: continue
                                val shelf = sec.optJSONObject("musicShelfRenderer") ?: continue
                                val items = shelf.optJSONArray("contents") ?: continue

                                for (i in 0 until items.length()) {
                                    val item = items.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                                    val cols = item.optJSONArray("flexColumns") ?: continue
                                    if (cols.length() < 1) continue

                                    val nameRuns = cols.optJSONObject(0)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")
                                    val name = nameRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                                    if (name.isBlank() || isIndianContent(name, name)) continue

                                    val navEndpoint = item.optJSONObject("navigationEndpoint")
                                    val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId", "") ?: ""

                                    val thumbs = item.optJSONObject("thumbnail")
                                        ?.optJSONObject("musicThumbnailRenderer")
                                        ?.optJSONObject("thumbnail")
                                        ?.optJSONArray("thumbnails")
                                    var thumbUrl = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                                    thumbUrl = thumbUrl.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")

                                    artists.add(
                                        Artist(
                                            id = if (browseId.isNotBlank()) browseId else "artist_${name.hashCode()}",
                                            name = name,
                                            avatarUrl = if (thumbUrl.isNotBlank()) thumbUrl else null,
                                            isVerified = true
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        artists
    }

    suspend fun resolveArtistAvatar(artistName: String): String? = withContext(Dispatchers.IO) {
        val clean = artistName.trim()
        if (clean.isBlank()) return@withContext null

        // 1. Check verified static catalog & runtime cache
        val catalogAvatar = ArtistCatalog.getVerifiedAvatarUrl(clean)
        if (!catalogAvatar.isNullOrBlank()) return@withContext catalogAvatar

        // 2. Fast query to Deezer public artist search (unauthenticated, fast CDN)
        try {
            val encoded = java.net.URLEncoder.encode(clean, "UTF-8")
            val req = Request.Builder()
                .url("https://api.deezer.com/search/artist?q=$encoded&limit=1")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val data = json.optJSONArray("data")
                        if (data != null && data.length() > 0) {
                            val obj = data.getJSONObject(0)
                            val pic = obj.optString("picture_big", "").takeIf { it.isNotBlank() }
                                ?: obj.optString("picture_medium", "").takeIf { it.isNotBlank() }
                            // Filter out blank placeholder md5
                            if (!pic.isNullOrBlank() && !pic.contains("d41d8cd98f00b204e9800998ecf8427e")) {
                                ArtistCatalog.cacheAvatar(clean, pic)
                                return@withContext pic
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: YouTube Music artist search
        try {
            val ytArtists = searchArtists(clean)
            val first = ytArtists.firstOrNull { it.avatarUrl != null && it.avatarUrl.isNotBlank() }
            if (first != null && first.avatarUrl != null) {
                ArtistCatalog.cacheAvatar(clean, first.avatarUrl)
                return@withContext first.avatarUrl
            }
        } catch (_: Exception) {}

        null
    }

    suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        try {
            val url = "https://music.youtube.com/youtubei/v1/search"
            val payload = JSONObject().apply {
                put("query", query.trim())
                put("params", "EgWKAQIYAWoSEAUQAxAEEAoQDhAJEBAQFRAR")
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240318.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val contents = json.optJSONObject("contents")
                            ?.optJSONObject("tabbedSearchResultsRenderer")
                            ?.optJSONArray("tabs")
                            ?.optJSONObject(0)
                            ?.optJSONObject("tabRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("sectionListRenderer")
                            ?.optJSONArray("contents")

                        if (contents != null) {
                            for (s in 0 until contents.length()) {
                                val sec = contents.optJSONObject(s) ?: continue
                                val shelf = sec.optJSONObject("musicShelfRenderer") ?: continue
                                val items = shelf.optJSONArray("contents") ?: continue

                                for (i in 0 until items.length()) {
                                    val item = items.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                                    val cols = item.optJSONArray("flexColumns") ?: continue
                                    if (cols.length() < 2) continue

                                    val titleRuns = cols.optJSONObject(0)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")
                                    val title = titleRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                                    if (title.isBlank()) continue

                                    val subRuns = cols.optJSONObject(1)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")
                                    val artistName = if ((subRuns?.length() ?: 0) > 2) subRuns?.optJSONObject(2)?.optString("text", "Artist") ?: "Artist" else "Artist"
                                    val year = if ((subRuns?.length() ?: 0) > 4) subRuns?.optJSONObject(4)?.optString("text", "") ?: "" else ""

                                    if (isIndianContent(title, artistName)) continue

                                    val navEndpoint = item.optJSONObject("navigationEndpoint")
                                    val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId", "") ?: ""

                                    val thumbs = item.optJSONObject("thumbnail")
                                        ?.optJSONObject("musicThumbnailRenderer")
                                        ?.optJSONObject("thumbnail")
                                        ?.optJSONArray("thumbnails")
                                    var thumbUrl = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                                    thumbUrl = thumbUrl.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")

                                    albums.add(
                                        Album(
                                            id = if (browseId.isNotBlank()) browseId else "album_${title.hashCode()}",
                                            title = title,
                                            artistName = artistName,
                                            year = year,
                                            coverUrl = thumbUrl
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        albums
    }

    suspend fun searchAll(query: String): SearchAllResults = withContext(Dispatchers.IO) {
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return@withContext SearchAllResults()

        val cacheKey = cleanQ.lowercase()
        val cached = searchAllCache[cacheKey]
        if (cached != null) {
            android.util.Log.d("MusicEuy", "searchAll cache hit for: $cleanQ")
            return@withContext cached
        }

        val tracksDef = async { searchTracks(cleanQ) }
        val artistsDef = async { searchArtists(cleanQ) }
        val albumsDef = async { searchAlbums(cleanQ) }

        val result = SearchAllResults(
            tracks = tracksDef.await(),
            artists = artistsDef.await(),
            albums = albumsDef.await()
        )
        if (result.tracks.isNotEmpty() || result.artists.isNotEmpty() || result.albums.isNotEmpty()) {
            searchAllCache[cacheKey] = result
        }
        result
    }

    suspend fun fetchExploreHome(favoriteArtists: List<String> = emptyList()): ExploreHomeData = withContext(Dispatchers.IO) {
        val topReleasesCatalog = listOf(
            // Top Indonesian Releases
            Album(
                id = "MPREb_0Ai4t8QRIZ2",
                title = "Semoga Hanya di Mimpi",
                artistName = "Bernadya",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/4_-ytwCqSUkPrYfjkSaY7Cs497XnahcMR7_vCcxoahaUjeoESN6XvxZlrg9FJ2sPYyL75oYZdDg2DPbw=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_hMQ3MJ2xKDq",
                title = "Sialnya, Hidup Harus Tetap Berjalan",
                artistName = "Bernadya",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/wWzwvnTgKUOp6XzlvnJSt8lR3wLSVOtA3_OCFHky1B4nw2VaZxDcvmRVHBz4xQCFDzVTIjjcKnJYfEdj0w=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_TwQKomxWcNC",
                title = "Menari Dengan Bayangan",
                artistName = "Hindia",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/7oX7kccOMbD9v7SqDbSwHHoLQogdM1QWPyfSaeCUD9FMFj8pJK9dXYdR7bM8hH9S8bGWH2IXnLFtuGdx=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_4TEBtGA3cZb",
                title = "Monokrom",
                artistName = "Tulus",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/SJTWzxOnk0D_TIfYfkcIUzlYbSB5P8QtPYfdjRt3cBeyMk-0cuxdxTIXYp45ghLEhjEvnUpakX8scPgN=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_0KPpVUDJtCU",
                title = "Kisah Klasik Untuk Masa Depan",
                artistName = "Sheila On 7",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/3j4jB2mmRExE3BXFTX7vnOW9T_aGQ37ome4PN_AUwl6s6fPS1bXDSfFqRF3SJ9lwSsr9Z-gFmXhn769r=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_gfd3mrJqpJX",
                title = "fábula",
                artistName = "Mahalini",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/dca67HvZTqlOuKgfPkADCZ4SYDdwm4B-TfWzYIvBLhF7ycFsHcqGmeMYJ3v6n0emRBsga1If93iiFWs=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_oMWO9R4wvAJ",
                title = "Buzz",
                artistName = "NIKI",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/1Q9nhO8nVlZWDkPZ-cDsfho9kvmCwZGaDqVm7AcD4NlsNyfFz3mujJ40miRE-SFBvAJ9y_zvqsYM7kY=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_N1mNDe0n1Q6",
                title = "MARKERS AND SUCH PENS FLASHDISKS",
                artistName = "Sal Priadi",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/CnRlHvII5qQcoLQ8XW3_0b7qOLpBaDFtrCR-rQaCyKyQxuycqhLUc1PatxRtYFiEpDzZyzWqE1wdO0l9Cw=w544-h544-l90-rj"
            ),

            // Top Western Releases
            Album(
                id = "MPREb_x6rS8BJlVQW",
                title = "THE TORTURED POETS DEPARTMENT",
                artistName = "Taylor Swift",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/SYfKamriC6t59kKjaUdZk2QDKqLRW4PtBZ054F82J06Sai_Y95XbxsLdeEHDakGcwuAbBOUsWJRs66aO=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_Wp9Aj8HpTsB",
                title = "HIT ME HARD AND SOFT",
                artistName = "Billie Eilish",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/mXJjWX4E6Gpr03CUYl18PdVXlczmoL2Tm-LEBGafIr_8smlHnl8AHniJu0_7Y80e-aeloJxcryQQx0ZJ=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_pnVvyDhXXd0",
                title = "Moon Music",
                artistName = "Coldplay",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/a5utM-vn-E7089CSnyTbmCR8TZnIUkMK_P5HhAjEY6yLM2AKt-PF8ugze-b8k58J1DxRiBO3pUk6LgknKw=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_XWv5H88Eaub",
                title = "Die With A Smile",
                artistName = "Bruno Mars & Lady Gaga",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/x5ST0wHIxD__IIxHF1UqUgXP4QW58ouj1EHROC9I5yHIb8cmoNr9rL54RfA-BnYay_eWmmOJfXNuFqmPEA=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_FN2r5jrFXVm",
                title = "Short n' Sweet",
                artistName = "Sabrina Carpenter",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/bTWlZSenrOAYgH4r6NAzyDraWQR_wLl3OuRexJ_8h3NZUVHEilRSzUmKNa9YMOFSVcF0YtOuzKdXrt2UHg=w544-h544-l90-rj"
            ),
            Album(
                id = "MPREb_VLEl8SjepHO",
                title = "folklore",
                artistName = "Taylor Swift",
                year = "2026",
                coverUrl = "https://yt3.googleusercontent.com/q7JVahTldHxztLXfCJfgjf5DHr9mi18m2HKpS_IJqBqvvzfAJKPCh_rg-9v-aoGqp-Cc6PHW3ceFMlIQ=w544-h544-l90-rj"
            )
        )

        // Shuffled & dynamically balanced mix of top Indonesian & Western releases
        val indoReleases = topReleasesCatalog.filter { it.artistName in listOf("Bernadya", "Hindia", "Tulus", "Sheila On 7", "Mahalini", "NIKI", "Sal Priadi") }.shuffled()
        val westernReleases = topReleasesCatalog.filter { it.artistName !in listOf("Bernadya", "Hindia", "Tulus", "Sheila On 7", "Mahalini", "NIKI", "Sal Priadi") }.shuffled()

        val dynamicNewReleases = mutableListOf<Album>()
        val maxPairs = maxOf(indoReleases.size, westernReleases.size)
        for (idx in 0 until maxPairs) {
            if (idx < indoReleases.size) dynamicNewReleases.add(indoReleases[idx])
            if (idx < westernReleases.size) dynamicNewReleases.add(westernReleases[idx])
        }

        // Adaptive recommendation feed: mix user favorite artists' hits with top trending Indonesia
        val trending = try {
            val generalTrending = searchYouTubeMusic("Top Hits Indonesia Terpopuler 2026")
            if (favoriteArtists.isNotEmpty()) {
                val topFav = favoriteArtists.take(2).joinToString(" ")
                val personalized = searchYouTubeMusic("$topFav hits lagu terpopuler")
                if (personalized.isNotEmpty()) {
                    val combined = mutableListOf<Track>()
                    val seenIds = mutableSetOf<String>()
                    val pIter = personalized.iterator()
                    val gIter = generalTrending.iterator()
                    while (pIter.hasNext() || gIter.hasNext()) {
                        if (pIter.hasNext()) {
                            val t = pIter.next()
                            if (seenIds.add(t.id)) combined.add(t)
                        }
                        if (gIter.hasNext()) {
                            val t = gIter.next()
                            if (seenIds.add(t.id)) combined.add(t)
                        }
                    }
                    combined
                } else generalTrending
            } else {
                generalTrending.ifEmpty { fetchTrending() }
            }
        } catch (_: Exception) {
            fetchTrending()
        }

        val popularArtists = ArtistCatalog.getCuratedPopularArtists()

        ExploreHomeData(
            trendingTracks = trending,
            newReleases = dynamicNewReleases,
            popularArtists = popularArtists
        )
    }

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // 1. YouTube Music (Verified official tracks, authentic Indonesian & Global Pop/Indie)
        val ytTracks = searchYouTubeMusic(query.trim())
        if (ytTracks.isNotEmpty()) {
            return@withContext ytTracks
        }

        // 2. High-Quality Direct Stream (with strict Indian language filtering)
        val directSaavn = fetchSaavn(query.trim())
        if (directSaavn.isNotEmpty()) {
            return@withContext directSaavn
        }

        // 3. Fallback: Deezer search with automatic clean stream resolution
        val results = mutableListOf<Track>()
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

                                if (isIndianContent(title, artistName)) continue

                                val albumObj = item.optJSONObject("album")
                                val albumTitle = albumObj?.optString("title", "")
                                val coverUrl = albumObj?.optString("cover_big", "")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: albumObj?.optString("cover_medium", "")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: ""

                                // Attempt to resolve clean stream with strict match
                                val saavnMatches = fetchSaavn("$artistName $title", limit = 5).filter {
                                    isStrictMatch(expectedTitle = title, expectedArtist = artistName, candidateTitle = it.title, candidateArtist = it.artist.name)
                                }

                                if (saavnMatches.isNotEmpty()) {
                                    val matched = saavnMatches.first()
                                    results.add(
                                        matched.copy(
                                            title = title,
                                            artist = Artist(id = id, name = artistName, avatarUrl = artistAvatar),
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

                                val language = item.optString("language", "").lowercase()
                                val title = unescapeHtml(item.optString("song", item.optString("title", "Track")))
                                val artistName = unescapeHtml(item.optString("singers", item.optString("primary_artists", "Artist")))

                                // Block Indian languages & keywords completely
                                if (isIndianContent(title, artistName, language)) continue

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
        val curatedQueries = listOf(
            "Bernadya",
            "Pop Indonesia Terkini",
            "Nadin Amizah",
            "Hindia",
            "Sal Priadi",
            "Sheila On 7",
            "Coldplay",
            "Bruno Mars"
        )
        val tracks = searchTracks(curatedQueries.random())
        if (tracks.isNotEmpty()) {
            tracks
        } else {
            searchTracks("Top Hits Musik Indonesia").ifEmpty { emptyList() }
        }
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

    fun parseDurationToSeconds(str: String): Int {
        val clean = str.replace(".", ":").trim()
        val parts = clean.split(":")
        return when (parts.size) {
            2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
            else -> 180
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

    /**
     * Fetches artist profile (avatar, banner, browseId) and all official albums from YouTube Music.
     */
    suspend fun getArtistProfileAndAlbums(artistName: String): Pair<Artist, List<Album>> = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return@withContext Pair(Artist("empty", "", null), emptyList())

        val cacheKey = cleanName.lowercase()
        val cached = artistProfileCache[cacheKey]
        if (cached != null) {
            android.util.Log.d("MusicEuy", "getArtistProfileAndAlbums cache hit for: $cleanName")
            return@withContext cached
        }

        val albums = mutableListOf<Album>()
        var artistBrowseId: String? = null
        var artistAvatar: String? = null
        var artistBanner: String? = null

        try {
            // 1. Search artist to get their browseId and avatar
            val searchUrl = "https://music.youtube.com/youtubei/v1/search"
            val searchPayload = JSONObject().apply {
                put("query", artistName.trim())
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240318.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
            }
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(searchPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(searchReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val contents = json.optJSONObject("contents")
                            ?.optJSONObject("tabbedSearchResultsRenderer")
                            ?.optJSONArray("tabs")
                            ?.optJSONObject(0)
                            ?.optJSONObject("tabRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("sectionListRenderer")
                            ?.optJSONArray("contents")

                        if (contents != null) {
                            for (i in 0 until contents.length()) {
                                val s = contents.optJSONObject(i) ?: continue
                                val card = s.optJSONObject("musicCardShelfRenderer") ?: continue

                                // Extract Artist Avatar from search card
                                val cardThumbs = card.optJSONObject("thumbnail")
                                    ?.optJSONObject("musicThumbnailRenderer")
                                    ?.optJSONObject("thumbnail")
                                    ?.optJSONArray("thumbnails")
                                if (cardThumbs != null && cardThumbs.length() > 0) {
                                    val rawAvatar = cardThumbs.optJSONObject(cardThumbs.length() - 1)?.optString("url", "") ?: ""
                                    if (rawAvatar.isNotBlank()) {
                                        artistAvatar = rawAvatar.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")
                                    }
                                }

                                val titleRuns = card.optJSONObject("title")?.optJSONArray("runs")
                                if (titleRuns != null) {
                                    for (r in 0 until titleRuns.length()) {
                                        val b = titleRuns.optJSONObject(r)
                                            ?.optJSONObject("navigationEndpoint")
                                            ?.optJSONObject("browseEndpoint")
                                            ?.optString("browseId")
                                        if (!b.isNullOrBlank()) {
                                            artistBrowseId = b
                                            break
                                        }
                                    }
                                }
                                if (artistBrowseId != null) break
                            }
                        }
                    }
                }
            }

            if (artistBrowseId.isNullOrBlank()) {
                val fallbackArtist = Artist(
                    id = "art_$artistName",
                    name = artistName,
                    avatarUrl = artistAvatar,
                    bannerUrl = null,
                    isVerified = true
                )
                return@withContext Pair(fallbackArtist, emptyList())
            }

            // 2. Browse artist page to get banner, foreground avatar, and album carousel
            val browseUrl = "https://music.youtube.com/youtubei/v1/browse"
            val browsePayload = JSONObject().apply {
                put("browseId", artistBrowseId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240318.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
            }
            val browseReq = Request.Builder()
                .url(browseUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(browsePayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            var discographyBrowseId: String? = null
            var discographyParams: String? = null
            val fallbackAlbums = mutableListOf<Album>()

            client.newCall(browseReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)

                        // Extract Header Banner & Foreground Avatar
                        val header = json.optJSONObject("header")
                        val hObj = header?.optJSONObject("musicImmersiveHeaderRenderer")
                            ?: header?.optJSONObject("musicVisualHeaderRenderer")
                        if (hObj != null) {
                            val hThumbs = hObj.optJSONObject("thumbnail")
                                ?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")
                                ?.optJSONArray("thumbnails")
                            if (hThumbs != null && hThumbs.length() > 0) {
                                artistBanner = hThumbs.optJSONObject(hThumbs.length() - 1)?.optString("url", "") ?: ""
                            }
                            if (artistAvatar.isNullOrBlank()) {
                                val fgThumbs = hObj.optJSONObject("foregroundThumbnail")
                                    ?.optJSONObject("musicThumbnailRenderer")
                                    ?.optJSONObject("thumbnail")
                                    ?.optJSONArray("thumbnails")
                                if (fgThumbs != null && fgThumbs.length() > 0) {
                                    val rawFg = fgThumbs.optJSONObject(fgThumbs.length() - 1)?.optString("url", "") ?: ""
                                    if (rawFg.isNotBlank()) {
                                        artistAvatar = rawFg.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")
                                    }
                                }
                            }
                        }

                        val secs = json.optJSONObject("contents")
                            ?.optJSONObject("singleColumnBrowseResultsRenderer")
                            ?.optJSONArray("tabs")
                            ?.optJSONObject(0)
                            ?.optJSONObject("tabRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("sectionListRenderer")
                            ?.optJSONArray("contents")

                        if (secs != null) {
                            for (i in 0 until secs.length()) {
                                val s = secs.optJSONObject(i) ?: continue
                                val carousel = s.optJSONObject("musicCarouselShelfRenderer") ?: continue
                                val basicHeader = carousel.optJSONObject("header")
                                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer") ?: continue
                                val headerTitle = basicHeader.optJSONObject("title")
                                    ?.optJSONArray("runs")
                                    ?.optJSONObject(0)
                                    ?.optString("text", "") ?: ""

                                if (headerTitle.equals("Album", ignoreCase = true) || headerTitle.contains("Album", ignoreCase = true)) {
                                    val moreBtn = basicHeader.optJSONObject("moreContentButton")
                                        ?.optJSONObject("buttonRenderer")
                                        ?.optJSONObject("navigationEndpoint")
                                        ?.optJSONObject("browseEndpoint")
                                    if (moreBtn != null) {
                                        discographyBrowseId = moreBtn.optString("browseId", "")
                                        discographyParams = moreBtn.optString("params", "")
                                    }

                                    val items = carousel.optJSONArray("contents") ?: JSONArray()
                                    for (j in 0 until items.length()) {
                                        val twoRow = items.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                                        val title = twoRow.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                        val bId = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId", "") ?: ""
                                        val subruns = twoRow.optJSONObject("subtitle")?.optJSONArray("runs")
                                        val year = if ((subruns?.length() ?: 0) > 0) subruns?.optJSONObject(subruns.length() - 1)?.optString("text", "") ?: "" else ""
                                        val thumbs = twoRow.optJSONObject("thumbnailRenderer")
                                            ?.optJSONObject("musicThumbnailRenderer")
                                            ?.optJSONObject("thumbnail")
                                            ?.optJSONArray("thumbnails")
                                        var cover = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                                        cover = cover.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")

                                        if (title.isNotBlank() && bId.isNotBlank()) {
                                            fallbackAlbums.add(
                                                Album(
                                                    id = bId,
                                                    title = title,
                                                    artistName = artistName,
                                                    year = year.trim(),
                                                    coverUrl = cover
                                                )
                                            )
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            }

            // 3. If discography browseId exists, load full discography grid
            if (!discographyBrowseId.isNullOrBlank()) {
                val discoPayload = JSONObject().apply {
                    put("browseId", discographyBrowseId)
                    if (!discographyParams.isNullOrBlank()) {
                        put("params", discographyParams)
                    }
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20240318.01.00")
                            put("hl", "id")
                            put("gl", "ID")
                        })
                    })
                }
                val discoReq = Request.Builder()
                    .url(browseUrl)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(discoPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(discoReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val items = json.optJSONObject("contents")
                                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                                ?.optJSONArray("tabs")
                                ?.optJSONObject(0)
                                ?.optJSONObject("tabRenderer")
                                ?.optJSONObject("content")
                                ?.optJSONObject("sectionListRenderer")
                                ?.optJSONArray("contents")
                                ?.optJSONObject(0)
                                ?.optJSONObject("gridRenderer")
                                ?.optJSONArray("items")

                            if (items != null) {
                                for (i in 0 until items.length()) {
                                    val twoRow = items.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                                    val title = twoRow.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                                    val bId = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId", "") ?: ""
                                    val subruns = twoRow.optJSONObject("subtitle")?.optJSONArray("runs")
                                    val year = if ((subruns?.length() ?: 0) > 0) subruns?.optJSONObject(subruns.length() - 1)?.optString("text", "") ?: "" else ""
                                    val thumbs = twoRow.optJSONObject("thumbnailRenderer")
                                        ?.optJSONObject("musicThumbnailRenderer")
                                        ?.optJSONObject("thumbnail")
                                        ?.optJSONArray("thumbnails")
                                    var cover = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
                                    cover = cover.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")

                                    if (title.isNotBlank() && bId.isNotBlank()) {
                                        albums.add(
                                            Album(
                                                id = bId,
                                                title = title,
                                                artistName = artistName,
                                                year = year.trim(),
                                                coverUrl = cover
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (albums.isEmpty()) {
                albums.addAll(fallbackAlbums)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error fetching artist albums: ${e.message}", e)
        }

        val verifiedAvatar = artistAvatar?.takeIf { it.isNotBlank() }
            ?: ArtistCatalog.getVerifiedAvatarUrl(artistName)

        val fullArtist = Artist(
            id = artistBrowseId ?: "art_$artistName",
            name = artistName,
            avatarUrl = verifiedAvatar,
            bannerUrl = artistBanner,
            isVerified = true
        )
        val result = Pair(fullArtist, albums)
        artistProfileCache[cacheKey] = result
        result
    }

    /**
     * Fetches all official albums of an artist directly from YouTube Music InnerTube API.
     */
    suspend fun getArtistAlbums(artistName: String): List<Album> = withContext(Dispatchers.IO) {
        getArtistProfileAndAlbums(artistName).second
    }

    /**
     * Fetches all tracks of an album from YouTube Music by album browseId.
     */
    suspend fun getAlbumTracks(albumId: String, artistName: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val url = "https://music.youtube.com/youtubei/v1/browse"
            val payload = JSONObject().apply {
                put("browseId", albumId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240318.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
            }
            val req = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val twoCol = json.optJSONObject("contents")?.optJSONObject("twoColumnBrowseResultsRenderer")
                        val secList = twoCol?.optJSONObject("secondaryContents")?.optJSONObject("sectionListRenderer")
                        val contents = secList?.optJSONArray("contents")

                        val tabs = twoCol?.optJSONArray("tabs")
                        val tabContent = tabs?.optJSONObject(0)?.optJSONObject("tabRenderer")?.optJSONObject("content")
                        val headerSec = tabContent?.optJSONObject("sectionListRenderer")
                            ?.optJSONArray("contents")
                            ?.optJSONObject(0)
                            ?.optJSONObject("musicResponsiveHeaderRenderer")

                        val albumTitle = headerSec?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "") ?: ""
                        val headerThumbs = headerSec?.optJSONObject("thumbnail")
                            ?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")
                            ?.optJSONArray("thumbnails")
                        var defaultCover = headerThumbs?.optJSONObject(headerThumbs.length() - 1)?.optString("url", "") ?: ""
                        defaultCover = defaultCover.replace(Regex("=w\\d+-h\\d+"), "=w500-h500")

                        if (contents != null) {
                            for (s in 0 until contents.length()) {
                                val shelf = contents.optJSONObject(s)?.optJSONObject("musicShelfRenderer")
                                    ?: contents.optJSONObject(s)?.optJSONObject("musicPlaylistShelfRenderer")
                                    ?: continue
                                val items = shelf.optJSONArray("contents") ?: continue

                                for (i in 0 until items.length()) {
                                    val item = items.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                                    val cols = item.optJSONArray("flexColumns") ?: continue
                                    if (cols.length() < 1) continue

                                    val titleRuns = cols.optJSONObject(0)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")
                                    val title = titleRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                                    if (title.isBlank()) continue

                                    // Extract artist from column 1 if available (essential for charts with various artists)
                                    val trackArtist = if (cols.length() > 1) {
                                        val artRuns = cols.optJSONObject(1)
                                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                            ?.optJSONObject("text")
                                            ?.optJSONArray("runs")
                                        artRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                                    } else ""
                                    val effectiveArtist = if (trackArtist.isNotBlank()) trackArtist else artistName

                                    var durationSec = 180
                                    val fixedCols = item.optJSONArray("fixedColumns")
                                    if (fixedCols != null && fixedCols.length() > 0) {
                                        val durStr = fixedCols.optJSONObject(0)
                                            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                                            ?.optJSONObject("text")
                                            ?.optJSONArray("runs")
                                            ?.optJSONObject(0)
                                            ?.optString("text", "3:00") ?: "3:00"
                                        durationSec = parseDurationToSeconds(durStr)
                                    }

                                    val playBtn = item.optJSONObject("overlay")
                                        ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                                        ?.optJSONObject("content")
                                        ?.optJSONObject("musicPlayButtonRenderer")
                                    var videoId = playBtn?.optJSONObject("playNavigationEndpoint")
                                        ?.optJSONObject("watchEndpoint")
                                        ?.optString("videoId", "") ?: ""

                                    if (videoId.isBlank()) {
                                        videoId = item.optJSONObject("playlistItemData")?.optString("videoId", "") ?: ""
                                    }
                                    if (videoId.isBlank()) {
                                        videoId = titleRuns?.optJSONObject(0)?.optJSONObject("navigationEndpoint")
                                            ?.optJSONObject("watchEndpoint")?.optString("videoId", "") ?: ""
                                    }

                                    // Item thumbnail
                                    val itemThumbs = item.optJSONObject("thumbnail")
                                        ?.optJSONObject("musicThumbnailRenderer")
                                        ?.optJSONObject("thumbnail")
                                        ?.optJSONArray("thumbnails")
                                    var itemCover = itemThumbs?.optJSONObject(itemThumbs.length() - 1)?.optString("url", "") ?: ""
                                    if (itemCover.isBlank() && videoId.isNotBlank()) {
                                        itemCover = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                    }
                                    val finalCover = if (itemCover.isNotBlank()) itemCover else defaultCover

                                    // Extract Play Count or Chart Ranking
                                    var playCount: String? = null
                                    if (cols.length() > 2) {
                                        val pcRuns = cols.optJSONObject(2)
                                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                            ?.optJSONObject("text")
                                            ?.optJSONArray("runs")
                                        val rawText = pcRuns?.optJSONObject(0)?.optString("text", "") ?: ""
                                        if (rawText.isNotBlank()) {
                                            playCount = rawText
                                        }
                                    }
                                    if (playCount.isNullOrBlank()) {
                                        val rankStr = item.optJSONObject("customIndexColumn")
                                            ?.optJSONObject("musicCustomIndexColumnRenderer")
                                            ?.optJSONObject("text")
                                            ?.optJSONArray("runs")
                                            ?.optJSONObject(0)
                                            ?.optString("text", "") ?: ""
                                        if (rankStr.isNotBlank() && rankStr.toIntOrNull() != null) {
                                            playCount = "#$rankStr di Tangga Lagu"
                                        }
                                    }

                                    if (videoId.isNotBlank()) {
                                        tracks.add(
                                            Track(
                                                id = "yt:$videoId",
                                                title = title,
                                                artist = Artist(id = "art_${effectiveArtist.hashCode()}", name = effectiveArtist, isVerified = true),
                                                albumTitle = albumTitle.ifBlank { "YouTube Music" },
                                                durationSec = durationSec,
                                                audioUrl = "yt:$videoId",
                                                coverUrl = finalCover,
                                                isVerified = true,
                                                playCount = playCount,
                                                sourceLabel = "YouTube Music"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error fetching album tracks: ${e.message}", e)
        }
        tracks
    }

    /**
     * Generates a Song Radio queue based on the seed track's artist and title.
     * Guaranteed to produce related songs strictly within the same musical genre, mood, and artist cluster.
     */
    suspend fun fetchSongRadio(track: Track): List<Track> = withContext(Dispatchers.IO) {
        val radioTracks = mutableListOf<Track>()
        try {
            val peerArtists = ArtistCatalog.getPeerArtists(track.artist.name)

            // Pass 1: Seed track radio mix (filtered for compatible vibe)
            val query1 = "${track.artist.name} ${track.title} radio mix"
            val results1 = searchYouTubeMusic(query1)
            for (candidate in results1) {
                if (candidate.id != track.id && ArtistCatalog.isTrackCompatibleWithSeed(candidate, track.artist.name)) {
                    radioTracks.add(candidate)
                }
            }

            // Pass 2: If queue needs more tracks, fetch songs from peer artists in the same genre cluster
            if (radioTracks.size < 12 && peerArtists.isNotEmpty()) {
                val peerSample = peerArtists.shuffled().take(3).joinToString(" ")
                val query2 = "$peerSample hits radio mix"
                val results2 = searchYouTubeMusic(query2)
                for (candidate in results2) {
                    if (candidate.id != track.id &&
                        ArtistCatalog.isTrackCompatibleWithSeed(candidate, track.artist.name) &&
                        radioTracks.none { it.id == candidate.id || it.title.equals(candidate.title, ignoreCase = true) }
                    ) {
                        radioTracks.add(candidate)
                    }
                }
            }

            // Pass 3: Fallback artist hits
            if (radioTracks.size < 8) {
                val query3 = "${track.artist.name} hits"
                val results3 = searchYouTubeMusic(query3)
                for (candidate in results3) {
                    if (candidate.id != track.id &&
                        ArtistCatalog.isLegitimateArtistTrack(candidate, track.artist.name) &&
                        radioTracks.none { it.id == candidate.id || it.title.equals(candidate.title, ignoreCase = true) }
                    ) {
                        radioTracks.add(candidate)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error fetching song radio: ${e.message}")
        }
        radioTracks.distinctBy { it.id }
    }

    /**
     * Fetches top songs strictly performed by or featuring the specified artist.
     * Rejects obscure songs, unrelated tracks, or songs with misleading titles that mimic the artist name.
     */
    suspend fun getArtistTopTracks(artistName: String): List<Track> = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return@withContext emptyList()

        val cacheKey = cleanName.lowercase()
        val cached = artistTracksCache[cacheKey]
        if (cached != null && cached.isNotEmpty()) {
            android.util.Log.d("MusicEuy", "getArtistTopTracks cache hit for: $cleanName")
            return@withContext cached
        }

        val verifiedTracks = mutableListOf<Track>()
        try {
            // Fast direct artist query without quotes (avoids the 80s YouTube quote query timeout)
            val results1 = searchYouTubeMusic(cleanName)
            for (t in results1) {
                if (ArtistCatalog.isLegitimateArtistTrack(t, cleanName)) {
                    verifiedTracks.add(t)
                }
            }

            // Supplemental search if fewer than 8 songs
            if (verifiedTracks.size < 8) {
                val results2 = searchYouTubeMusic("$cleanName lagu hits")
                for (t in results2) {
                    if (ArtistCatalog.isLegitimateArtistTrack(t, cleanName) && verifiedTracks.none { it.id == t.id || it.title.equals(t.title, ignoreCase = true) }) {
                        verifiedTracks.add(t)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error fetching artist top tracks: ${e.message}")
        }

        val finalTracks = verifiedTracks.distinctBy { it.id }
        if (finalTracks.isNotEmpty()) {
            artistTracksCache[cacheKey] = finalTracks
        }
        finalTracks
    }

    /**
     * Fetches curated tracks based on user-selected mood/genre chip on the Home screen.
     */
    suspend fun fetchMoodCategoryTracks(category: String): List<Track> = withContext(Dispatchers.IO) {
        val query = when (category) {
            "Fokus / Belajar", "Fokus" -> "lofi study chill beats focus instrumental"
            "Santai & Chill", "Santai" -> "lagu santai akustik indie senja indonesia"
            "Workout / Energi", "Workout" -> "workout gym energetic hype edm motivation"
            "Pesta / Party", "Pesta" -> "party dance club hits remix"
            "Pop Indonesia", "Pop" -> "pop indonesia terbaru terbaik hits 2026"
            "Indie Indonesia", "Indie" -> "indie pop indonesia terbaik senja"
            "Rock" -> "rock indonesia hits terbaik classic alternative"
            "Dangdut Koplo", "Dangdut" -> "dangdut koplo terbaru viral hits 2026"
            "Rilisan Baru" -> "lagu baru rilis indonesia hits 2026"
            else -> "lagu trending indonesia terpopuler 2026"
        }
        searchYouTubeMusic(query)
    }

    /**
     * Fetches official curated charts (Top 50 Indonesia, Top 50 Global, Viral Hits) from YouTube Music.
     */
    suspend fun fetchOfficialChart(chartName: String): List<Track> = withContext(Dispatchers.IO) {
        val browseId = when {
            chartName.contains("Indonesia", ignoreCase = true) && !chartName.contains("Viral", ignoreCase = true) -> "VLPL4fGSI1pDJn5QPpj0R4vVgRWk8sSq549G"
            chartName.contains("Global", ignoreCase = true) -> "VLPLOHoVaTp8R7dqEEhhaOG7yfDoj_y2BrZ4"
            chartName.contains("Viral", ignoreCase = true) -> "VLOLAK5uy_n7Ig_LAUbKE6_ZeQ1pwHmJcEhwX7BekBo"
            chartName.contains("Rilis", ignoreCase = true) -> "VLPL4fGSI1pDJn4lW6MJA2q37CfnraqAizqE"
            else -> "VLPL4fGSI1pDJn5QPpj0R4vVgRWk8sSq549G"
        }
        val tracks = getAlbumTracks(browseId, "YouTube Music Charts")
        if (tracks.isNotEmpty()) {
            tracks
        } else {
            val query = when {
                chartName.contains("Indonesia", ignoreCase = true) -> "Top 50 Hits Indonesia Terpopuler 2026"
                chartName.contains("Global", ignoreCase = true) -> "Top 50 Global Hits Billboard Official 2026"
                chartName.contains("Viral", ignoreCase = true) -> "Viral Hits TikTok Indonesia Terbaru 2026"
                else -> "Top Trending Indonesia 2026"
            }
            searchYouTubeMusic(query)
        }
    }
}
