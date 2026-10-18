package com.rafliadipratama.musiceuy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rafliadipratama.musiceuy.MainActivity
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.data.local.LocalMusicHistoryManager
import com.rafliadipratama.musiceuy.data.local.MusicCacheManager
import com.rafliadipratama.musiceuy.data.local.MusicDownloadManager
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.utils.CoilBitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground MediaLibraryService powering background audio playback,
 * lockscreen media controls, Bluetooth audio focus, and Android Auto dashboard media browsing.
 *
 * Implements Android MediaStyle rich notification matching Musicfy with custom action controls:
 * - Like / Heart toggle
 * - Previous track
 * - Play / Pause
 * - Next track
 * - Repeat mode cycle
 * - Interactive scrubber / seek bar
 */
@OptIn(UnstableApi::class)
class MusicService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var rawPlayer: ExoPlayer? = null
    private var forwardingPlayer: ForwardingPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            return MediaSession.ConnectionResult.accept(
                connectionResult.availableSessionCommands.buildUpon()
                    .add(MediaSessionConstants.CommandToggleLike)
                    .add(MediaSessionConstants.CommandToggleRepeatMode)
                    .add(MediaSessionConstants.CommandToggleShuffle)
                    .build(),
                connectionResult.availablePlayerCommands.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                MediaSessionConstants.ACTION_TOGGLE_LIKE -> {
                    val handled = onToggleLikeCallback?.invoke() ?: false
                    if (!handled) {
                        val currentMedia = session.player.currentMediaItem
                        if (currentMedia != null) {
                            val hm = LocalMusicHistoryManager(this@MusicService)
                            val isLiked = hm.isTrackLiked(currentMedia.mediaId)
                            val dummyTrack = Track(
                                id = currentMedia.mediaId,
                                title = currentMedia.mediaMetadata.title?.toString() ?: "",
                                artist = Artist(
                                    id = "artist_${currentMedia.mediaId}",
                                    name = currentMedia.mediaMetadata.artist?.toString() ?: ""
                                ),
                                albumTitle = currentMedia.mediaMetadata.albumTitle?.toString() ?: "",
                                coverUrl = currentMedia.mediaMetadata.artworkUri?.toString() ?: "",
                                audioUrl = ""
                            )
                            hm.toggleLikedTrack(dummyTrack)
                            updateCustomLayout(!isLiked, session.player.repeatMode)
                        }
                    }
                }
                MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> {
                    val nextRepeat = when (session.player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                        else -> Player.REPEAT_MODE_OFF
                    }
                    session.player.repeatMode = nextRepeat
                    onToggleRepeatCallback?.invoke(nextRepeat)
                    val isLiked = session.player.currentMediaItem?.let {
                        LocalMusicHistoryManager(this@MusicService).isTrackLiked(it.mediaId)
                    } ?: false
                    updateCustomLayout(isLiked, nextRepeat)
                }
                MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> {
                    session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("ROOT_ID")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("MusicEuy")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = mutableListOf<MediaItem>()

            try {
                if (parentId == "ROOT_ID") {
                    items.add(
                        MediaItem.Builder()
                            .setMediaId("FOLDER_FAVORITES")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle("Lagu Disukai")
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build()
                            )
                            .build()
                    )
                    items.add(
                        MediaItem.Builder()
                            .setMediaId("FOLDER_DOWNLOADED")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle("Lagu Terunduh (Offline)")
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build()
                            )
                            .build()
                    )
                    items.add(
                        MediaItem.Builder()
                            .setMediaId("FOLDER_HISTORY")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle("Baru Saja Diputar")
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .build()
                            )
                            .build()
                    )
                } else if (parentId == "FOLDER_DOWNLOADED") {
                    val dm = MusicDownloadManager(this@MusicService)
                    val downloaded = dm.getDownloadedTracks()
                    for (t in downloaded) {
                        items.add(
                            MediaItem.Builder()
                                .setMediaId(t.id)
                                .setUri(t.audioUrl)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(t.title)
                                        .setArtist(t.artist.name)
                                        .setIsBrowsable(false)
                                        .setIsPlayable(true)
                                        .build()
                                )
                                .build()
                        )
                    }
                } else if (parentId == "FOLDER_FAVORITES" || parentId == "FOLDER_HISTORY") {
                    val hm = LocalMusicHistoryManager(this@MusicService)
                    val tracks = if (parentId == "FOLDER_FAVORITES") hm.getLikedTracks() else hm.getRecentlyPlayedTracks()
                    for (t in tracks) {
                        items.add(
                            MediaItem.Builder()
                                .setMediaId(t.id)
                                .setUri(if (t.audioUrl.startsWith("http")) t.audioUrl else null)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(t.title)
                                        .setArtist(t.artist.name)
                                        .setIsBrowsable(false)
                                        .setIsPlayable(true)
                                        .build()
                                )
                                .build()
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicEuy", "Error building MediaLibrary children: ${e.message}")
            }

            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(MusicCacheManager.createCacheDataSourceFactory(this))

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        rawPlayer = exo

        // ForwardingPlayer ensures COMMAND_SEEK_TO_NEXT and COMMAND_SEEK_TO_PREVIOUS
        // are always enabled so SystemUI displays previous & next buttons in the notification controls row
        val fPlayer = object : ForwardingPlayer(exo) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(COMMAND_SEEK_TO_NEXT)
                    .add(COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                if (command == COMMAND_SEEK_TO_NEXT || command == COMMAND_SEEK_TO_PREVIOUS) {
                    return true
                }
                return super.isCommandAvailable(command)
            }

            override fun seekToNext() {
                onNextCallback?.invoke() ?: super.seekToNext()
            }

            override fun seekToNextMediaItem() {
                onNextCallback?.invoke() ?: super.seekToNextMediaItem()
            }

            override fun seekToPrevious() {
                if (currentPosition > 3000L) {
                    seekTo(0)
                } else {
                    onPrevCallback?.invoke() ?: super.seekToPrevious()
                }
            }

            override fun seekToPreviousMediaItem() {
                if (currentPosition > 3000L) {
                    seekTo(0)
                } else {
                    onPrevCallback?.invoke() ?: super.seekToPreviousMediaItem()
                }
            }
        }
        forwardingPlayer = fPlayer

        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    com.rafliadipratama.musiceuy.audio.AudioEffectManager.attachAudioSession(this@MusicService, audioSessionId)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicEuy", "ExoPlayer Error [${error.errorCodeName} / ${error.errorCode}]: ${error.message}", error)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    val hm = LocalMusicHistoryManager(this@MusicService)
                    val isLiked = hm.isTrackLiked(mediaItem.mediaId)
                    updateCustomLayout(isLiked, exo.repeatMode)
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mediaItem = exo.currentMediaItem
                val isLiked = if (mediaItem != null) {
                    LocalMusicHistoryManager(this@MusicService).isTrackLiked(mediaItem.mediaId)
                } else false
                updateCustomLayout(isLiked, repeatMode)
            }
        })

        val initialSessionId = exo.audioSessionId
        if (initialSessionId != 0 && initialSessionId != C.AUDIO_SESSION_ID_UNSET) {
            com.rafliadipratama.musiceuy.audio.AudioEffectManager.attachAudioSession(this, initialSessionId)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Setup notification channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.music_player),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.music_player_notification_desc)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        // Musicfy architecture: start foreground immediately to guarantee continuous playback
        try {
            val initialNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.music_player))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_musiceuy_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Failed to start initial foreground notification: ${e.message}")
        }

        // Official Media3 DefaultMediaNotificationProvider
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            ).apply {
                setSmallIcon(R.drawable.ic_musiceuy_notification)
            }
        )

        // Build MediaLibrarySession with CoilBitmapLoader to load cover artwork
        mediaLibrarySession = MediaLibrarySession.Builder(this, fPlayer, libraryCallback)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(CoilBitmapLoader(this, serviceScope))
            .build()

        // Set initial custom layout actions (Heart and Repeat)
        updateCustomLayout(isLiked = false, repeatMode = exo.repeatMode)
    }

    fun updateCustomLayout(isLiked: Boolean, repeatMode: Int) {
        val likeButton = CommandButton.Builder()
            .setDisplayName(getString(if (isLiked) R.string.action_remove_like else R.string.action_like))
            .setIconResId(if (isLiked) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            .setSessionCommand(MediaSessionConstants.CommandToggleLike)
            .setEnabled(true)
            .build()

        val repeatIcon = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> R.drawable.repeat
            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
            Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
            else -> R.drawable.repeat
        }

        val repeatDisplayName = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> getString(R.string.repeat_mode_off)
            Player.REPEAT_MODE_ONE -> getString(R.string.repeat_mode_one)
            Player.REPEAT_MODE_ALL -> getString(R.string.repeat_mode_all)
            else -> getString(R.string.repeat_mode_off)
        }

        val repeatButton = CommandButton.Builder()
            .setDisplayName(repeatDisplayName)
            .setIconResId(repeatIcon)
            .setSessionCommand(MediaSessionConstants.CommandToggleRepeatMode)
            .setEnabled(true)
            .build()

        mediaLibrarySession?.setCustomLayout(listOf(likeButton, repeatButton))
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "musiceuy_playback_channel"

        var instance: MusicService? = null
        var onNextCallback: (() -> Unit)? = null
        var onPrevCallback: (() -> Unit)? = null
        var onToggleLikeCallback: (() -> Boolean)? = null
        var onToggleRepeatCallback: ((Int) -> Unit)? = null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        android.util.Log.d("MusicEuy", "MusicService onTaskRemoved: Stopping playback and releasing session")
        val exo = rawPlayer
        if (exo != null) {
            exo.pause()
            exo.stop()
            exo.clearMediaItems()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        rawPlayer = null
        forwardingPlayer = null
        stopSelf()
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        rawPlayer = null
        forwardingPlayer = null
        com.rafliadipratama.musiceuy.audio.AudioEffectManager.releaseEffects()
        super.onDestroy()
    }
}
