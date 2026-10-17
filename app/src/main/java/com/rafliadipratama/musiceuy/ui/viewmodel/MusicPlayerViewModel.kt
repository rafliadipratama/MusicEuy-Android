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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val searchResults: List<Track> = emptyList(),
    val trendingTracks: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val streamSource = MusicStreamSource()
    private val lrcClient = LrcLibClient()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var progressJob: Job? = null

    init {
        initMediaController()
        loadTrending()
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
            val results = streamSource.searchTracks(query)
            _uiState.update {
                it.copy(
                    searchResults = results,
                    isSearching = false,
                    error = if (results.isEmpty()) "Lagu tidak ditemukan. Coba kata kunci lain." else null
                )
            }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            val trending = streamSource.fetchTrending()
            _uiState.update { it.copy(trendingTracks = trending) }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        val controller = mediaController ?: return
        val currentQueue = newQueue ?: _uiState.value.queue.ifEmpty { listOf(track) }

        _uiState.update {
            it.copy(
                currentTrack = track,
                queue = currentQueue,
                currentPositionMs = 0L,
                lyrics = Lyrics(),
                error = null
            )
        }

        // Fetch synchronized lyrics in background
        viewModelScope.launch {
            val lyrics = lrcClient.fetchLyrics(track.artist.name, track.title)
            _uiState.update { it.copy(lyrics = lyrics) }
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist.name)
            .setAlbumTitle(track.albumTitle ?: "")
            .setArtworkUri(Uri.parse(track.coverUrl))
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.audioUrl)
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
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

    override fun onCleared() {
        progressJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
