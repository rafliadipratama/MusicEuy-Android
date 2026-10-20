package com.rafliadipratama.musiceuy.data.network

import android.util.Log
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Playlist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ImportedPlaylistResult(
    val playlist: Playlist,
    val tracks: List<Track>,
    val sourceName: String
)

class PlaylistImporter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "PlaylistImporter"
        private val YT_PLAYLIST_PATTERN = Pattern.compile("(?:list=)([a-zA-Z0-9_-]+)")
        private val SPOTIFY_PLAYLIST_PATTERN = Pattern.compile("open\\.spotify\\.com/playlist/([a-zA-Z0-9]+)")
    }

    suspend fun importFromUrl(url: String, customName: String? = null): ImportedPlaylistResult = withContext(Dispatchers.IO) {
        val trimmed = url.trim()

        if (trimmed.contains("spotify.com")) {
            return@withContext importSpotifyPlaylist(trimmed, customName)
        } else if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
            return@withContext importYouTubePlaylist(trimmed, customName)
        } else {
            throw IllegalArgumentException("URL tidak didukung. Masukkan tautan playlist publik Spotify atau YouTube.")
        }
    }

    private fun importYouTubePlaylist(url: String, customName: String?): ImportedPlaylistResult {
        val matcher = YT_PLAYLIST_PATTERN.matcher(url)
        val playlistId = if (matcher.find()) matcher.group(1) else null
            ?: throw IllegalArgumentException("ID Playlist YouTube tidak ditemukan pada link.")

        val canonicalUrl = "https://www.youtube.com/playlist?list=$playlistId"
        Log.d(TAG, "Importing YouTube playlist: $canonicalUrl")

        try {
            val extractor = ServiceList.YouTube.getPlaylistExtractor(canonicalUrl)
            extractor.fetchPage()

            val playlistTitle = customName?.ifBlank { null }
                ?: extractor.name.ifBlank { "Playlist YouTube Import" }
            val playlistThumbnail = extractor.thumbnails?.firstOrNull()?.url ?: ""

            val tracks = mutableListOf<Track>()
            val items = extractor.initialPage?.items ?: emptyList()

            for (item in items) {
                if (item is StreamInfoItem) {
                    val rawUrl = item.url ?: ""
                    val videoId = if (rawUrl.contains("v=")) {
                        rawUrl.substringAfter("v=").substringBefore("&")
                    } else {
                        rawUrl.substringAfterLast("/")
                    }

                    if (videoId.isNotBlank()) {
                        tracks.add(
                            Track(
                                id = "yt:$videoId",
                                title = item.name ?: "Tanpa Judul",
                                artist = Artist(
                                    id = item.uploaderUrl ?: "yt_artist",
                                    name = item.uploaderName ?: "Artis YouTube"
                                ),
                                audioUrl = "yt:$videoId",
                                coverUrl = item.thumbnails?.firstOrNull()?.url ?: playlistThumbnail.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                                durationSec = if (item.duration > 0) item.duration.toInt() else 210,
                                sourceLabel = "YouTube"
                            )
                        )
                    }
                }
            }

            if (tracks.isEmpty()) {
                throw IllegalStateException("Playlist YouTube kosong atau bersifat pribadi (private).")
            }

            val playlist = Playlist(
                id = "pl_yt_${UUID.randomUUID()}",
                name = playlistTitle,
                description = "Diimpor dari YouTube Playlist (${tracks.size} lagu)",
                coverUrl = playlistThumbnail.ifBlank { tracks.firstOrNull()?.coverUrl ?: "" },
                tracks = tracks
            )

            return ImportedPlaylistResult(playlist, tracks, "YouTube")
        } catch (e: Exception) {
            Log.e(TAG, "Error importing YouTube playlist: ${e.message}", e)
            throw IllegalStateException("Gagal mengambil playlist YouTube: ${e.message ?: "Pastikan playlist publik"}")
        }
    }

    private fun importSpotifyPlaylist(url: String, customName: String?): ImportedPlaylistResult {
        val matcher = SPOTIFY_PLAYLIST_PATTERN.matcher(url)
        val playlistId = if (matcher.find()) matcher.group(1) else null
            ?: throw IllegalArgumentException("ID Playlist Spotify tidak valid pada tautan.")

        val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
        Log.d(TAG, "Importing Spotify playlist via embed: $embedUrl")

        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Gagal menghubungi Spotify (HTTP ${response.code}).")
            }

            val html = response.body?.string()
                ?: throw IllegalStateException("Respon dari Spotify kosong.")

            val scriptStartTag = "<script id=\"__NEXT_DATA__\" type=\"application/json\">"
            val startIndex = html.indexOf(scriptStartTag)
            if (startIndex == -1) {
                throw IllegalStateException("Format data Spotify tidak dapat diuraikan.")
            }

            val jsonStart = startIndex + scriptStartTag.length
            val jsonEnd = html.indexOf("</script>", jsonStart)
            if (jsonEnd == -1) {
                throw IllegalStateException("Penutup data Spotify tidak ditemukan.")
            }

            val jsonContent = html.substring(jsonStart, jsonEnd)
            val root = JSONObject(jsonContent)
            val entity = root.getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("state")
                .getJSONObject("data")
                .getJSONObject("entity")

            val rawTitle = entity.optString("title", "Playlist Spotify")
            val playlistTitle = customName?.ifBlank { null } ?: rawTitle

            val visualIdentity = entity.optJSONObject("visualIdentity")
            val imageArr = visualIdentity?.optJSONArray("image")
            val playlistCover = if (imageArr != null && imageArr.length() > 0) {
                imageArr.getJSONObject(imageArr.length() - 1).optString("url", "")
            } else ""

            val trackList = entity.optJSONArray("trackList")
                ?: throw IllegalStateException("Daftar lagu pada playlist Spotify ini kosong.")

            val tracks = mutableListOf<Track>()
            for (i in 0 until trackList.length()) {
                val item = trackList.getJSONObject(i)
                val trackTitle = item.optString("title", "").trim()
                val artistName = item.optString("subtitle", "").trim()
                val durationMs = item.optLong("duration", 0L)
                val durationSec = if (durationMs > 0) (durationMs / 1000).toInt() else 210

                if (trackTitle.isNotBlank()) {
                    val safeId = "sp:${trackTitle}_$artistName"
                        .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        .lowercase()
                    tracks.add(
                        Track(
                            id = safeId,
                            title = trackTitle,
                            artist = Artist(
                                id = "sp_art_${kotlin.math.abs(artistName.hashCode())}",
                                name = artistName.ifBlank { "Artis Spotify" }
                            ),
                            audioUrl = "", // resolved dynamically when played via YouTubeMusicPlugin
                            coverUrl = playlistCover,
                            durationSec = durationSec,
                            sourceLabel = "Spotify"
                        )
                    )
                }
            }

            if (tracks.isEmpty()) {
                throw IllegalStateException("Tidak ada lagu yang ditemukan di playlist Spotify.")
            }

            val playlist = Playlist(
                id = "pl_sp_${UUID.randomUUID()}",
                name = playlistTitle,
                description = "Diimpor dari Spotify Playlist (${tracks.size} lagu)",
                coverUrl = playlistCover.ifBlank { tracks.firstOrNull()?.coverUrl ?: "" },
                tracks = tracks
            )

            return ImportedPlaylistResult(playlist, tracks, "Spotify")
        }
    }
}
