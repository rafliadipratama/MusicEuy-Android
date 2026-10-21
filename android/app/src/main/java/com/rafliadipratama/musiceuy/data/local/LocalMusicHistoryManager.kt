package com.rafliadipratama.musiceuy.data.local

import android.content.Context
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Playlist
import com.rafliadipratama.musiceuy.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

data class ListeningStats(
    val totalPlayCount: Int = 0,
    val estimatedMinutes: Long = 0L,
    val topArtists: List<Pair<String, Int>> = emptyList(),
    val topTracks: List<Track> = emptyList(),
    val favoriteArtist: String? = null
)

/**
 * Musicfy-style taste profile capturing mathematical time-decay listening weights,
 * positive liked affinity, negative skip suppression, and tri-directional seed candidates.
 */
data class TasteProfile(
    val heard: Set<String> = emptySet(),
    val avoidSongs: Set<String> = emptySet(),
    val avoidArtists: Set<String> = emptySet(),
    val songSeeds: List<Track> = emptyList(),
    val topArtists: List<String> = emptyList()
)

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
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ONBOARDING_ARTISTS = "onboarding_artists"
        private const val KEY_NOT_INTERESTED_TRACKS = "not_interested_tracks_set"
        private const val KEY_NOT_INTERESTED_ARTISTS = "not_interested_artists_set"
        private const val KEY_SEARCH_HISTORY = "search_history_json"
        private const val KEY_LAST_PLAYED_TRACK = "last_played_track_json"
        private const val KEY_LAST_PLAYED_POS = "last_played_position_ms"
        private const val KEY_LAST_PLAYED_QUEUE = "last_played_queue_json"
        private const val KEY_SKIPS = "listening_skips_json"
        private const val KEY_LYRICS_OFFSETS = "lyrics_offsets_json"
        private const val MAX_SKIPS = 200
    }

    // ==================== Search History ====================

    fun getSearchHistory(): List<String> {
        return try {
            val json = prefs.getString(KEY_SEARCH_HISTORY, "[]") ?: "[]"
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val q = arr.getString(i).trim()
                if (q.isNotBlank()) list.add(q)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val current = getSearchHistory().toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        val trimmed = current.take(20)
        val arr = JSONArray(trimmed)
        prefs.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = getSearchHistory().toMutableList()
        current.removeAll { it.equals(query.trim(), ignoreCase = true) }
        val arr = JSONArray(current)
        prefs.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    // ==================== Last Played Track ====================

    fun saveLastPlayedTrack(track: Track, positionMs: Long = 0L) {
        saveLastPlaybackState(track, positionMs, emptyList())
    }

    fun saveLastPlaybackState(track: Track, positionMs: Long = 0L, queue: List<Track> = emptyList()) {
        try {
            val obj = trackToJson(track).apply {
                put("lastPositionMs", positionMs)
                put("savedAt", System.currentTimeMillis())
            }
            val editor = prefs.edit()
                .putString(KEY_LAST_PLAYED_TRACK, obj.toString())
                .putLong(KEY_LAST_PLAYED_POS, positionMs)

            if (queue.isNotEmpty()) {
                val arr = JSONArray()
                queue.take(50).forEach { t -> arr.put(trackToJson(t)) }
                editor.putString(KEY_LAST_PLAYED_QUEUE, arr.toString())
            }
            editor.apply()
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error saving last played track: ${e.message}")
        }
    }

    fun getLastPlaybackQueue(): List<Track> {
        return try {
            val json = prefs.getString(KEY_LAST_PLAYED_QUEUE, null) ?: return emptyList()
            val arr = JSONArray(json)
            val list = mutableListOf<Track>()
            for (i in 0 until arr.length()) {
                list.add(jsonToTrack(arr.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getLastPlayedTrack(): Pair<Track, Long>? {
        return try {
            val json = prefs.getString(KEY_LAST_PLAYED_TRACK, null) ?: return null
            val obj = JSONObject(json)
            val track = jsonToTrack(obj)
            val pos = prefs.getLong(KEY_LAST_PLAYED_POS, obj.optLong("lastPositionMs", 0L))
            Pair(track, pos)
        } catch (_: Exception) {
            null
        }
    }

    fun trackToJson(track: Track): JSONObject {
        val verifiedAvatar = track.artist.avatarUrl?.takeIf { it.isNotBlank() }
            ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(track.artist.name)
            ?: getCachedArtistAvatar(track.artist.name) ?: ""

        return JSONObject().apply {
            put("id", track.id)
            put("title", track.title)
            put("artistName", track.artist.name)
            put("artistAvatar", verifiedAvatar)
            put("albumTitle", track.albumTitle ?: "")
            put("durationSec", track.durationSec)
            put("audioUrl", track.audioUrl)
            put("coverUrl", track.coverUrl)
            put("isVerified", track.isVerified)
            put("sourceLabel", track.sourceLabel)
            put("globalPlayCount", track.playCount ?: "")
        }
    }

    fun getNotInterestedTrackIds(): Set<String> {
        return prefs.getStringSet(KEY_NOT_INTERESTED_TRACKS, emptySet()) ?: emptySet()
    }

    fun getNotInterestedArtists(): Set<String> {
        return prefs.getStringSet(KEY_NOT_INTERESTED_ARTISTS, emptySet()) ?: emptySet()
    }

    fun markTrackNotInterested(trackId: String, artistName: String) {
        val currentTracks = getNotInterestedTrackIds().toMutableSet()
        val currentArtists = getNotInterestedArtists().toMutableSet()
        if (trackId.isNotBlank()) currentTracks.add(trackId)
        if (artistName.isNotBlank()) currentArtists.add(artistName.trim().lowercase())
        prefs.edit()
            .putStringSet(KEY_NOT_INTERESTED_TRACKS, currentTracks)
            .putStringSet(KEY_NOT_INTERESTED_ARTISTS, currentArtists)
            .apply()
    }

    fun isTrackNotInterested(trackId: String, artistName: String): Boolean {
        val trackDisliked = getNotInterestedTrackIds().contains(trackId)
        val artistDisliked = getNotInterestedArtists().contains(artistName.trim().lowercase())
        return trackDisliked || artistDisliked
    }

    // ==================== Per-Track Lyrics Offset (z.reference/db SongEntity.lyricsOffset) ====================

    fun getLyricsOffset(trackId: String): Long {
        if (trackId.isBlank()) return 0L
        return try {
            val json = prefs.getString(KEY_LYRICS_OFFSETS, "{}") ?: "{}"
            val obj = JSONObject(json)
            obj.optLong(trackId, 0L)
        } catch (_: Exception) {
            0L
        }
    }

    fun saveLyricsOffset(trackId: String, offsetMs: Long) {
        if (trackId.isBlank()) return
        try {
            val json = prefs.getString(KEY_LYRICS_OFFSETS, "{}") ?: "{}"
            val obj = JSONObject(json)
            if (offsetMs == 0L) {
                obj.remove(trackId)
            } else {
                obj.put(trackId, offsetMs)
            }
            prefs.edit().putString(KEY_LYRICS_OFFSETS, obj.toString()).apply()
        } catch (_: Exception) {}
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getOnboardingArtists(): List<String> {
        val str = prefs.getString(KEY_ONBOARDING_ARTISTS, "") ?: ""
        if (str.isBlank()) return emptyList()
        return str.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun saveOnboardingArtists(artists: List<String>) {
        prefs.edit().putString(KEY_ONBOARDING_ARTISTS, artists.distinct().joinToString(",")).apply()
    }

    fun getListeningStats(): ListeningStats {
        try {
            val currentMap = loadTracksMap()
            var totalPlays = 0
            var totalSec = 0L
            val artistCounts = mutableMapOf<String, Int>()

            for (entry in currentMap.values) {
                val count = entry.optInt("playCount", 1)
                val dur = entry.optInt("durationSec", 200)
                totalPlays += count
                totalSec += (dur.toLong() * count)
                val artist = entry.optString("artistName", "")
                if (artist.isNotBlank()) {
                    artistCounts[artist] = (artistCounts[artist] ?: 0) + count
                }
            }

            val topArtists = artistCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key to it.value }

            val topTracks = currentMap.values
                .sortedByDescending { it.optInt("playCount", 0) }
                .take(5)
                .map { jsonToTrack(it) }

            return ListeningStats(
                totalPlayCount = totalPlays,
                estimatedMinutes = totalSec / 60,
                topArtists = topArtists,
                topTracks = topTracks,
                favoriteArtist = topArtists.firstOrNull()?.first
            )
        } catch (_: Exception) {
            return ListeningStats()
        }
    }

    fun saveImportedPlaylist(playlist: Playlist) {
        val all = getAllPlaylists().toMutableList()
        all.removeAll { it.id == playlist.id }
        all.add(0, playlist)
        savePlaylists(all)
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

            val verifiedAvatar = track.artist.avatarUrl?.takeIf { it.isNotBlank() }
                ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(track.artist.name)
                ?: getCachedArtistAvatar(track.artist.name) ?: ""

            val entry = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artistName", track.artist.name)
                put("artistAvatar", verifiedAvatar)
                put("albumTitle", track.albumTitle ?: "")
                put("durationSec", track.durationSec)
                put("audioUrl", track.audioUrl)
                put("coverUrl", track.coverUrl)
                put("isVerified", track.isVerified)
                put("sourceLabel", track.sourceLabel)
                put("globalPlayCount", track.playCount ?: "")
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
            val playedArtists = artistCounts.entries
                .sortedByDescending { it.value }
                .map { it.key }
            val onboarded = getOnboardingArtists()
            (playedArtists + onboarded).distinct().take(limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Musicfy algorithm: Records an early-skipped track (e.g. skipped in first 30 seconds).
     * Skips are stored persistently (max 200) to keep avoided songs out of recommendations
     * for 14 days, and artists skipped 3+ times completely suppressed.
     */
    fun recordTrackSkipped(track: Track) {
        try {
            val currentJson = prefs.getString(KEY_SKIPS, "[]") ?: "[]"
            val arr = JSONArray(currentJson)
            val skipObj = JSONObject().apply {
                put("songId", track.id)
                put("artist", track.artist.name)
                put("at", System.currentTimeMillis())
            }
            val list = mutableListOf<JSONObject>()
            for (i in 0 until arr.length()) {
                list.add(arr.getJSONObject(i))
            }
            list.add(skipObj)
            while (list.size > MAX_SKIPS) {
                list.removeAt(0)
            }
            val newArr = JSONArray()
            list.forEach { newArr.put(it) }
            prefs.edit().putString(KEY_SKIPS, newArr.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error recording track skip: ${e.message}")
        }
    }

    /**
     * Retrieves recent skips within [days] days as pairs of (songId, artistName).
     */
    fun getRecentSkips(days: Int = 14): List<Pair<String, String>> {
        return try {
            val currentJson = prefs.getString(KEY_SKIPS, "[]") ?: "[]"
            val arr = JSONArray(currentJson)
            val cutoff = System.currentTimeMillis() - (days * 86_400_000L)
            val result = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val at = obj.optLong("at", 0L)
                if (at >= cutoff) {
                    val sId = obj.optString("songId", "")
                    val aName = obj.optString("artist", "")
                    if (sId.isNotBlank()) {
                        result.add(Pair(sId, aName))
                    }
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Musicfy Taste Profiler:
     * 1. Mathematical Time-Decay Scoring: score = exp(-ageDays / 14.0) * playCount
     * 2. Liked Tracks Affinity: +1.5 boost per liked track
     * 3. Negative Taste Suppression: avoidSongs (<14 days skipped), avoidArtists (>=3 skips)
     * 4. Tri-directional seed selection: Top candidates sampled via Efraimidis-Spirakis weighted sampling,
     *    strictly 1 seed per artist to maximize diversity.
     */
    fun getTasteProfile(): TasteProfile {
        return try {
            val now = System.currentTimeMillis()
            val currentMap = loadTracksMap()
            val liked = getLikedTracks()
            val recentSkips = getRecentSkips(14)
            val notInterestedTracks = getNotInterestedTrackIds()
            val notInterestedArtists = getNotInterestedArtists()

            // 1. Skips & Avoidance
            val avoidSongs = (recentSkips.map { it.first } + notInterestedTracks).toSet()
            val avoidArtistsFromSkips = recentSkips.map { it.second.trim().lowercase() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .filterValues { it >= 3 }
                .keys
            val avoidArtists = (avoidArtistsFromSkips + notInterestedArtists).toSet()

            // 2. Mathematical Time-Decay scoring
            val scores = HashMap<String, Double>()
            val songs = HashMap<String, Track>()
            val heard = mutableSetOf<String>()

            for (entry in currentMap.values) {
                val track = jsonToTrack(entry)
                val lastPlayed = entry.optLong("lastPlayed", 0L)
                val playCount = entry.optInt("playCount", 1).coerceAtLeast(1)
                val ageDays = (now - lastPlayed) / 86_400_000.0

                if (ageDays <= 90.0) {
                    heard.add(track.id)
                    val decayWeight = kotlin.math.exp(-ageDays / 14.0)
                    val totalTrackWeight = decayWeight * kotlin.math.min(playCount, 5)
                    scores[track.id] = (scores[track.id] ?: 0.0) + totalTrackWeight
                    songs[track.id] = track
                }
            }

            // Liked songs have strong signal (+1.5 boost)
            liked.take(50).forEach { track ->
                heard.add(track.id)
                scores[track.id] = (scores[track.id] ?: 0.0) + 1.5
                songs.putIfAbsent(track.id, track)
            }

            // 3. Tri-Directional Candidates (Filtered against skips & negative taste)
            val validCandidates = scores.entries
                .sortedByDescending { it.value }
                .mapNotNull { (id, score) -> songs[id]?.let { it to score } }
                .filter { (track, _) ->
                    track.id !in avoidSongs &&
                    track.artist.name.trim().lowercase() !in avoidArtists
                }
                .distinctBy { (track, _) -> track.artist.name.trim().lowercase() }
                .take(14)

            // Efraimidis-Spirakis weighted sampling (Musicfy formula)
            // key = ln(Random(1e-9..1.0)) / score
            val songSeeds = validCandidates
                .map { (track, score) ->
                    val r = kotlin.random.Random.nextDouble(1e-9, 1.0)
                    val key = kotlin.math.ln(r) / score.coerceAtLeast(1e-6)
                    track to key
                }
                .sortedByDescending { it.second }
                .take(6)
                .map { it.first }

            // Top Artists for "More Like <Artist>" shelves
            val topArtists = getFavoriteArtists(10)
                .filter { it.trim().lowercase() !in avoidArtists }

            TasteProfile(
                heard = heard,
                avoidSongs = avoidSongs,
                avoidArtists = avoidArtists,
                songSeeds = songSeeds,
                topArtists = topArtists
            )
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error building taste profile: ${e.message}")
            TasteProfile()
        }
    }

    /**
     * Returns the list of popular artists dynamically sorted based on the user's actual listening history.
     * Artists that the user listens to most frequently are placed at the beginning of the list.
     * Artists selected during onboarding receive a bonus weight.
     */
    fun getSortedPopularArtists(baseCuratedArtists: List<Artist>): List<Artist> {
        return try {
            val currentMap = loadTracksMap()
            val artistPlayCounts = mutableMapOf<String, Int>()
            val artistAvatars = mutableMapOf<String, String>()

            for (entry in currentMap.values) {
                val artist = entry.optString("artistName", "").trim()
                if (artist.isNotBlank()) {
                    val count = entry.optInt("playCount", 1)
                    val key = artist.lowercase()
                    artistPlayCounts[key] = (artistPlayCounts[key] ?: 0) + count
                    val avatar = entry.optString("artistAvatar", "").takeIf { it.isNotBlank() }
                    if (avatar != null && !artistAvatars.containsKey(key)) {
                        artistAvatars[key] = avatar
                    }
                }
            }

            val onboarded = getOnboardingArtists().map { it.trim().lowercase() }.toSet()
            val notInterested = getNotInterestedArtists().map { it.trim().lowercase() }.toSet()

            // Merge curated artists and user-discovered artists
            val allArtistMap = linkedMapOf<String, Artist>()
            for (ca in baseCuratedArtists) {
                val key = ca.name.trim().lowercase()
                allArtistMap[key] = ca
            }

            // Also add any artists user has played even if not in curated list
            for (entry in currentMap.values) {
                val name = entry.optString("artistName", "").trim()
                val key = name.lowercase()
                if (name.isNotBlank() && !allArtistMap.containsKey(key)) {
                    val avatar = entry.optString("artistAvatar", "").takeIf { it.isNotBlank() }
                        ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(name)
                        ?: getCachedArtistAvatar(name)
                    allArtistMap[key] = Artist(
                        id = "art_${key.hashCode()}",
                        name = name,
                        avatarUrl = avatar,
                        isVerified = true
                    )
                }
            }

            // Calculate score for each artist
            val scoredArtists = allArtistMap.values
                .filter { !notInterested.contains(it.name.trim().lowercase()) }
                .map { artist ->
                    val key = artist.name.trim().lowercase()
                    val actualPlays = artistPlayCounts[key] ?: 0
                    val onboardedBonus = if (onboarded.contains(key)) 5 else 0
                    val totalScore = actualPlays * 10 + onboardedBonus
                    val rawAvatar = artist.avatarUrl ?: artistAvatars[key] ?: getCachedArtistAvatar(artist.name)
                    val finalAvatar = rawAvatar?.takeIf { it.isNotBlank() }
                        ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(artist.name)
                    val updatedArtist = if (finalAvatar != artist.avatarUrl) artist.copy(avatarUrl = finalAvatar) else artist
                    Pair(updatedArtist, totalScore)
                }

            // Sort by score descending. For tie-breaks, maintain original curated order
            scoredArtists.sortedWith(
                compareByDescending<Pair<Artist, Int>> { it.second }
            ).map { it.first }
        } catch (_: Exception) {
            baseCuratedArtists
        }
    }

    /**
     * Persistently stores and updates an artist's high-resolution avatar URL.
     */
    fun updateArtistAvatar(artistName: String, avatarUrl: String) {
        try {
            if (avatarUrl.isBlank()) return
            val cleanName = artistName.trim().lowercase()

            // Update in persistent cached avatars map
            val avatarJson = prefs.getString("KEY_CACHED_ARTIST_AVATARS", "{}") ?: "{}"
            val obj = JSONObject(avatarJson)
            obj.put(cleanName, avatarUrl)
            prefs.edit().putString("KEY_CACHED_ARTIST_AVATARS", obj.toString()).apply()

            // Update in tracks map as well
            val currentMap = loadTracksMap()
            var changed = false
            for (entry in currentMap.values) {
                val name = entry.optString("artistName", "").trim().lowercase()
                if (name == cleanName) {
                    entry.put("artistAvatar", avatarUrl)
                    changed = true
                }
            }
            if (changed) {
                saveTracksMap(currentMap)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error updating artist avatar: ${e.message}", e)
        }
    }

    /**
     * Retrieves the persistent cached avatar URL for an artist if available.
     */
    fun getCachedArtistAvatar(artistName: String): String? {
        return try {
            val avatarJson = prefs.getString("KEY_CACHED_ARTIST_AVATARS", "{}") ?: "{}"
            val obj = JSONObject(avatarJson)
            obj.optString(artistName.trim().lowercase(), "").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
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
        val playCount = obj.optString("globalPlayCount", "").takeIf { it.isNotBlank() }
            ?: (obj.opt("playCount") as? String)?.takeIf { it.isNotBlank() }

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
            playCount = playCount,
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
                        put("globalPlayCount", t.playCount ?: "")
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
                put("globalPlayCount", t.playCount ?: "")
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

