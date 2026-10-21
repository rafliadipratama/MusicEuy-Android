package com.rafliadipratama.musiceuy.ui.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.ui.components.DoubleTapHeartOverlay
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
internal fun PlayerVideoSurface(
    streamUrl: String?,
    isLoading: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    track: Track,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDoubleTapHeart by remember(track.id) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
            .pointerInput(track.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isLiked) {
                            onToggleLike()
                        }
                        showDoubleTapHeart = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (streamUrl.isNullOrBlank() || isLoading) {
            // Loading video state with cover backdrop
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.35f }
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = AccentEmerald,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Menghubungkan video klip...",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val context = LocalContext.current
            val exoPlayer = remember(streamUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(streamUrl))
                    volume = 0f // Muted so audio foreground service handles music without echo
                    repeatMode = Player.REPEAT_MODE_ALL
                    videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    prepare()
                }
            }

            DisposableEffect(exoPlayer) {
                onDispose {
                    exoPlayer.release()
                }
            }

            LaunchedEffect(exoPlayer, isPlaying) {
                exoPlayer.playWhenReady = isPlaying
            }

            // Keep in sync with audio position
            LaunchedEffect(exoPlayer, currentPositionMs) {
                val duration = exoPlayer.duration
                if (duration > 0) {
                    val target = currentPositionMs % duration
                    if (kotlin.math.abs(exoPlayer.currentPosition - target) > 1000) {
                        exoPlayer.seekTo(target.coerceIn(0, duration))
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        DoubleTapHeartOverlay(
            show = showDoubleTapHeart,
            onAnimationEnd = { showDoubleTapHeart = false }
        )
    }
}
