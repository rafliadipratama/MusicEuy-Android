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
import kotlinx.coroutines.async
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
    val isLoadingPlaylist: Boolean = false,
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
    val isImportPlaylistDialogVisible: Boolean = false,
    val isImportingPlaylist: Boolean = false,
    val isRecapDialogVisible: Boolean = false,
    val listeningStats: com.rafliadipratama.musiceuy.data.local.ListeningStats = com.rafliadipratama.musiceuy.data.local.ListeningStats(),
    val isStorageDialogVisible: Boolean = false,
    val audioCacheSizeBytes: Long = 0L,
    val downloadedSizeBytes: Long = 0L,
    val isOnboardingCompleted: Boolean = false,
    val isCalibratingOnboarding: Boolean = false,
    val isRefreshingHome: Boolean = false,
    val notInterestedTrackIds: Set<String> = emptySet(),
    val notInterestedArtists: Set<String> = emptySet(),
    val searchHistory: List<String> = emptyList(),
    val lastPlayedTrack: Track? = null,
    val lastPlayedPositionMs: Long = 0L,
    val currentLanguageCode: String = "system",
    val isLanguageDialogVisible: Boolean = false,
    val isProfileVisible: Boolean = false,
    val userProfile: com.rafliadipratama.musiceuy.data.local.UserProfile = com.rafliadipratama.musiceuy.data.local.UserProfile(),
    val isLyricsShareDialogVisible: Boolean = false,
    val selectedLyricForShare: String = "",
    val isOnline: Boolean = true,
    val error: String? = null
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    val streamSource = MusicStreamSource()
    val pluginManager = com.rafliadipratama.musiceuy.plugin.PluginManager(application, streamSource)
    val historyManager = LocalMusicHistoryManager(application)
    val downloadManager = com.rafliadipratama.musiceuy.data.local.MusicDownloadManager(application)
    val playlistImporter = com.rafliadipratama.musiceuy.data.network.PlaylistImporter()
    val connectivityObserver = com.rafliadipratama.musiceuy.util.NetworkConnectivityObserver(application)
    val userProfileManager = com.rafliadipratama.musiceuy.data.local.UserProfileManager(application)
    private val lrcClient = LrcLibClient(cacheDir = application.filesDir)
    private var sleepTimerJob: Job? = null

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            isOnboardingCompleted = historyManager.isOnboardingCompleted()
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var progressJob: Job? = null

    init {
        com.rafliadipratama.musiceuy.audio.AudioEffectManager.initFromPrefs(application)
        val initialLang = com.rafliadipratama.musiceuy.util.LanguageHelper.getSavedLanguageCode(application)
        val initialProfile = userProfileManager.getProfile()
        val savedLastPlayed = historyManager.getLastPlayedTrack()
        val savedQueue = historyManager.getLastPlaybackQueue()
        val initialTrack = savedLastPlayed?.first
        val initialPos = savedLastPlayed?.second ?: 0L
        val initialDuration = initialTrack?.let { it.durationSec * 1000L } ?: 0L
        val initialOnline = connectivityObserver.isCurrentlyConnected()

        _uiState.update {
            it.copy(
                isOnboardingCompleted = historyManager.isOnboardingCompleted(),
                currentLanguageCode = initialLang,
                userProfile = initialProfile,
                isOnline = initialOnline,
                lastPlayedTrack = initialTrack,
                lastPlayedPositionMs = initialPos,
                currentTrack = initialTrack,
                currentPositionMs = initialPos,
                durationMs = initialDuration,
                queue = if (savedQueue.isNotEmpty()) savedQueue else if (initialTrack != null) listOf(initialTrack) else emptyList(),
                isPlaying = false
            )
        }

        viewModelScope.launch {
            connectivityObserver.observe().collect { online ->
                val wasOnline = _uiState.value.isOnline
                _uiState.update { it.copy(isOnline = online) }
                if (!wasOnline && online) {
                    loadDownloadedTracks()
                    if (_uiState.value.trendingTracks.isEmpty()) {
                        refreshHomeContent()
                    }
                }
            }
        }

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

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicEuy", "Player playback error occurred: ${error.message}", error)
                val curTrack = _uiState.value.currentTrack ?: return
                if (curTrack.audioUrl.startsWith("content://") || curTrack.audioUrl.startsWith("file://")) return

                viewModelScope.launch {
                    val q = "${curTrack.title} ${curTrack.artist.name}".trim()
                    android.util.Log.d("MusicEuy", "Auto-recovering playback with YouTube search for: $q")
                    val results = withContext(Dispatchers.IO) { streamSource.searchYouTubeMusic(q) }
                    val match = results.firstOrNull()
                    if (match != null) {
                        val quality = com.rafliadipratama.musiceuy.audio.AudioEffectManager.getAudioQuality(getApplication())
                        val recoveredStream = withContext(Dispatchers.IO) {
                            streamSource.resolveYouTubeAudioStream(match.id.removePrefix("yt:"), quality)
                        }
                        if (!recoveredStream.isNullOrBlank()) {
                            val transformed = com.rafliadipratama.musiceuy.solver.YouTubeCipherSolver.transformNParamInUrl(
                                getApplication(),
                                recoveredStream
                            )
                            val item = MediaItem.Builder().setMediaId(curTrack.id).setUri(transformed).build()
                            mediaController?.setMediaItem(item)
                            mediaController?.prepare()
                            mediaController?.play()
                        }
                    }
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var counter = 0
            while (isActive) {
                val pos = mediaController?.currentPosition ?: 0L
                val dur = mediaController?.duration ?: 0L
                _uiState.update {
                    it.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else it.durationMs
                    )
                }
                counter++
                if (counter % 50 == 0) { // every ~4-5 seconds
                    _uiState.value.currentTrack?.let { cur ->
                        historyManager.saveLastPlaybackState(cur, pos, _uiState.value.queue)
                    }
                }
                val delayMs = if (_uiState.value.isPlaying) 80L else 300L
                delay(delayMs)
            }
        }
    }

    fun setSearchCategory(category: String) {
        _uiState.update { it.copy(searchCategory = category) }
    }

    fun selectHomeFilter(filter: String) {
        _uiState.update { it.copy(selectedHomeFilter = filter) }
        when (filter) {
            "Semua" -> {
                _uiState.update { it.copy(searchResults = emptyList(), searchResultArtists = emptyList(), searchResultAlbums = emptyList()) }
                filterHomeCategory("Semua")
            }
            else -> {
                _uiState.update { it.copy(searchResults = emptyList(), searchResultArtists = emptyList(), searchResultAlbums = emptyList()) }
                filterHomeCategory(filter)
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        historyManager.saveSearchQuery(query)
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchHistory = historyManager.getSearchHistory(), error = null) }
            
            if (!_uiState.value.isOnline) {
                val qLower = query.trim().lowercase()
                val localResults = _uiState.value.downloadedTracks.filter {
                    it.title.lowercase().contains(qLower) || it.artist.name.lowercase().contains(qLower)
                }
                _uiState.update {
                    it.copy(
                        searchResults = localResults,
                        searchResultArtists = emptyList(),
                        searchResultAlbums = emptyList(),
                        isSearching = false,
                        error = if (localResults.isEmpty()) "Tidak ditemukan lagu terunduh untuk '$query'." else null
                    )
                }
                return@launch
            }

            val official = withContext(Dispatchers.IO) {
                try { streamSource.searchAll(query) } catch (_: Exception) { com.rafliadipratama.musiceuy.data.network.SearchAllResults() }
            }

            // Immediately show official results so search is blazing fast
            _uiState.update {
                it.copy(
                    searchResults = official.tracks,
                    searchResultArtists = official.artists,
                    searchResultAlbums = official.albums,
                    isSearching = false,
                    error = if (official.tracks.isEmpty() && official.artists.isEmpty() && official.albums.isEmpty())
                        "Tidak ditemukan hasil untuk '$query'. Coba kata kunci lain." else null
                )
            }

            // Asynchronously check plugins in background without blocking the UI
            launch(Dispatchers.IO) {
                try {
                    val pluginTracks = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        pluginManager.search(query)
                    } ?: emptyList()
                    if (pluginTracks.isNotEmpty()) {
                        _uiState.update { state ->
                            val currentTracks = state.searchResults.toMutableList()
                            var added = false
                            for (pt in pluginTracks) {
                                if (currentTracks.none { it.id == pt.id || it.title.equals(pt.title, ignoreCase = true) }) {
                                    currentTracks.add(pt)
                                    added = true
                                }
                            }
                            if (added) state.copy(searchResults = currentTracks) else state
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun removeSearchHistory(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.removeSearchQuery(query)
            _uiState.update { it.copy(searchHistory = historyManager.getSearchHistory()) }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.clearSearchHistory()
            _uiState.update { it.copy(searchHistory = emptyList()) }
        }
    }

    fun resumeLastPlayedTrack() {
        val trackToPlay = _uiState.value.currentTrack ?: _uiState.value.lastPlayedTrack ?: return
        val pos = if (_uiState.value.currentPositionMs > 0) _uiState.value.currentPositionMs else _uiState.value.lastPlayedPositionMs
        if (_uiState.value.currentTrack?.id == trackToPlay.id && _uiState.value.isPlaying) {
            mediaController?.pause()
            return
        }
        playTrack(trackToPlay)
        if (pos > 0) {
            viewModelScope.launch {
                delay(700)
                seekTo(pos)
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val notInterestedIds = withContext(Dispatchers.IO) { historyManager.getNotInterestedTrackIds() }
            val notInterestedArtists = withContext(Dispatchers.IO) { historyManager.getNotInterestedArtists() }

            val mostPlayed = withContext(Dispatchers.IO) {
                historyManager.getMostPlayedTracks().filter { track ->
                    !notInterestedIds.contains(track.id) &&
                    !notInterestedArtists.contains(track.artist.name.trim().lowercase())
                }
            }
            val favArtists = withContext(Dispatchers.IO) {
                historyManager.getFavoriteArtists().filter { artist ->
                    !notInterestedArtists.contains(artist.trim().lowercase())
                }
            }
            val playlists = withContext(Dispatchers.IO) {
                historyManager.getAllPlaylists()
            }
            val liked = withContext(Dispatchers.IO) {
                historyManager.getLikedTracks()
            }
            val likedIds = liked.map { it.id }.toSet()
            val searchHistory = withContext(Dispatchers.IO) { historyManager.getSearchHistory() }
            val lastPlayed = withContext(Dispatchers.IO) { historyManager.getLastPlayedTrack() }
            val savedQueue = withContext(Dispatchers.IO) { historyManager.getLastPlaybackQueue() }

            _uiState.update { current ->
                val track = current.currentTrack ?: lastPlayed?.first
                val pos = if (current.currentTrack == null) (lastPlayed?.second ?: 0L) else current.currentPositionMs
                current.copy(
                    mostPlayedTracks = mostPlayed,
                    favoriteArtists = favArtists,
                    playlists = playlists,
                    likedTracks = liked,
                    likedTrackIds = likedIds,
                    notInterestedTrackIds = notInterestedIds,
                    notInterestedArtists = notInterestedArtists,
                    searchHistory = searchHistory,
                    lastPlayedTrack = lastPlayed?.first,
                    lastPlayedPositionMs = lastPlayed?.second ?: 0L,
                    currentTrack = track,
                    currentPositionMs = pos,
                    durationMs = if (current.currentTrack == null && track != null) track.durationSec * 1000L else current.durationMs,
                    queue = if (current.queue.isNotEmpty()) current.queue else (if (savedQueue.isNotEmpty()) savedQueue else if (track != null) listOf(track) else emptyList())
                )
            }

            // Muat lagu yang sudah terunduh di HP
            loadDownloadedTracks()

            // Muat data Explore YouTube Music (Trending, Rilisan Baru, Musisi Populer)
            val explore = withContext(Dispatchers.IO) {
                streamSource.fetchExploreHome(favArtists)
            }
            val filteredTrending = explore.trendingTracks.filter { track ->
                !notInterestedIds.contains(track.id) &&
                !notInterestedArtists.contains(track.artist.name.trim().lowercase())
            }
            val filteredArtists = explore.popularArtists.filter { artist ->
                !notInterestedArtists.contains(artist.name.trim().lowercase())
            }
            val sortedArtists = withContext(Dispatchers.IO) {
                historyManager.getSortedPopularArtists(filteredArtists)
            }
            _uiState.update {
                it.copy(
                    trendingTracks = filteredTrending,
                    newReleases = explore.newReleases,
                    popularArtists = sortedArtists,
                    favoriteArtists = favArtists
                )
            }
            resolveMissingArtistAvatars(sortedArtists)
        }
    }

    fun refreshHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingHome = true) }
            try {
                val notInterestedIds = withContext(Dispatchers.IO) { historyManager.getNotInterestedTrackIds() }
                val notInterestedArtists = withContext(Dispatchers.IO) { historyManager.getNotInterestedArtists() }

                val favArtists = withContext(Dispatchers.IO) {
                    historyManager.getFavoriteArtists().filter { artist ->
                        !notInterestedArtists.contains(artist.trim().lowercase())
                    }
                }

                val explore = withContext(Dispatchers.IO) {
                    streamSource.fetchExploreHome(favArtists)
                }
                val filteredTrending = explore.trendingTracks.filter { track ->
                    !notInterestedIds.contains(track.id) &&
                    !notInterestedArtists.contains(track.artist.name.trim().lowercase())
                }
                val filteredArtists = explore.popularArtists.filter { artist ->
                    !notInterestedArtists.contains(artist.name.trim().lowercase())
                }
                val sortedArtists = withContext(Dispatchers.IO) {
                    historyManager.getSortedPopularArtists(filteredArtists)
                }

                val mostPlayed = withContext(Dispatchers.IO) {
                    historyManager.getMostPlayedTracks().filter { track ->
                        !notInterestedIds.contains(track.id) &&
                        !notInterestedArtists.contains(track.artist.name.trim().lowercase())
                    }
                }

                _uiState.update {
                    it.copy(
                        isRefreshingHome = false,
                        trendingTracks = filteredTrending,
                        mostPlayedTracks = mostPlayed,
                        newReleases = explore.newReleases,
                        popularArtists = sortedArtists,
                        favoriteArtists = favArtists,
                        notInterestedTrackIds = notInterestedIds,
                        notInterestedArtists = notInterestedArtists,
                        downloadNotificationMessage = "Beranda berhasil disegarkan."
                    )
                }
                resolveMissingArtistAvatars(sortedArtists)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshingHome = false,
                        error = "Gagal memperbarui beranda: ${e.message}"
                    )
                }
            }
        }
    }

    private fun resolveMissingArtistAvatars(artists: List<Artist>) {
        viewModelScope.launch(Dispatchers.IO) {
            var anyResolved = false
            val updatedList = _uiState.value.popularArtists.map { artist ->
                val currentAvatar = artist.avatarUrl?.takeIf { it.isNotBlank() }
                    ?: com.rafliadipratama.musiceuy.data.network.ArtistCatalog.getVerifiedAvatarUrl(artist.name)
                    ?: historyManager.getCachedArtistAvatar(artist.name)

                if (currentAvatar.isNullOrBlank()) {
                    val resolved = streamSource.resolveArtistAvatar(artist.name)
                    if (!resolved.isNullOrBlank()) {
                        historyManager.updateArtistAvatar(artist.name, resolved)
                        com.rafliadipratama.musiceuy.data.network.ArtistCatalog.cacheAvatar(artist.name, resolved)
                        anyResolved = true
                        artist.copy(avatarUrl = resolved)
                    } else artist
                } else if (artist.avatarUrl != currentAvatar) {
                    anyResolved = true
                    artist.copy(avatarUrl = currentAvatar)
                } else artist
            }
            if (anyResolved) {
                _uiState.update { it.copy(popularArtists = updatedList) }
            }
        }
    }

    fun markTrackNotInterested(track: Track) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyManager.markTrackNotInterested(track.id, track.artist.name)
            }
            val notInterestedIds = historyManager.getNotInterestedTrackIds()
            val notInterestedArtists = historyManager.getNotInterestedArtists()

            // Jika lagu yang ditandai sedang diputar, otomatis lewati ke trek berikutnya
            if (_uiState.value.currentTrack?.id == track.id) {
                nextTrack()
            }

            _uiState.update { current ->
                val filteredTrending = current.trendingTracks.filter {
                    it.id != track.id && !it.artist.name.equals(track.artist.name, ignoreCase = true)
                }
                val filteredMostPlayed = current.mostPlayedTracks.filter {
                    it.id != track.id && !it.artist.name.equals(track.artist.name, ignoreCase = true)
                }
                val filteredSearchResults = current.searchResults.filter {
                    it.id != track.id && !it.artist.name.equals(track.artist.name, ignoreCase = true)
                }
                val filteredQueue = current.queue.filter {
                    it.id != track.id && !it.artist.name.equals(track.artist.name, ignoreCase = true)
                }

                current.copy(
                    trendingTracks = filteredTrending,
                    mostPlayedTracks = filteredMostPlayed,
                    searchResults = filteredSearchResults,
                    queue = filteredQueue,
                    notInterestedTrackIds = notInterestedIds,
                    notInterestedArtists = notInterestedArtists,
                    downloadNotificationMessage = "Lagu '${track.title}' & musisi ${track.artist.name} disembunyikan dari rekomendasi."
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

        // Record locally for offline recommendation & history & last played
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.recordTrackPlayed(track)
            historyManager.saveLastPlaybackState(track, 0L, _uiState.value.queue)
            val updatedMostPlayed = historyManager.getMostPlayedTracks()
            val updatedFavArtists = historyManager.getFavoriteArtists()
            val reSortedArtists = historyManager.getSortedPopularArtists(_uiState.value.popularArtists)
            _uiState.update {
                it.copy(
                    mostPlayedTracks = updatedMostPlayed,
                    favoriteArtists = updatedFavArtists,
                    popularArtists = reSortedArtists,
                    lastPlayedTrack = track,
                    lastPlayedPositionMs = 0L
                )
            }
        }

        // Fetch synchronized lyrics in background if plugin enabled
        viewModelScope.launch {
            val lrcPlugin = pluginManager.getLrcLibPlugin()
            if (lrcPlugin == null || !lrcPlugin.isEnabled) {
                _uiState.update { it.copy(lyrics = com.rafliadipratama.musiceuy.data.model.Lyrics(), isLoadingLyrics = false) }
                return@launch
            }
            val lyrics = lrcPlugin.fetchLyrics(track.artist.name, track.title)
            _uiState.update { it.copy(lyrics = lyrics, isLoadingLyrics = false) }
        }

        viewModelScope.launch {
            var activeAudioUri = track.audioUrl

            android.util.Log.d("MusicEuy", "playTrack: title=${track.title}, artist=${track.artist.name}, initialUrl=${track.audioUrl}")

            var isLocalAudio = activeAudioUri.startsWith("content://") || activeAudioUri.startsWith("file://")

            // Check if track matches a locally downloaded track
            if (!isLocalAudio) {
                val downloadedMatch = _uiState.value.downloadedTracks.firstOrNull {
                    it.id == track.id || (it.title.equals(track.title, ignoreCase = true) && it.artist.name.equals(track.artist.name, ignoreCase = true))
                }
                if (downloadedMatch != null && (downloadedMatch.audioUrl.startsWith("content://") || downloadedMatch.audioUrl.startsWith("file://"))) {
                    activeAudioUri = downloadedMatch.audioUrl
                    isLocalAudio = true
                }
            }

            // Gracefully handle playback when offline and track is not local
            if (!isLocalAudio && !_uiState.value.isOnline) {
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        downloadNotificationMessage = "Lagu ini memerlukan koneksi internet. Unduh terlebih dahulu untuk memutar offline."
                    )
                }
                return@launch
            }

            // Resolve stream if needed
            if (!isLocalAudio) {
                if (!activeAudioUri.startsWith("http") || activeAudioUri.startsWith("yt:") || track.id.startsWith("yt:")) {
                    val resolvedStream = withContext(Dispatchers.IO) {
                        pluginManager.resolveStream(track)
                    }
                    if (!resolvedStream.isNullOrBlank() && resolvedStream.startsWith("http")) {
                        activeAudioUri = resolvedStream
                        android.util.Log.d("MusicEuy", "Resolved via PluginManager: $activeAudioUri")
                    } else {
                        val quality = com.rafliadipratama.musiceuy.audio.AudioEffectManager.getAudioQuality(getApplication())
                        val fallbackStream = withContext(Dispatchers.IO) {
                            val vid = track.id.removePrefix("yt:").removePrefix("mono_")
                            streamSource.resolveYouTubeAudioStream(vid, quality)
                        }
                        if (!fallbackStream.isNullOrBlank()) {
                            activeAudioUri = fallbackStream
                        }
                    }
                }

                // If Monochrome track, ensure canonical endpoint
                if (track.id.startsWith("mono_") || activeAudioUri.contains("monochrome.st")) {
                    val monoId = track.id.removePrefix("mono_")
                    activeAudioUri = "https://tracks.monochrome.st/track/$monoId"
                }

                // BULLETPROOF FALLBACK:
                // If activeAudioUri is still not valid HTTP, search YouTube Music dynamically
                if (!activeAudioUri.startsWith("http")) {
                    val q = "${track.title} ${track.artist.name}".trim()
                    android.util.Log.w("MusicEuy", "Direct URI resolution failed, searching YouTube Music fallback: $q")
                    val ytMatches = withContext(Dispatchers.IO) {
                        try { streamSource.searchYouTubeMusic(q) } catch (_: Exception) { emptyList() }
                    }
                    val topMatch = ytMatches.firstOrNull()
                    if (topMatch != null) {
                        val quality = com.rafliadipratama.musiceuy.audio.AudioEffectManager.getAudioQuality(getApplication())
                        val resolved = withContext(Dispatchers.IO) {
                            streamSource.resolveYouTubeAudioStream(topMatch.id.removePrefix("yt:"), quality)
                        }
                        if (!resolved.isNullOrBlank()) {
                            activeAudioUri = resolved
                        }
                    }
                }

                // Apply YouTube Cipher & n-param solver if stream is from googlevideo.com
                if (activeAudioUri.startsWith("http") && activeAudioUri.contains("googlevideo.com")) {
                    activeAudioUri = com.rafliadipratama.musiceuy.solver.YouTubeCipherSolver.transformNParamInUrl(
                        getApplication(),
                        activeAudioUri
                    )
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

            // Apply EBU R128 / YouTube ReplayGain volume normalization
            val loudnessDb = com.rafliadipratama.musiceuy.data.network.MusicStreamSource.getLoudnessDb(track.id)
            com.rafliadipratama.musiceuy.audio.AudioEffectManager.applyTrackReplayGain(loudnessDb)

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

            // High-speed background pre-resolution for the next track in queue (adopted from Musicfy)
            preloadNextTrackInQueue(activeTrack, currentQueue)
        }
    }

    private fun preloadNextTrackInQueue(current: Track, queue: List<Track>) {
        if (queue.isEmpty() || !_uiState.value.isOnline) return
        val currentIndex = queue.indexOfFirst { it.id == current.id }
        val nextTrack = if (currentIndex != -1 && currentIndex < queue.size - 1) {
            queue[currentIndex + 1]
        } else if (_uiState.value.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
            queue.first()
        } else null

        if (nextTrack != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val audioUrl = nextTrack.audioUrl
                    val isLocal = audioUrl.startsWith("content://") || audioUrl.startsWith("file://")
                    if (!isLocal) {
                        val quality = com.rafliadipratama.musiceuy.audio.AudioEffectManager.getAudioQuality(getApplication())
                        val vid = nextTrack.id.removePrefix("yt:").removePrefix("mono_")
                        android.util.Log.d("MusicEuy", "Preloading next track stream in background: ${nextTrack.title} ($vid)")
                        streamSource.resolveYouTubeAudioStream(vid, quality)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MusicEuy", "Preload next track failed: ${e.message}")
                }
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller == null) {
            val current = _uiState.value.currentTrack
            if (current != null) {
                resumeLastPlayedTrack()
            }
            return
        }
        if (controller.isPlaying) {
            controller.pause()
            _uiState.value.currentTrack?.let { cur ->
                historyManager.saveLastPlaybackState(cur, controller.currentPosition, _uiState.value.queue)
            }
        } else {
            if (controller.currentMediaItem == null && _uiState.value.currentTrack != null) {
                resumeLastPlayedTrack()
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
        _uiState.value.currentTrack?.let { cur ->
            historyManager.saveLastPlaybackState(cur, positionMs, _uiState.value.queue)
        }
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

        // Trigger Last.fm Scrobbler plugin if enabled
        state.currentTrack?.let { completedTrack ->
            val lastFm = pluginManager.getLastFmPlugin()
            if (lastFm != null && lastFm.isEnabled) {
                viewModelScope.launch {
                    lastFm.scrobbleTrack(completedTrack)
                }
            }
        }

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
                streamSource.fetchSongRadio(endedTrack)
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

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.queue.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                state.copy(queue = list)
            } else {
                state
            }
        }
    }

    fun reorderUpcomingTracks(fromUpcomingIndex: Int, toUpcomingIndex: Int) {
        _uiState.update { state ->
            val currentTrack = state.currentTrack ?: return@update state
            val currentQueue = state.queue.toMutableList()
            val currentIndex = currentQueue.indexOfFirst { it.id == currentTrack.id }
            if (currentIndex == -1) {
                if (fromUpcomingIndex in currentQueue.indices && toUpcomingIndex in currentQueue.indices && fromUpcomingIndex != toUpcomingIndex) {
                    val item = currentQueue.removeAt(fromUpcomingIndex)
                    currentQueue.add(toUpcomingIndex, item)
                    return@update state.copy(queue = currentQueue)
                }
                return@update state
            }

            val played = currentQueue.subList(0, currentIndex + 1)
            val upcoming = currentQueue.subList(currentIndex + 1, currentQueue.size).toMutableList()

            if (fromUpcomingIndex in upcoming.indices && toUpcomingIndex in upcoming.indices && fromUpcomingIndex != toUpcomingIndex) {
                val item = upcoming.removeAt(fromUpcomingIndex)
                upcoming.add(toUpcomingIndex, item)
                state.copy(queue = played + upcoming)
            } else {
                state
            }
        }
    }

    fun moveTrackToNext(trackId: String) {
        _uiState.update { state ->
            val currentTrack = state.currentTrack ?: return@update state
            val currentIndex = state.queue.indexOfFirst { it.id == currentTrack.id }
            val targetIndex = state.queue.indexOfFirst { it.id == trackId }
            if (currentIndex != -1 && targetIndex != -1 && targetIndex != currentIndex + 1) {
                val list = state.queue.toMutableList()
                val item = list.removeAt(targetIndex)
                val insertIndex = if (targetIndex < currentIndex) currentIndex else currentIndex + 1
                list.add(insertIndex.coerceIn(0, list.size), item)
                state.copy(
                    queue = list,
                    downloadNotificationMessage = "'${item.title}' diprioritaskan untuk diputar berikutnya."
                )
            } else {
                state
            }
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
        _uiState.update { it.copy(selectedPlaylist = null, isLoadingPlaylist = false) }
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!track.audioUrl.startsWith("content://") && !track.audioUrl.startsWith("file://")) {
                    val quality = com.rafliadipratama.musiceuy.audio.AudioEffectManager.getAudioQuality(getApplication())
                    val vid = track.id.removePrefix("yt:").removePrefix("mono_")
                    streamSource.resolveYouTubeAudioStream(vid, quality)
                }
            } catch (_: Exception) {}
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
                    streamSource.getArtistTopTracks(artist.name)
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
                val resolvedAvatar = fullArtist?.avatarUrl?.takeIf { it.isNotBlank() }
                if (resolvedAvatar != null) {
                    historyManager.updateArtistAvatar(artist.name, resolvedAvatar)
                    com.rafliadipratama.musiceuy.data.network.ArtistCatalog.cacheAvatar(artist.name, resolvedAvatar)
                }
                _uiState.update { state ->
                    val updatedPopular = state.popularArtists.map { a ->
                        if (a.name.equals(artist.name, ignoreCase = true) && resolvedAvatar != null) {
                            a.copy(avatarUrl = resolvedAvatar)
                        } else a
                    }
                    state.copy(
                        selectedArtist = fullArtist ?: state.selectedArtist,
                        artistAlbums = albums,
                        isLoadingAlbums = false,
                        popularArtists = updatedPopular
                    )
                }
            }
        }
    }

    fun openAlbum(album: Album) {
        viewModelScope.launch {
            val effectiveYear = if (album.year.isNotBlank() && album.year >= "2025") album.year else "2026"
            _uiState.update {
                it.copy(
                    isPlayerExpanded = false,
                    isLoadingPlaylist = true,
                    selectedPlaylist = Playlist(
                        id = album.id,
                        name = album.title,
                        description = "Album • $effectiveYear • ${album.artistName}",
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
                        isLoadingPlaylist = false,
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
                    viewModelScope.launch(Dispatchers.IO) {
                        try { lrcClient.fetchLyrics(track.artist.name, track.title) } catch (_: Exception) {}
                    }
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

    fun startSongRadio(track: Track) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAutoplayEnabled = true,
                    downloadNotificationMessage = "Memulai Radio Lagu '${track.title}'..."
                )
            }
            val related = streamSource.fetchSongRadio(track)
            val queue = listOf(track) + related
            playTrack(track, queue)
        }
    }

    fun filterHomeCategory(category: String) {
        _uiState.update { it.copy(selectedHomeFilter = category) }
        viewModelScope.launch {
            if (category == "Semua") {
                val favArtists = withContext(Dispatchers.IO) { historyManager.getFavoriteArtists() }
                val homeData = withContext(Dispatchers.IO) { streamSource.fetchExploreHome(favArtists) }
                val sortedArtists = withContext(Dispatchers.IO) { historyManager.getSortedPopularArtists(homeData.popularArtists) }
                _uiState.update {
                    it.copy(
                        trendingTracks = homeData.trendingTracks,
                        newReleases = homeData.newReleases,
                        popularArtists = sortedArtists
                    )
                }
            } else {
                val tracks = streamSource.fetchMoodCategoryTracks(category)
                if (tracks.isNotEmpty()) {
                    _uiState.update { it.copy(trendingTracks = tracks) }
                }
            }
        }
    }

    fun openOfficialChart(chartName: String, browseId: String, subtitle: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPlayerExpanded = false,
                    isLoadingPlaylist = true,
                    selectedPlaylist = Playlist(
                        id = browseId,
                        name = chartName,
                        description = "$subtitle • Tangga Lagu Resmi 2026",
                        coverUrl = "",
                        tracks = emptyList()
                    )
                )
            }
            val tracks = withContext(Dispatchers.IO) {
                streamSource.fetchOfficialChart(chartName)
            }
            _uiState.update { state ->
                if (state.selectedPlaylist?.id == browseId) {
                    val cover = tracks.firstOrNull()?.coverUrl ?: ""
                    state.copy(
                        isLoadingPlaylist = false,
                        selectedPlaylist = state.selectedPlaylist.copy(
                            tracks = tracks,
                            coverUrl = cover
                        )
                    )
                } else state
            }
        }
    }

    fun playOfficialChart(chartName: String) {
        openOfficialChart(chartName, "chart_${chartName.hashCode()}", "YouTube Music Official")
    }

    fun showImportPlaylistDialog() {
        _uiState.update { it.copy(isImportPlaylistDialogVisible = true) }
    }

    fun dismissImportPlaylistDialog() {
        _uiState.update { it.copy(isImportPlaylistDialogVisible = false, isImportingPlaylist = false) }
    }

    fun importPlaylistFromUrl(url: String, customName: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingPlaylist = true) }
            try {
                val res = playlistImporter.importFromUrl(url, customName)
                historyManager.saveImportedPlaylist(res.playlist)
                _uiState.update {
                    it.copy(
                        playlists = historyManager.getAllPlaylists(),
                        isImportingPlaylist = false,
                        isImportPlaylistDialogVisible = false
                    )
                }
                onResult(true, "Berhasil mengimpor playlist ${res.sourceName}: \"${res.playlist.name}\" (${res.tracks.size} lagu)")
            } catch (e: Exception) {
                _uiState.update { it.copy(isImportingPlaylist = false) }
                onResult(false, e.message ?: "Gagal mengimpor playlist")
            }
        }
    }

    fun importExternalPlaylist(url: String, customName: String? = null) {
        importPlaylistFromUrl(url, customName) { _, _ -> }
    }

    fun showRecapDialog() {
        val stats = historyManager.getListeningStats()
        _uiState.update { it.copy(isRecapDialogVisible = true, listeningStats = stats) }
    }

    fun dismissRecapDialog() {
        _uiState.update { it.copy(isRecapDialogVisible = false) }
    }

    fun showStorageDialog() {
        val cacheSize = com.rafliadipratama.musiceuy.data.local.MusicCacheManager.getCacheSizeBytes(getApplication())
        val downloadSize = downloadManager.getTotalDownloadedSizeBytes()
        _uiState.update {
            it.copy(
                isStorageDialogVisible = true,
                audioCacheSizeBytes = cacheSize,
                downloadedSizeBytes = downloadSize
            )
        }
    }

    fun dismissStorageDialog() {
        _uiState.update { it.copy(isStorageDialogVisible = false) }
    }

    fun showLanguageDialog() {
        _uiState.update { it.copy(isLanguageDialogVisible = true) }
    }

    fun dismissLanguageDialog() {
        _uiState.update { it.copy(isLanguageDialogVisible = false) }
    }

    fun setLanguage(languageCode: String) {
        com.rafliadipratama.musiceuy.util.LanguageHelper.applyLanguage(getApplication(), languageCode)
        _uiState.update {
            it.copy(
                currentLanguageCode = languageCode,
                isLanguageDialogVisible = false
            )
        }
    }

    fun clearAudioCache() {
        com.rafliadipratama.musiceuy.data.local.MusicCacheManager.clearCache(getApplication())
        val newCacheSize = com.rafliadipratama.musiceuy.data.local.MusicCacheManager.getCacheSizeBytes(getApplication())
        _uiState.update {
            it.copy(
                audioCacheSizeBytes = newCacheSize,
                downloadNotificationMessage = "Cache streaming audio berhasil dibersihkan."
            )
        }
    }

    fun completeOnboarding(selectedArtists: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalibratingOnboarding = true) }
            withContext(Dispatchers.IO) {
                historyManager.saveOnboardingArtists(selectedArtists)
                historyManager.setOnboardingCompleted(true)
            }

            // Kalibrasi Beranda: Ambil lagu-lagu top dari musisi pilihan pengguna secara instan
            val calibratedTracks = mutableListOf<Track>()
            withContext(Dispatchers.IO) {
                for (artist in selectedArtists.take(4)) {
                    try {
                        val tracks = streamSource.searchTracks(artist).take(3)
                        calibratedTracks.addAll(tracks)
                    } catch (e: Exception) {
                        android.util.Log.e("MusicEuy", "Gagal kalibrasi artis $artist: ${e.message}")
                    }
                }
            }

            _uiState.update { current ->
                val updatedMostPlayed = if (current.mostPlayedTracks.isEmpty()) calibratedTracks else current.mostPlayedTracks
                val updatedTrending = if (calibratedTracks.isNotEmpty()) calibratedTracks + current.trendingTracks else current.trendingTracks
                current.copy(
                    isOnboardingCompleted = true,
                    isCalibratingOnboarding = false,
                    favoriteArtists = historyManager.getFavoriteArtists(),
                    mostPlayedTracks = updatedMostPlayed,
                    trendingTracks = updatedTrending
                )
            }
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyManager.setOnboardingCompleted(true)
            }
            _uiState.update { it.copy(isOnboardingCompleted = true) }
        }
    }

    fun restartOnboarding() {
        _uiState.update { it.copy(isOnboardingCompleted = false) }
    }

    fun playAllDownloadedTracks() {
        val downloaded = _uiState.value.downloadedTracks
        if (downloaded.isNotEmpty()) {
            playTrack(downloaded.first(), downloaded)
        }
    }

    fun openProfile() {
        _uiState.update { it.copy(isProfileVisible = true) }
    }

    fun closeProfile() {
        _uiState.update { it.copy(isProfileVisible = false) }
    }

    fun getUserProfile(): com.rafliadipratama.musiceuy.data.local.UserProfile {
        return userProfileManager.getProfile()
    }

    fun saveUserProfile(profile: com.rafliadipratama.musiceuy.data.local.UserProfile) {
        userProfileManager.updateProfile(profile)
        _uiState.update { it.copy(userProfile = profile) }
    }

    fun openLyricsShare(snippet: String) {
        _uiState.update { it.copy(isLyricsShareDialogVisible = true, selectedLyricForShare = snippet) }
    }

    fun closeLyricsShare() {
        _uiState.update { it.copy(isLyricsShareDialogVisible = false) }
    }

    override fun onCleared() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
