package com.rafliadipratama.musiceuy.data.network

import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SpotifyPublicSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        const val PLAYLIST_TOP_50_INDONESIA = "37i9dQZEVXbObFQZ3JLcXt"
        const val PLAYLIST_TODAYS_TOP_HITS = "37i9dQZF1DXcBWIGoYBM5M"
        const val PLAYLIST_CHILL_HITS = "37i9dQZF1DX4WYpdgoIcn6"
        const val PLAYLIST_NEW_MUSIC_FRIDAY = "37i9dQZF1DX4JAvHpjipBk"
        const val PLAYLIST_RAPCAVIAR = "37i9dQZF1DX0XUsuxWHRQd"
    }

    private val jsonPattern = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>")
    private val idPattern = Pattern.compile("([a-zA-Z0-9]{22})")

    /**
     * Extracts Spotify playlist ID from direct ID or full open.spotify.com URL
     */
    fun extractPlaylistId(input: String): String? {
        val trimmed = input.trim()
        val matcher = idPattern.matcher(trimmed)
        return if (matcher.find()) matcher.group(1) else null
    }

    /**
     * Fetches public Spotify playlist metadata without any user login.
     * Extracts track names, artists, durations, and cover art.
     */
    suspend fun fetchPlaylist(playlistIdOrUrl: String): List<Track> = withContext(Dispatchers.IO) {
        val playlistId = extractPlaylistId(playlistIdOrUrl) ?: return@withContext emptyList()
        val url = "https://open.spotify.com/embed/playlist/$playlistId"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "id,en-US;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string() ?: return@withContext emptyList()

                val matcher = jsonPattern.matcher(html)
                if (!matcher.find()) return@withContext emptyList()

                val jsonStr = matcher.group(1) ?: return@withContext emptyList()
                val root = JSONObject(jsonStr)
                val entity = root.optJSONObject("props")
                    ?.optJSONObject("pageProps")
                    ?.optJSONObject("state")
                    ?.optJSONObject("data")
                    ?.optJSONObject("entity") ?: return@withContext emptyList()

                val playlistTitle = entity.optString("title", "Spotify Playlist")
                val defaultCover = entity.optJSONObject("coverArt")
                    ?.optJSONArray("sources")
                    ?.optJSONObject(0)
                    ?.optString("url")
                    ?: "https://charts-images.scdn.co/assets/locale_en/regional/daily/region_id_default.jpg"

                val trackList = entity.optJSONArray("trackList") ?: return@use emptyList()
                val result = mutableListOf<Track>()

                for (i in 0 until trackList.length()) {
                    val item = trackList.optJSONObject(i) ?: continue
                    val title = item.optString("title", "").trim()
                    if (title.isBlank()) continue

                    val rawArtist = item.optString("subtitle", "Artis Spotify")
                    val artistName = rawArtist.replace("\u00A0", " ").trim()

                    val durationMs = item.optInt("duration", 0)
                    val durationSec = durationMs / 1000

                    val uri = item.optString("uri", "spotify:track:$i")
                    val trackId = "sp:" + uri.removePrefix("spotify:track:")

                    val audioPlaceholder = "sp:$artistName:::$title"

                    result.add(
                        Track(
                            id = trackId,
                            title = title,
                            artist = Artist(
                                id = "sp_art_$i",
                                name = artistName,
                                avatarUrl = defaultCover,
                                isVerified = true
                            ),
                            albumTitle = playlistTitle,
                            durationSec = durationSec,
                            audioUrl = audioPlaceholder,
                            coverUrl = defaultCover,
                            isLiked = false,
                            isVerified = true,
                            sourceLabel = "Spotify • $playlistTitle"
                        )
                    )
                }

                android.util.Log.d("MusicEuy", "Fetched ${result.size} tracks from Spotify playlist: $playlistTitle")
                return@withContext result
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "fetchPlaylist error for $playlistId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Top 50 Indonesia official daily chart from Spotify
     */
    suspend fun getTop50Indonesia(): List<Track> {
        val tracks = fetchPlaylist(PLAYLIST_TOP_50_INDONESIA)
        return if (tracks.isNotEmpty()) tracks else fetchKworbFallback()
    }

    /**
     * Today's Top Hits official Spotify playlist
     */
    suspend fun getTodaysTopHits(): List<Track> {
        return fetchPlaylist(PLAYLIST_TODAYS_TOP_HITS)
    }

    /**
     * Chill Hits playlist
     */
    suspend fun getChillHits(): List<Track> {
        return fetchPlaylist(PLAYLIST_CHILL_HITS)
    }

    /**
     * Fallback for Top Indonesia tracks via public kworb daily chart if Spotify embed is unreachable
     */
    private suspend fun fetchKworbFallback(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val request = Request.Builder()
                .url("https://kworb.net/spotify/country/id_daily.html")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string() ?: return@withContext emptyList()

                val pattern = Pattern.compile("<td class=\"text mp\"><div>(.*?)</div></td>")
                val matcher = pattern.matcher(html)
                var count = 0
                val defaultCover = "https://charts-images.scdn.co/assets/locale_en/regional/daily/region_id_default.jpg"

                while (matcher.find() && count < 50) {
                    val rawText = matcher.group(1)?.replace(Regex("<[^>]+>"), "") ?: continue
                    val parts = rawText.split(" - ", limit = 2)
                    if (parts.size == 2) {
                        val artist = parts[0].trim()
                        val title = parts[1].replace(Regex("\\(w/.*\\)"), "").trim()
                        tracks.add(
                            Track(
                                id = "sp_kworb_$count",
                                title = title,
                                artist = Artist(id = "sp_art_$count", name = artist, avatarUrl = defaultCover, isVerified = true),
                                albumTitle = "Spotify Top 50 Indonesia",
                                durationSec = 210,
                                audioUrl = "sp:$artist:::$title",
                                coverUrl = defaultCover,
                                isLiked = false,
                                isVerified = true,
                                sourceLabel = "Spotify • Top 50 Indonesia"
                            )
                        )
                        count++
                    }
                }
            }
        } catch (_: Exception) {}
        tracks
    }
}
