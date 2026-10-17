package com.rafliadipratama.musiceuy.data.local

import android.content.Context
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Playlist
import com.rafliadipratama.musiceuy.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local database manager for user listening history, preferences, and playlists.
 * Operates 100% locally without requiring user accounts or cloud authentication.
 * Stores play counts, timestamps, most frequently played tracks, and custom playlists.
 */
class LocalMusicHistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("musiceuy_local_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TRACKS = "played_tracks_json"
        private const val KEY_PLAYLISTS = "playlists_json"
        private const val KEY_LIKED_TRACKS = "liked_tracks_json"
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
        val artistBanner = obj.optString("artistBanner", "").takeIf { it.isNotBlank() }
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
                bannerUrl = artistBanner,
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

    // ==================== Playlist Operations ====================

    fun createPlaylist(name: String, description: String = ""): Playlist {
        val playlist = Playlist(
            name = name.ifBlank { "Playlist Saya" },
            description = description,
            tracks = emptyList()
        )
        val all = getAllPlaylists().toMutableList()
        all.add(0, playlist)
        savePlaylists(all)
        return playlist
    }

    fun getAllPlaylists(): List<Playlist> {
        val jsonStr = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Playlist>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "Playlist")
                val desc = obj.optString("description", "")
                val coverUrl = obj.optString("coverUrl", "").takeIf { it.isNotBlank() }
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                val tracksArr = obj.optJSONArray("tracks") ?: JSONArray()
                val tracks = mutableListOf<Track>()
                for (j in 0 until tracksArr.length()) {
                    tracks.add(jsonToTrack(tracksArr.getJSONObject(j)))
                }
                list.add(
                    Playlist(
                        id = id,
                        name = name,
                        description = desc,
                        coverUrl = coverUrl ?: tracks.firstOrNull()?.coverUrl,
                        tracks = tracks,
                        createdAt = createdAt
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track): Boolean {
        val all = getAllPlaylists().toMutableList()
        val index = all.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        val current = all[index]
        if (current.tracks.any { it.id == track.id }) return false

        val updatedTracks = current.tracks.toMutableList().apply { add(track) }
        val updatedPlaylist = current.copy(
            tracks = updatedTracks,
            coverUrl = current.coverUrl ?: track.coverUrl
        )
        all[index] = updatedPlaylist
        savePlaylists(all)
        return true
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean {
        val all = getAllPlaylists().toMutableList()
        val index = all.indexOfFirst { it.id == playlistId }
        if (index == -1) return false

        val current = all[index]
        val updatedTracks = current.tracks.filterNot { it.id == trackId }
        val updatedPlaylist = current.copy(
            tracks = updatedTracks,
            coverUrl = updatedTracks.firstOrNull()?.coverUrl
        )
        all[index] = updatedPlaylist
        savePlaylists(all)
        return true
    }

    fun deletePlaylist(playlistId: String): Boolean {
        val all = getAllPlaylists().filterNot { it.id == playlistId }
        savePlaylists(all)
        return true
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        for (pl in playlists) {
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("description", pl.description)
                put("coverUrl", pl.coverUrl ?: "")
                put("createdAt", pl.createdAt)

                val tracksArr = JSONArray()
                for (t in pl.tracks) {
                    val tObj = JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("artistName", t.artist.name)
                        put("artistAvatar", t.artist.avatarUrl ?: "")
                        put("artistBanner", t.artist.bannerUrl ?: "")
                        put("albumTitle", t.albumTitle ?: "")
                        put("durationSec", t.durationSec)
                        put("audioUrl", t.audioUrl)
                        put("coverUrl", t.coverUrl)
                        put("isVerified", t.isVerified)
                        put("sourceLabel", t.sourceLabel)
                    }
                    tracksArr.put(tObj)
                }
                put("tracks", tracksArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    // ==================== Liked Tracks Operations ====================

    fun toggleLikedTrack(track: Track): Boolean {
        val currentList = getLikedTracks().toMutableList()
        val index = currentList.indexOfFirst { it.id == track.id }
        val isNowLiked = if (index >= 0) {
            currentList.removeAt(index)
            false
        } else {
            currentList.add(0, track.copy(isLiked = true))
            true
        }
        saveLikedTracks(currentList)
        return isNowLiked
    }

    fun isTrackLiked(trackId: String): Boolean {
        return getLikedTracks().any { it.id == trackId }
    }

    fun getLikedTracks(): List<Track> {
        val jsonStr = prefs.getString(KEY_LIKED_TRACKS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Track>()
            for (i in 0 until arr.length()) {
                list.add(jsonToTrack(arr.getJSONObject(i)).copy(isLiked = true))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLikedTracks(tracks: List<Track>) {
        val arr = JSONArray()
        for (t in tracks) {
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("artistName", t.artist.name)
                put("artistAvatar", t.artist.avatarUrl ?: "")
                put("albumTitle", t.albumTitle ?: "")
                put("durationSec", t.durationSec)
                put("audioUrl", t.audioUrl)
                put("coverUrl", t.coverUrl)
                put("isVerified", t.isVerified)
                put("sourceLabel", t.sourceLabel)
            }
            arr.put(tObj)
        }
        prefs.edit().putString(KEY_LIKED_TRACKS, arr.toString()).apply()
    }

    // ==================== Backup & Restore ====================

    fun exportBackupJson(): String {
        val root = JSONObject().apply {
            put("app", "MusicEuy")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("playlists", JSONArray(prefs.getString(KEY_PLAYLISTS, "[]") ?: "[]"))
            put("likedTracks", JSONArray(prefs.getString(KEY_LIKED_TRACKS, "[]") ?: "[]"))
        }
        return root.toString(2)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("playlists") && !root.has("likedTracks")) {
                return false
            }

            if (root.has("playlists")) {
                val incomingPlaylists = root.getJSONArray("playlists")
                val existingPlaylists = getAllPlaylists().toMutableList()
                for (i in 0 until incomingPlaylists.length()) {
                    val pObj = incomingPlaylists.getJSONObject(i)
                    val pId = pObj.optString("id", "")
                    if (existingPlaylists.none { it.id == pId }) {
                        val tracksList = mutableListOf<Track>()
                        val tArr = pObj.optJSONArray("tracks") ?: JSONArray()
                        for (j in 0 until tArr.length()) {
                            tracksList.add(jsonToTrack(tArr.getJSONObject(j)))
                        }
                        existingPlaylists.add(
                            Playlist(
                                id = pId.ifBlank { "pl_${System.currentTimeMillis()}_$i" },
                                name = pObj.optString("name", "Playlist Impor"),
                                description = pObj.optString("description", ""),
                                coverUrl = if (pObj.optString("coverUrl", "").isNotBlank()) pObj.optString("coverUrl") else null,
                                createdAt = pObj.optLong("createdAt", System.currentTimeMillis()),
                                tracks = tracksList
                            )
                        )
                    }
                }
                savePlaylists(existingPlaylists)
            }

            if (root.has("likedTracks")) {
                val incomingLiked = root.getJSONArray("likedTracks")
                val existingLiked = getLikedTracks().toMutableList()
                val existingIds = existingLiked.map { it.id }.toSet()
                for (i in 0 until incomingLiked.length()) {
                    val t = jsonToTrack(incomingLiked.getJSONObject(i))
                    if (!existingIds.contains(t.id)) {
                        existingLiked.add(t.copy(isLiked = true))
                    }
                }
                saveLikedTracks(existingLiked)
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error importing backup: ${e.message}", e)
            false
        }
    }
}

