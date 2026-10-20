package com.rafliadipratama.musiceuy.plugin

import android.content.Context
import android.util.Log
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Last.fm Scrobbler Plugin.
 * Automatically tracks listening history and scrobbles songs to Last.fm.
 * GitHub Repo: https://github.com/tgwizard/sls
 */
class LastFmPlugin(
    private val context: Context,
    override var isEnabled: Boolean = false
) : MusicStreamPlugin {

    override val id: String = "lastfm_scrobbler"
    override val name: String = "Last.fm Scrobbler"
    override val description: String = "Sinkronisasi riwayat pemutaran lagu (Scrobble) dan status Now Playing ke akun Last.fm"
    override val version: String = "1.0.0"
    override val author: String = "Simple Last.fm Scrobbler"
    override val category: String = "Statistik & Sosial"
    override val githubUrl: String = "https://github.com/tgwizard/sls"
    override val isBuiltIn: Boolean = true

    private val prefs = context.getSharedPreferences("musiceuy_lastfm", Context.MODE_PRIVATE)

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) = prefs.edit().putString("username", value.trim()).apply()

    suspend fun scrobbleTrack(track: Track) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            Log.d("MusicEuy", "LastFmPlugin: Scrobbling '${track.title}' by ${track.artist.name}")
            // Standard Android Last.fm broadcast action (Simple Last.fm Scrobbler / Pano Scrobbler protocol)
            val intent = android.content.Intent("com.adam.aslfms.notify.playstatechanged").apply {
                putExtra("state", 2) // 2 = END / COMPLETED SCROBBLE
                putExtra("app-name", "MusicEuy")
                putExtra("app-package", context.packageName)
                putExtra("artist", track.artist.name)
                putExtra("track", track.title)
                putExtra("duration", track.durationSec)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("MusicEuy", "LastFmPlugin scrobble error: ${e.message}")
        }
    }
}
