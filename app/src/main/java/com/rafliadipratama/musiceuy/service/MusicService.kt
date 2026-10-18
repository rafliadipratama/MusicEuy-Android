package com.rafliadipratama.musiceuy.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rafliadipratama.musiceuy.MainActivity

/**
 * Foreground MediaLibraryService powering background audio playback,
 * lockscreen media controls, Bluetooth audio focus, and Android Auto dashboard media browsing.
 */
class MusicService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    private val libraryCallback = object : MediaLibrarySession.Callback {
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
                    // Root Categories for Android Auto Dashboard
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
                    val dm = com.rafliadipratama.musiceuy.data.local.MusicDownloadManager(this@MusicService)
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
                    val hm = com.rafliadipratama.musiceuy.data.local.LocalMusicHistoryManager(this@MusicService)
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

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(com.rafliadipratama.musiceuy.data.local.MusicCacheManager.createCacheDataSourceFactory(this))

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500, // Instant playback: starts playing with just 500ms of audio buffer
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(audioAttributes, true) // automatic audio focus
            .setHandleAudioBecomingNoisy(true) // pause when headphones unplugged
            .build()

        player?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    com.rafliadipratama.musiceuy.audio.AudioEffectManager.attachAudioSession(this@MusicService, audioSessionId)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicEuy", "ExoPlayer Error [${error.errorCodeName} / ${error.errorCode}]: ${error.message}", error)
            }
        })

        val initialSessionId = player?.audioSessionId ?: 0
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

        // Setup Dedicated Playback Notification Channel & Musicfy-Style Notification Provider
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            getString(com.rafliadipratama.musiceuy.R.string.music_player),
            android.app.NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(com.rafliadipratama.musiceuy.R.string.music_player_notification_desc)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)

        setMediaNotificationProvider(
            MusicEuyNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                com.rafliadipratama.musiceuy.R.string.music_player
            ).setSmallIcon(com.rafliadipratama.musiceuy.R.drawable.ic_musiceuy_notification)
        )

        player?.let { exo ->
            mediaLibrarySession = MediaLibrarySession.Builder(this, exo, libraryCallback)
                .setSessionActivity(pendingIntent)
                .build()
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "musiceuy_playback_channel"
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        android.util.Log.d("MusicEuy", "MusicService onTaskRemoved: Stopping playback and releasing session")
        val exo = player
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
        player = null
        stopSelf()
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        player = null
        com.rafliadipratama.musiceuy.audio.AudioEffectManager.releaseEffects()
        super.onDestroy()
    }
}
