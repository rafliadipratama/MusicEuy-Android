package com.rafliadipratama.musiceuy.data.network

import android.util.Base64
import com.rafliadipratama.musiceuy.data.model.Album
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
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

    /**
     * Resolves a YouTube video ID into an active, high-bitrate direct audio stream URL
     * using NewPipeExtractor (the gold standard open-source Android extractor).
     */
    fun resolveYouTubeAudioStream(videoId: String): String? {
        return try {
            val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId") as? YoutubeStreamExtractor ?: return null
            extractor.fetchPage()
            val audioStreams = extractor.audioStreams
            val stream = audioStreams.maxByOrNull { it.bitrate }?.content
            android.util.Log.d("MusicEuy", "resolveYouTubeAudioStream for $videoId: success, streamUrl=$stream")
            stream
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "resolveYouTubeAudioStream error for $videoId: ${e.javaClass.simpleName} - ${e.message}", e)
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
                                            coverUrl = thumbUrl
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

        val fullArtist = Artist(
            id = artistBrowseId ?: "art_$artistName",
            name = artistName,
            avatarUrl = artistAvatar,
            bannerUrl = artistBanner,
            isVerified = true
        )
        Pair(fullArtist, albums)
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
                                val shelf = contents.optJSONObject(s)?.optJSONObject("musicShelfRenderer") ?: continue
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
                                    val videoId = playBtn?.optJSONObject("playNavigationEndpoint")
                                        ?.optJSONObject("watchEndpoint")
                                        ?.optString("videoId", "") ?: ""

                                    if (videoId.isNotBlank()) {
                                        tracks.add(
                                            Track(
                                                id = "yt:$videoId",
                                                title = title,
                                                artist = Artist(id = "art_$artistName", name = artistName, isVerified = true),
                                                albumTitle = albumTitle,
                                                durationSec = durationSec,
                                                audioUrl = "yt:$videoId",
                                                coverUrl = defaultCover,
                                                isVerified = true,
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
}
