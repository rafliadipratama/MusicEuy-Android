package com.rafliadipratama.musiceuy.data.local

import android.content.Context
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local database manager for user listening history and preferences.
 * Operates 100% locally without requiring user accounts or cloud authentication.
 * Stores play counts, timestamps, and most frequently played tracks.
 */
class LocalMusicHistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("musiceuy_local_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TRACKS = "played_tracks_json"
    }

    /**
     * Record a played track: increments its play count and updates the last played timestamp.
     */
    fun recordTrackPlayed(track: Track) {
        try {
            val currentMap = loadTracksMap()
            val existing = currentMap[track.id]

            val count = (existing?.optInt("playCount", 0) ?: 0) + 1
            val timestamp = System.currentTimeMillis()

            val entry = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artistName", track.artist.name)
                put("artistAvatar", track.artist.avatarUrl ?: "")
                put("albumTitle", track.albumTitle ?: "")
                put("durationSec", track.durationSec)
                put("audioUrl", track.audioUrl)
                put("coverUrl", track.coverUrl)
                put("isVerified", track.isVerified)
                put("sourceLabel", track.sourceLabel)
                put("playCount", count)
                put("lastPlayed", timestamp)
            }

            currentMap[track.id] = entry
            saveTracksMap(currentMap)
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error recording track play: ${e.message}", e)
        }
    }

    /**
     * Returns the tracks most frequently played by this user, ordered by play count descending.
     */
    fun getMostPlayedTracks(limit: Int = 15): List<Track> {
        return try {
            val currentMap = loadTracksMap()
            currentMap.values
                .sortedByDescending { it.optInt("playCount", 0) }
                .take(limit)
                .map { jsonToTrack(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns recently played tracks, ordered by last played timestamp descending.
     */
    fun getRecentlyPlayedTracks(limit: Int = 15): List<Track> {
        return try {
            val currentMap = loadTracksMap()
            currentMap.values
                .sortedByDescending { it.optLong("lastPlayed", 0L) }
                .take(limit)
                .map { jsonToTrack(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns the artist names that the user listens to most frequently.
     */
    fun getFavoriteArtists(limit: Int = 5): List<String> {
        return try {
            val currentMap = loadTracksMap()
            val artistCounts = mutableMapOf<String, Int>()
            for (entry in currentMap.values) {
                val artist = entry.optString("artistName", "")
                if (artist.isNotBlank()) {
                    val count = entry.optInt("playCount", 1)
                    artistCounts[artist] = (artistCounts[artist] ?: 0) + count
                }
            }
            artistCounts.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadTracksMap(): MutableMap<String, JSONObject> {
        val jsonStr = prefs.getString(KEY_TRACKS, null) ?: return mutableMapOf()
        val map = mutableMapOf<String, JSONObject>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                if (id.isNotBlank()) {
                    map[id] = obj
                }
            }
        } catch (_: Exception) {}
        return map
    }

    private fun saveTracksMap(map: Map<String, JSONObject>) {
        val arr = JSONArray()
        for (obj in map.values) {
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TRACKS, arr.toString()).apply()
    }

    private fun jsonToTrack(obj: JSONObject): Track {
        val id = obj.optString("id", "")
        val title = obj.optString("title", "")
        val artistName = obj.optString("artistName", "Artis")
        val artistAvatar = obj.optString("artistAvatar", "").takeIf { it.isNotBlank() }
        val albumTitle = obj.optString("albumTitle", "").takeIf { it.isNotBlank() }
        val durationSec = obj.optInt("durationSec", 0)
        val audioUrl = obj.optString("audioUrl", "")
        val coverUrl = obj.optString("coverUrl", "")
        val isVerified = obj.optBoolean("isVerified", true)
        val sourceLabel = obj.optString("sourceLabel", "YouTube Music")

        return Track(
            id = id,
            title = title,
            artist = Artist(
                id = "art_$artistName",
                name = artistName,
                avatarUrl = artistAvatar,
                isVerified = isVerified
            ),
            albumTitle = albumTitle,
            durationSec = durationSec,
            audioUrl = audioUrl,
            coverUrl = coverUrl,
            isLiked = false,
            isVerified = isVerified,
            sourceLabel = sourceLabel
        )
    }
}
