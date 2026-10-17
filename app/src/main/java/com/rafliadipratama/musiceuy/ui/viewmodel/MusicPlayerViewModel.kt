package com.rafliadipratama.musiceuy.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.rafliadipratama.musiceuy.data.model.Lyrics
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.data.network.LrcLibClient
import com.rafliadipratama.musiceuy.data.network.MusicStreamSource
import com.rafliadipratama.musiceuy.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.rafliadipratama.musiceuy.data.local.LocalMusicHistoryManager
import com.rafliadipratama.musiceuy.data.model.Album
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Playlist

enum class RepeatMode { OFF, ALL, ONE }

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val isShuffle: Boolean = false,
    val queue: List<Track> = emptyList(),
    val lyrics: Lyrics = Lyrics(),
    val isPlayerExpanded: Boolean = false,
    val isLyricsVisible: Boolean = false,
    val isQueueVisible: Boolean = false,
    val searchResults: List<Track> = emptyList(),
    val trendingTracks: List<Track> = emptyList(),
    val mostPlayedTracks: List<Track> = emptyList(),
    val favoriteArtists: List<String> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val selectedArtist: Artist? = null,
    val artistTracks: List<Track> = emptyList(),
    val artistAlbums: List<Album> = emptyList(),
    val isLoadingArtist: Boolean = false,
    val isLoadingAlbums: Boolean = false,
    val isAddToPlaylistSheetVisible: Boolean = false,
    val trackToAddToPlaylist: Track? = null,
    val trackForContextMenu: Track? = null,
    val likedTracks: List<Track> = emptyList(),
    val likedTrackIds: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val error: String? = null
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    val streamSource = MusicStreamSource()
    val pluginManager = com.rafliadipratama.musiceuy.plugin.PluginManager(application, streamSource)
    val historyManager = LocalMusicHistoryManager(application)
    private val lrcClient = LrcLibClient()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var progressJob: Job? = null

    init {
        initMediaController()
        loadInitialData()
    }

    private fun initMediaController() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
            } catch (_: Exception) {
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    progressJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = mediaController?.duration ?: 0L
                    _uiState.update { it.copy(durationMs = if (dur > 0) dur else 0L) }
                } else if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val pos = mediaController?.currentPosition ?: 0L
                val dur = mediaController?.duration ?: 0L
                _uiState.update {
                    it.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else it.durationMs
                    )
                }
                delay(300)
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            val results = withContext(Dispatchers.IO) {
                pluginManager.search(query).ifEmpty { streamSource.searchTracks(query) }
            }
            _uiState.update {
                it.copy(
                    searchResults = results,
                    isSearching = false,
                    error = if (results.isEmpty()) "Lagu tidak ditemukan. Coba kata kunci lain." else null
                )
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val mostPlayed = withContext(Dispatchers.IO) {
                historyManager.getMostPlayedTracks()
            }
            val favArtists = withContext(Dispatchers.IO) {
                historyManager.getFavoriteArtists()
            }
            val playlists = withContext(Dispatchers.IO) {
                historyManager.getAllPlaylists()
            }
            val liked = withContext(Dispatchers.IO) {
                historyManager.getLikedTracks()
            }
            val likedIds = liked.map { it.id }.toSet()
            _uiState.update {
                it.copy(
                    mostPlayedTracks = mostPlayed,
                    favoriteArtists = favArtists,
                    playlists = playlists,
                    likedTracks = liked,
                    likedTrackIds = likedIds
                )
            }

            // If user has listening history, prioritize their favorite / most played songs
            // Otherwise, fetch curated trending Indonesian tracks so startup is never random
            if (mostPlayed.isNotEmpty()) {
                _uiState.update { it.copy(trendingTracks = mostPlayed) }
            } else {
                val trending = withContext(Dispatchers.IO) { streamSource.fetchTrending() }
                _uiState.update { it.copy(trendingTracks = trending) }
            }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(searchResults = emptyList(), error = null) }
            val trending = withContext(Dispatchers.IO) { streamSource.fetchTrending() }
            _uiState.update { it.copy(trendingTracks = trending) }
        }
    }

    fun showMostPlayed() {
        viewModelScope.launch {
            val mostPlayed = withContext(Dispatchers.IO) {
                historyManager.getMostPlayedTracks()
            }
            _uiState.update {
                it.copy(
                    searchResults = mostPlayed,
                    isSearching = false,
                    error = if (mostPlayed.isEmpty()) "Belum ada lagu yang sering diputar. Mulai dengarkan lagu favoritmu!" else null
                )
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        val controller = mediaController ?: return
        val currentQueue = newQueue ?: _uiState.value.queue.ifEmpty { listOf(track) }
        val isLiked = _uiState.value.likedTrackIds.contains(track.id)
        val activeTrack = track.copy(isLiked = isLiked)

        _uiState.update {
            it.copy(
                currentTrack = activeTrack,
                queue = currentQueue,
                currentPositionMs = 0L,
                lyrics = Lyrics(),
                error = null
            )
        }

        // Record locally for offline recommendation & history
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.recordTrackPlayed(track)
            val updatedMostPlayed = historyManager.getMostPlayedTracks()
            val updatedFavArtists = historyManager.getFavoriteArtists()
            _uiState.update {
                it.copy(
                    mostPlayedTracks = updatedMostPlayed,
                    favoriteArtists = updatedFavArtists
                )
            }
        }

        // Fetch synchronized lyrics in background
        viewModelScope.launch {
            val lyrics = lrcClient.fetchLyrics(track.artist.name, track.title)
            _uiState.update { it.copy(lyrics = lyrics) }
        }

        viewModelScope.launch {
            var activeAudioUri = track.audioUrl

            android.util.Log.d("MusicEuy", "playTrack: title=${track.title}, artist=${track.artist.name}, initialUrl=${track.audioUrl}")

            // If audioUrl is a placeholder (e.g. yt:ID), resolve via active plugins
            if (!activeAudioUri.startsWith("http")) {
                val resolvedStream = withContext(Dispatchers.IO) {
                    pluginManager.resolveStream(track)
                }
                if (!resolvedStream.isNullOrBlank() && resolvedStream.startsWith("http")) {
                    activeAudioUri = resolvedStream
                    android.util.Log.d("MusicEuy", "Resolved via PluginManager: $activeAudioUri")
                } else {
                    android.util.Log.w("MusicEuy", "PluginManager resolution failed, trying fallback streamSource for ${track.title}")
                    val fallbackStream = withContext(Dispatchers.IO) {
                        streamSource.resolveYouTubeAudioStream(track.id.removePrefix("yt:"))
                    }
                    if (!fallbackStream.isNullOrBlank()) {
                        activeAudioUri = fallbackStream
                    }
                }
            }

            if (!activeAudioUri.startsWith("http")) {
                android.util.Log.e("MusicEuy", "Cannot play track: activeAudioUri is invalid ($activeAudioUri)")
                _uiState.update { it.copy(isPlaying = false, error = "Stream audio belum tersedia untuk '${track.title}'. Coba lagu lain.") }
                return@launch
            }

            android.util.Log.d("MusicEuy", "Setting MediaItem URI: $activeAudioUri")

            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist.name)
                .setAlbumTitle(track.albumTitle ?: "")
                .setArtworkUri(Uri.parse(track.coverUrl))
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(activeAudioUri)
                .setMediaMetadata(metadata)
                .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun nextTrack() {
        val state = _uiState.value
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentTrack?.id }
        if (currentIndex != -1 && currentIndex < state.queue.size - 1) {
            playTrack(state.queue[currentIndex + 1])
        } else if (state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty()) {
            playTrack(state.queue.first())
        }
    }

    fun previousTrack() {
        val state = _uiState.value
        if (state.currentPositionMs > 3000) {
            seekTo(0)
            return
        }
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentTrack?.id }
        if (currentIndex > 0) {
            playTrack(state.queue[currentIndex - 1])
        } else if (state.queue.isNotEmpty()) {
            playTrack(state.queue.last())
        }
    }

    private fun handleTrackEnded() {
        val state = _uiState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaController?.play()
            }
            RepeatMode.ALL -> nextTrack()
            RepeatMode.OFF -> {
                val currentIndex = state.queue.indexOfFirst { it.id == state.currentTrack?.id }
                if (currentIndex < state.queue.size - 1) {
                    nextTrack()
                } else {
                    mediaController?.pause()
                    seekTo(0)
                }
            }
        }
    }

    fun toggleRepeatMode() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun expandPlayer() {
        _uiState.update { it.copy(isPlayerExpanded = true) }
    }

    fun collapsePlayer() {
        _uiState.update { it.copy(isPlayerExpanded = false) }
    }

    fun toggleLyrics() {
        _uiState.update { it.copy(isLyricsVisible = !it.isLyricsVisible) }
    }

    fun toggleQueue() {
        _uiState.update { it.copy(isQueueVisible = !it.isQueueVisible) }
    }

    fun dismissQueue() {
        _uiState.update { it.copy(isQueueVisible = false) }
    }

    fun playFromQueue(track: Track) {
        playTrack(track, _uiState.value.queue)
    }

    fun removeFromQueue(trackId: String) {
        _uiState.update { state ->
            val updated = state.queue.filterNot { it.id == trackId }
            state.copy(queue = updated)
        }
    }

    fun clearQueue() {
        _uiState.update { state ->
            val current = state.currentTrack
            state.copy(queue = if (current != null) listOf(current) else emptyList())
        }
    }

    // ==================== Playlist & Artist Management ====================

    fun loadPlaylists() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { historyManager.getAllPlaylists() }
            _uiState.update { it.copy(playlists = list) }
        }
    }

    fun createPlaylist(name: String, description: String = "", initialTrack: Track? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val p = historyManager.createPlaylist(name, description)
                if (initialTrack != null) {
                    historyManager.addTrackToPlaylist(p.id, initialTrack)
                }
            }
            loadPlaylists()
            if (initialTrack != null) {
                _uiState.update { it.copy(isAddToPlaylistSheetVisible = false, trackToAddToPlaylist = null) }
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyManager.addTrackToPlaylist(playlistId, track)
            }
            loadPlaylists()
            if (_uiState.value.selectedPlaylist?.id == playlistId) {
                val updated = withContext(Dispatchers.IO) {
                    historyManager.getAllPlaylists().find { it.id == playlistId }
                }
                _uiState.update { it.copy(selectedPlaylist = updated) }
            }
            _uiState.update { it.copy(isAddToPlaylistSheetVisible = false, trackToAddToPlaylist = null) }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyManager.removeTrackFromPlaylist(playlistId, trackId)
            }
            loadPlaylists()
            if (_uiState.value.selectedPlaylist?.id == playlistId) {
                val updated = withContext(Dispatchers.IO) {
                    historyManager.getAllPlaylists().find { it.id == playlistId }
                }
                _uiState.update { it.copy(selectedPlaylist = updated) }
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyManager.deletePlaylist(playlistId)
            }
            loadPlaylists()
            if (_uiState.value.selectedPlaylist?.id == playlistId) {
                _uiState.update { it.copy(selectedPlaylist = null) }
            }
        }
    }

    fun openPlaylist(playlist: Playlist) {
        _uiState.update { it.copy(selectedPlaylist = playlist, isPlayerExpanded = false) }
    }

    fun closePlaylist() {
        _uiState.update { it.copy(selectedPlaylist = null) }
    }

    fun showAddToPlaylist(track: Track) {
        _uiState.update { it.copy(isAddToPlaylistSheetVisible = true, trackToAddToPlaylist = track) }
    }

    fun dismissAddToPlaylist() {
        _uiState.update { it.copy(isAddToPlaylistSheetVisible = false, trackToAddToPlaylist = null) }
    }

    // ==================== Liked Tracks & Context Menu ====================

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            val isNowLiked = withContext(Dispatchers.IO) {
                historyManager.toggleLikedTrack(track)
            }
            val updatedLiked = withContext(Dispatchers.IO) {
                historyManager.getLikedTracks()
            }
            _uiState.update { state ->
                val updatedIds = updatedLiked.map { it.id }.toSet()
                val updatedCurrent = if (state.currentTrack?.id == track.id) {
                    state.currentTrack.copy(isLiked = isNowLiked)
                } else state.currentTrack
                val updatedSelectedPlaylist = if (state.selectedPlaylist?.id == "liked_tracks_virtual") {
                    state.selectedPlaylist.copy(
                        description = "Koleksi ${updatedLiked.size} lagu favorit yang kamu sukai",
                        coverUrl = updatedLiked.firstOrNull()?.coverUrl ?: "",
                        tracks = updatedLiked
                    )
                } else state.selectedPlaylist
                state.copy(
                    likedTracks = updatedLiked,
                    likedTrackIds = updatedIds,
                    currentTrack = updatedCurrent,
                    selectedPlaylist = updatedSelectedPlaylist
                )
            }
        }
    }

    fun openLikedTracks() {
        val liked = _uiState.value.likedTracks
        _uiState.update {
            it.copy(
                isPlayerExpanded = false,
                selectedPlaylist = Playlist(
                    id = "liked_tracks_virtual",
                    name = "Lagu Disukai",
                    description = "Koleksi ${liked.size} lagu favorit yang kamu sukai",
                    coverUrl = liked.firstOrNull()?.coverUrl ?: "",
                    tracks = liked
                )
            )
        }
    }

    fun showTrackContextMenu(track: Track) {
        _uiState.update { it.copy(trackForContextMenu = track) }
    }

    fun dismissTrackContextMenu() {
        _uiState.update { it.copy(trackForContextMenu = null) }
    }

    fun playNext(track: Track) {
        _uiState.update { state ->
            val currentQueue = state.queue.toMutableList()
            val currentIndex = currentQueue.indexOfFirst { it.id == state.currentTrack?.id }
            currentQueue.removeAll { it.id == track.id }
            val insertPos = if (currentIndex >= 0 && currentIndex < currentQueue.size) currentIndex + 1 else currentQueue.size
            currentQueue.add(insertPos, track)
            state.copy(queue = currentQueue, trackForContextMenu = null)
        }
    }

    fun addToQueue(track: Track) {
        _uiState.update { state ->
            val currentQueue = state.queue.toMutableList()
            if (currentQueue.none { it.id == track.id }) {
                currentQueue.add(track)
            }
            state.copy(queue = currentQueue, trackForContextMenu = null)
        }
    }

    fun openArtistDetail(artist: Artist) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedArtist = artist,
                    artistTracks = emptyList(),
                    artistAlbums = emptyList(),
                    isLoadingArtist = true,
                    isLoadingAlbums = true,
                    isPlayerExpanded = false
                )
            }
            launch {
                val results = withContext(Dispatchers.IO) {
                    pluginManager.search(artist.name).ifEmpty { streamSource.searchTracks(artist.name) }
                }
                _uiState.update {
                    it.copy(
                        artistTracks = results,
                        isLoadingArtist = false
                    )
                }
            }
            launch {
                val albums = withContext(Dispatchers.IO) {
                    streamSource.getArtistAlbums(artist.name)
                }
                _uiState.update {
                    it.copy(
                        artistAlbums = albums,
                        isLoadingAlbums = false
                    )
                }
            }
        }
    }

    fun openAlbum(album: Album) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPlayerExpanded = false,
                    selectedPlaylist = Playlist(
                        id = album.id,
                        name = album.title,
                        description = "Album • ${album.year.ifBlank { "YouTube Music" }} • ${album.artistName}",
                        coverUrl = album.coverUrl,
                        tracks = emptyList()
                    )
                )
            }
            val tracks = withContext(Dispatchers.IO) {
                streamSource.getAlbumTracks(album.id, album.artistName)
            }
            _uiState.update { state ->
                if (state.selectedPlaylist?.id == album.id) {
                    state.copy(
                        selectedPlaylist = state.selectedPlaylist.copy(tracks = tracks)
                    )
                } else state
            }
        }
    }

    fun closeArtistDetail() {
        _uiState.update { it.copy(selectedArtist = null, artistTracks = emptyList(), artistAlbums = emptyList()) }
    }

    override fun onCleared() {
        progressJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
