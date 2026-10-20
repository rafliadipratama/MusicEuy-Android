package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Animated dynamic audio equalizer bars indicating playing status.
 */
@Composable
fun PlayingIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    bars: Int = 4,
    barWidth: Dp = 3.dp,
    spacing: Dp = 2.5.dp,
    maxHeight: Dp = 16.dp
) {
    val levels = remember(bars) { List(bars) { Animatable(0.35f + 0.3f * it / bars.toFloat()) } }

    LaunchedEffect(isPlaying, levels) {
        if (!isPlaying) {
            levels.forEachIndexed { i, level ->
                launch { level.animateTo(0.2f, tween(200)) }
            }
            return@LaunchedEffect
        }

        levels.forEach { level ->
            launch {
                while (isActive) {
                    level.animateTo(
                        targetValue = 0.2f + Random.nextFloat() * 0.8f,
                        animationSpec = tween(
                            durationMillis = 180 + Random.nextInt(160),
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        }
    }

    val totalWidth = barWidth * bars + spacing * (bars - 1)
    Canvas(modifier = modifier.width(totalWidth).height(maxHeight)) {
        val w = barWidth.toPx()
        val gap = spacing.toPx()
        val corner = CornerRadius(w / 2f, w / 2f)

        levels.forEachIndexed { index, level ->
            val h = (size.height * level.value).coerceAtLeast(w)
            val x = index * (w + gap)
            val y = (size.height - h) / 2f // Center vertically

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = corner
            )
        }
    }
}
