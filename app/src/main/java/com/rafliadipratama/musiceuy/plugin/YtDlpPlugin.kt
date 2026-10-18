package com.rafliadipratama.musiceuy.plugin

import android.content.Context
import android.util.Log
import com.rafliadipratama.musiceuy.data.model.Track
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * yt-dlp Universal Audio Resolver & Downloader Plugin.
 * Powers resilient audio extraction across 1,800+ sites, ID3 tagging, and OTA binary updates.
 * GitHub Repo: https://github.com/yt-dlp/yt-dlp
 */
class YtDlpPlugin(
    private val context: Context,
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    companion object {
        private const val TAG = "YtDlpPlugin"
    }

    override val id: String = "ytdlp_engine"
    override val name: String = "yt-dlp Universal Engine"
    override val description: String = "Mesin ekstraksi tangguh & serbaguna (1.800+ situs) dengan fallback otomatis dan dukungan OTA update"
    override val version: String
        get() = try {
            YoutubeDL.getInstance().version(context) ?: "2025.x"
        } catch (_: Exception) {
            "2025.x"
        }
    override val author: String = "yt-dlp & JunkFood02"
    override val category: String = "Audio Stream"
    override val githubUrl: String = "https://github.com/yt-dlp/yt-dlp"
    override val isBuiltIn: Boolean = true

    /**
     * Resolves the best available audio stream URL for a given track.
     * Can extract from YouTube Video IDs, YouTube Music links, or any arbitrary URL.
     */
    override suspend fun resolveStream(track: Track): String? = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext null

        val targetUrl = when {
            track.id.startsWith("ytdlp:") -> track.id.removePrefix("ytdlp:")
            track.audioUrl.startsWith("ytdlp:") -> track.audioUrl.removePrefix("ytdlp:")
            track.id.startsWith("yt:") -> "https://www.youtube.com/watch?v=${track.id.removePrefix("yt:")}"
            track.audioUrl.startsWith("yt:") -> "https://www.youtube.com/watch?v=${track.audioUrl.removePrefix("yt:")}"
            track.audioUrl.startsWith("http://") || track.audioUrl.startsWith("https://") -> track.audioUrl
            else -> "https://www.youtube.com/watch?v=${track.id}"
        }

        try {
            val request = YoutubeDLRequest(targetUrl).apply {
                addOption("-f", "ba/b") // best audio or best format
                addOption("--no-playlist")
                addOption("--no-warnings")
                addOption("--socket-timeout", "10")
            }

            val videoInfo = YoutubeDL.getInstance().getInfo(request)
            val streamUrl = videoInfo.url?.ifBlank { null }
                ?: videoInfo.formats?.filter { !it.url.isNullOrBlank() }
                    ?.maxByOrNull { it.abr }
                    ?.url

            if (!streamUrl.isNullOrBlank()) {
                Log.d(TAG, "Successfully resolved audio via yt-dlp for '${track.title}': $streamUrl")
                return@withContext streamUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "yt-dlp extraction failed for track '${track.title}': ${e.message}")
        }
        null
    }

    /**
     * Checks and applies Over-The-Air (OTA) updates for yt-dlp directly inside the app.
     */
    suspend fun updateEngine(): String = withContext(Dispatchers.IO) {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            when (status) {
                YoutubeDL.UpdateStatus.DONE -> "yt-dlp berhasil diperbarui ke versi ${YoutubeDL.getInstance().version(context)}!"
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp sudah versi terbaru (${YoutubeDL.getInstance().version(context)})."
                else -> "Status pembaruan: $status"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update yt-dlp", e)
            "Gagal memperbarui yt-dlp: ${e.message}"
        }
    }
}
