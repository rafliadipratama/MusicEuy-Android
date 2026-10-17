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
    val searchResultArtists: List<Artist> = emptyList(),
    val searchResultAlbums: List<Album> = emptyList(),
    val searchCategory: String = "Semua",
    val trendingTracks: List<Track> = emptyList(),
    val mostPlayedTracks: List<Track> = emptyList(),
    val newReleases: List<Album> = emptyList(),
    val popularArtists: List<Artist> = emptyList(),
    val selectedHomeFilter: String = "Semua",
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
    val isLoadingLyrics: Boolean = false,
    val sleepTimerRemainingSeconds: Long? = null,
    val isSleepTimerEndOfTrack: Boolean = false,
    val isSleepTimerDialogVisible: Boolean = false,
    val downloadingTrackIds: Map<String, Float> = emptyMap(),
    val downloadedTrackIds: Set<String> = emptySet(),
    val downloadedTracks: List<Track> = emptyList(),
    val isDownloadedScreenVisible: Boolean = false,
    val downloadNotificationMessage: String? = null,
    val dominantColor: Long? = null,
    val isAutoplayEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val isEqualizerSheetVisible: Boolean = false,
    val isBackupRestoreDialogVisible: Boolean = false,
    val backupRestoreMessage: String? = null,
    val error: String? = null
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    val streamSource = MusicStreamSource()
    val pluginManager = com.rafliadipratama.musiceuy.plugin.PluginManager(application, streamSource)
    val historyManager = LocalMusicHistoryManager(application)
    val downloadManager = com.rafliadipratama.musiceuy.data.local.MusicDownloadManager(application)
    private val lrcClient = LrcLibClient()
    private var sleepTimerJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var progressJob: Job? = null

    init {
        com.rafliadipratama.musiceuy.audio.AudioEffectManager.initFromPrefs(application)
        com.rafliadipratama.musiceuy.widget.MusicWidgetProvider.playbackActionHandler = { action ->
            when (action) {
                com.rafliadipratama.musiceuy.widget.MusicWidgetProvider.ACTION_PLAY_PAUSE -> togglePlayPause()
                com.rafliadipratama.musiceuy.widget.MusicWidgetProvider.ACTION_NEXT -> nextTrack()
                com.rafliadipratama.musiceuy.widget.MusicWidgetProvider.ACTION_PREV -> previousTrack()
            }
        }
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
                com.rafliadipratama.musiceuy.widget.MusicWidgetProvider.updateWidget(
                    getApplication(),
                    _uiState.value.currentTrack,
                    isPlaying
                )
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

    fun setSearchCategory(category: String) {
        _uiState.update { it.copy(searchCategory = category) }
    }

    fun selectHomeFilter(filter: String) {
        _uiState.update { it.copy(selectedHomeFilter = filter) }
        when (filter) {
            "Semua", "Trending", "Rilisan Baru", "Artis Populer" -> {
                _uiState.update { it.copy(searchResults = emptyList(), searchResultArtists = emptyList(), searchResultAlbums = emptyList()) }
            }
            "Pop Indonesia" -> search("Pop Indonesia Hits")
            "Artis Terverifikasi" -> search("Sheila On 7 Bernadya Hindia Tulus")
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            val allResults = withContext(Dispatchers.IO) {
                val pluginTracks = pluginManager.search(query)
                if (pluginTracks.isNotEmpty()) {
                    val artists = streamSource.searchArtists(query)
                    val albums = streamSource.searchAlbums(query)
                    com.rafliadipratama.musiceuy.data.network.SearchAllResults(
                        tracks = pluginTracks,
                        artists = artists,
                        albums = albums
                    )
                } else {
                    streamSource.searchAll(query)
                }
            }
            _uiState.update {
                it.copy(
                    searchResults = allResults.tracks,
                    searchResultArtists = allResults.artists,
                    searchResultAlbums = allResults.albums,
                    isSearching = false,
                    error = if (allResults.tracks.isEmpty() && allResults.artists.isEmpty() && allResults.albums.isEmpty())
                        "Tidak ditemukan hasil untuk '$query'. Coba kata kunci lain." else null
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

            // Muat lagu yang sudah terunduh di HP
            loadDownloadedTracks()

            // Muat data Explore YouTube Music (Trending, Rilisan Baru, Musisi Populer)
            val explore = withContext(Dispatchers.IO) {
                streamSource.fetchExploreHome()
            }
            _uiState.update {
                it.copy(
                    trendingTracks = explore.trendingTracks,
                    newReleases = explore.newReleases,
                    popularArtists = explore.popularArtists
                )
            }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(searchResults = emptyList(), searchResultArtists = emptyList(), searchResultAlbums = emptyList(), error = null) }
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
                    searchResultArtists = emptyList(),
                    searchResultAlbums = emptyList(),
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
                isLoadingLyrics = true,
                error = null
            )
        }

        // Check if track is already downloaded to local storage
        viewModelScope.launch(Dispatchers.IO) {
            if (downloadManager.isTrackDownloaded(track)) {
                _uiState.update { it.copy(downloadedTrackIds = it.downloadedTrackIds + track.id) }
            }
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
            _uiState.update { it.copy(lyrics = lyrics, isLoadingLyrics = false) }
        }

        viewModelScope.launch {
            var activeAudioUri = track.audioUrl

            android.util.Log.d("MusicEuy", "playTrack: title=${track.title}, artist=${track.artist.name}, initialUrl=${track.audioUrl}")

            val isLocalAudio = activeAudioUri.startsWith("content://") || activeAudioUri.startsWith("file://")

            // If audioUrl is a placeholder (e.g. yt:ID), resolve via active plugins
            if (!isLocalAudio && !activeAudioUri.startsWith("http")) {
                val resolvedStream = withContext(Dispatchers.IO) {
                    pluginManager.resolveStream(track)
                }
                if (!resolvedStream.isNullOrBlank() && resolvedStream.startsWith("http")) {
                    activeAudioUri = resolvedStream
                    android.util.Log.d("MusicEuy", "Resolved via PluginManager: $activeAudioUri")
                } else {
                    android.util.Log.w("MusicEuy", "PluginManager resolution failed, trying fallback streamSource for ${track.title}")
                    val quality = com.rafliadipratama.musiceuy.audio.AudioEffectManager.getAudioQuality(getApplication())
                    val fallbackStream = withContext(Dispatchers.IO) {
                        streamSource.resolveYouTubeAudioStream(track.id.removePrefix("yt:"), quality)
                    }
                    if (!fallbackStream.isNullOrBlank()) {
                        activeAudioUri = fallbackStream
                    }
                }
            }

            if (!isLocalAudio && !activeAudioUri.startsWith("http")) {
                android.util.Log.e("MusicEuy", "Cannot play track: activeAudioUri is invalid ($activeAudioUri)")
                _uiState.update { it.copy(isPlaying = false, error = "Stream audio belum tersedia untuk '${track.title}'. Coba lagu lain.") }
                return@launch
            }

            android.util.Log.d("MusicEuy", "Setting MediaItem URI: $activeAudioUri")

            // Extract dynamic palette color from cover artwork
            extractPaletteColor(track.coverUrl)

            // Update Home Screen Widget
            com.rafliadipratama.musiceuy.widget.MusicWidgetProvider.updateWidget(
                getApplication(),
                activeTrack,
                true
            )

            val artworkUri = if (track.coverUrl.isNotBlank()) {
                try { Uri.parse(track.coverUrl) } catch (_: Exception) { null }
            } else null

            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist.name)
                .setAlbumTitle(track.albumTitle ?: "")
                .setArtworkUri(artworkUri)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(activeAudioUri)
                .setMediaMetadata(metadata)
                .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()

            // Smooth crossfade transition if enabled
            if (_uiState.value.crossfadeSeconds > 0) {
                controller.volume = 0.2f
                controller.play()
                viewModelScope.launch {
                    delay(250)
                    controller.volume = 0.6f
                    delay(250)
                    controller.volume = 1.0f
                }
            } else {
                controller.volume = 1.0f
                controller.play()
            }
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

        // Check if sleep timer is set to stop at the end of track
        if (state.isSleepTimerEndOfTrack) {
            mediaController?.pause()
            seekTo(0)
            _uiState.update { it.copy(isSleepTimerEndOfTrack = false, sleepTimerRemainingSeconds = null) }
            return
        }

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
                } else if (state.isAutoplayEnabled && state.currentTrack != null) {
                    triggerAutoplay(state.currentTrack)
                } else {
                    mediaController?.pause()
                    seekTo(0)
                }
            }
        }
    }

    fun triggerAutoplay(endedTrack: Track) {
        viewModelScope.launch {
            val related = withContext(Dispatchers.IO) {
                streamSource.searchTracks(endedTrack.artist.name)
            }
            val existingIds = _uiState.value.queue.map { it.id }.toSet()
            val candidate = related.firstOrNull { !existingIds.contains(it.id) && it.id != endedTrack.id }
                ?: related.firstOrNull { it.id != endedTrack.id }

            if (candidate != null) {
                _uiState.update { it.copy(queue = it.queue + candidate) }
                playTrack(candidate)
            } else {
                mediaController?.pause()
                seekTo(0)
            }
        }
    }

    fun toggleAutoplay() {
        _uiState.update { it.copy(isAutoplayEnabled = !it.isAutoplayEnabled) }
    }

    fun setCrossfade(seconds: Int) {
        _uiState.update { it.copy(crossfadeSeconds = seconds) }
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
                val (fullArtist, albums) = withContext(Dispatchers.IO) {
                    streamSource.getArtistProfileAndAlbums(artist.name)
                }
                _uiState.update {
                    it.copy(
                        selectedArtist = fullArtist,
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

    // --- Sleep Timer ---

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalSeconds = minutes * 60L
        _uiState.update {
            it.copy(
                sleepTimerRemainingSeconds = totalSeconds,
                isSleepTimerEndOfTrack = false,
                isSleepTimerDialogVisible = false
            )
        }

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                if (remaining <= 60) {
                    val vol = (remaining / 60f).coerceIn(0f, 1f)
                    mediaController?.volume = vol
                }
                _uiState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            if (isActive) {
                mediaController?.pause()
                mediaController?.volume = 1f
                _uiState.update {
                    it.copy(
                        sleepTimerRemainingSeconds = null,
                        isSleepTimerEndOfTrack = false
                    )
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        mediaController?.volume = 1f
        _uiState.update {
            it.copy(
                sleepTimerRemainingSeconds = null,
                isSleepTimerEndOfTrack = false,
                isSleepTimerDialogVisible = false
            )
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        _uiState.update {
            it.copy(
                sleepTimerRemainingSeconds = null,
                isSleepTimerEndOfTrack = true,
                isSleepTimerDialogVisible = false
            )
        }
    }

    fun showSleepTimerDialog() {
        _uiState.update { it.copy(isSleepTimerDialogVisible = true) }
    }

    fun hideSleepTimerDialog() {
        _uiState.update { it.copy(isSleepTimerDialogVisible = false) }
    }

    // --- Music Download ---

    fun downloadTrack(track: Track) {
        if (_uiState.value.downloadingTrackIds.containsKey(track.id)) return

        viewModelScope.launch {
            val isAlreadyDownloaded = withContext(Dispatchers.IO) {
                downloadManager.isTrackDownloaded(track)
            }
            if (isAlreadyDownloaded) {
                _uiState.update {
                    it.copy(
                        downloadedTrackIds = it.downloadedTrackIds + track.id,
                        downloadNotificationMessage = "Lagu '${track.title}' sudah tersimpan di penyimpanan HP."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    downloadingTrackIds = it.downloadingTrackIds + (track.id to 0.01f),
                    downloadNotificationMessage = "Mulai mengunduh '${track.title}'..."
                )
            }

            var activeAudioUri = track.audioUrl
            if (!activeAudioUri.startsWith("http")) {
                val resolved = withContext(Dispatchers.IO) {
                    pluginManager.resolveStream(track)
                }
                if (!resolved.isNullOrBlank() && resolved.startsWith("http")) {
                    activeAudioUri = resolved
                } else {
                    val fallback = withContext(Dispatchers.IO) {
                        streamSource.resolveYouTubeAudioStream(track.id.removePrefix("yt:"))
                    }
                    if (!fallback.isNullOrBlank()) {
                        activeAudioUri = fallback
                    }
                }
            }

            if (!activeAudioUri.startsWith("http")) {
                _uiState.update {
                    it.copy(
                        downloadingTrackIds = it.downloadingTrackIds - track.id,
                        downloadNotificationMessage = "Gagal mengunduh: URL stream audio '${track.title}' tidak tersedia."
                    )
                }
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                downloadManager.downloadTrack(track, activeAudioUri) { progress ->
                    _uiState.update { state ->
                        state.copy(downloadingTrackIds = state.downloadingTrackIds + (track.id to progress))
                    }
                }
            }

            result.fold(
                onSuccess = { path ->
                    _uiState.update {
                        it.copy(
                            downloadingTrackIds = it.downloadingTrackIds - track.id,
                            downloadedTrackIds = it.downloadedTrackIds + track.id,
                            downloadNotificationMessage = "✓ Tersimpan di $path"
                        )
                    }
                    loadDownloadedTracks()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            downloadingTrackIds = it.downloadingTrackIds - track.id,
                            downloadNotificationMessage = "Gagal mengunduh '${track.title}': ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadDownloadedTracks() {
        viewModelScope.launch {
            val downloaded = withContext(Dispatchers.IO) {
                downloadManager.getDownloadedTracks()
            }
            val downloadedIds = downloaded.map { it.id }.toSet()
            _uiState.update {
                it.copy(
                    downloadedTracks = downloaded,
                    downloadedTrackIds = it.downloadedTrackIds + downloadedIds
                )
            }
        }
    }

    fun deleteDownloadedTrack(track: Track) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                downloadManager.deleteDownloadedTrack(track)
            }
            if (success) {
                loadDownloadedTracks()
                _uiState.update {
                    it.copy(
                        downloadNotificationMessage = "Lagu '${track.title}' dihapus dari unduhan",
                        downloadedTrackIds = it.downloadedTrackIds - track.id
                    )
                }
            }
        }
    }

    fun openDownloadedTracks() {
        loadDownloadedTracks()
        _uiState.update { it.copy(isDownloadedScreenVisible = true, isPlayerExpanded = false) }
    }

    fun closeDownloadedTracks() {
        _uiState.update { it.copy(isDownloadedScreenVisible = false) }
    }

    fun clearDownloadMessage() {
        _uiState.update { it.copy(downloadNotificationMessage = null) }
    }

    // ==================== Dynamic Palette Color ====================

    private fun extractPaletteColor(coverUrl: String) {
        if (coverUrl.isBlank()) {
            _uiState.update { it.copy(dominantColor = null) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loader = coil.ImageLoader(getApplication())
                val req = coil.request.ImageRequest.Builder(getApplication())
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(req) as? coil.request.SuccessResult)?.drawable
                val bmp = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val palette = androidx.palette.graphics.Palette.from(bmp).generate()
                    val swatch = palette.darkVibrantSwatch ?: palette.dominantSwatch ?: palette.vibrantSwatch
                    val colorVal = swatch?.rgb?.toLong()
                    _uiState.update { it.copy(dominantColor = colorVal) }
                }
            } catch (e: Exception) {
                android.util.Log.w("MusicEuy", "Palette extraction failed: ${e.message}")
            }
        }
    }

    // ==================== Equalizer & Audio Effects ====================

    fun showEqualizer() {
        _uiState.update { it.copy(isEqualizerSheetVisible = true) }
    }

    fun dismissEqualizer() {
        _uiState.update { it.copy(isEqualizerSheetVisible = false) }
    }

    // ==================== Share Song as Story Card ====================

    fun shareCurrentTrackStory() {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            com.rafliadipratama.musiceuy.util.StoryCardGenerator.generateAndShareStoryCard(
                getApplication(),
                track
            )
        }
    }

    // ==================== Backup & Restore ====================

    fun showBackupRestoreDialog() {
        _uiState.update { it.copy(isBackupRestoreDialogVisible = true, backupRestoreMessage = null) }
    }

    fun dismissBackupRestoreDialog() {
        _uiState.update { it.copy(isBackupRestoreDialogVisible = false, backupRestoreMessage = null) }
    }

    fun exportBackupJson(): String {
        return historyManager.exportBackupJson()
    }

    fun importBackupJson(jsonStr: String): Boolean {
        val success = historyManager.importBackupJson(jsonStr)
        if (success) {
            loadInitialData()
            _uiState.update { it.copy(backupRestoreMessage = "✓ Berhasil memulihkan playlist dan lagu disukai!") }
        } else {
            _uiState.update { it.copy(backupRestoreMessage = "Gagal memulihkan. Format JSON tidak sesuai.") }
        }
        return success
    }

    override fun onCleared() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
