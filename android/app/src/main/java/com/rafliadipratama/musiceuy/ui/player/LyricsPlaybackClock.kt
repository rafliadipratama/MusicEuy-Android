package com.rafliadipratama.musiceuy.ui.player

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/** Read this state in drawing or derived state, never at the player screen's composition root. */
@Composable
internal fun rememberLyricsPlaybackClock(sampleMs: Long, playing: Boolean, trackId: String?): State<Long> {
    val position = remember(trackId) { mutableLongStateOf(sampleMs) }
    val sampledAt = remember(sampleMs, playing, trackId) { SystemClock.uptimeMillis() }
    LaunchedEffect(sampleMs, playing, trackId) {
        position.longValue = sampleMs
        if (playing) {
            while (true) {
                withFrameNanos {
                    position.longValue = lyricFramePosition(sampleMs, sampledAt, SystemClock.uptimeMillis(), playing)
                }
            }
        }
    }
    return position
}

internal fun lyricFramePosition(sampleMs: Long, sampledAt: Long, now: Long, playing: Boolean): Long =
    sampleMs + if (playing) (now - sampledAt).coerceIn(0L, 60L) else 0L
