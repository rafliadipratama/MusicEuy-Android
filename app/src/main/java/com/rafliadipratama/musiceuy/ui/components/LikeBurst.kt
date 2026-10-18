package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay

private val LikeParticleColors = listOf(
    Color(0xFFFF2D55), // Primary Vibrant Crimson Pink
    Color(0xFFFF6B8B), // Soft Rose Pink
    Color(0xFFFF8FAB), // Light Blush
    Color(0xFFFFD166)  // Golden Sparkle
)

/**
 * High-performance, tactile like button burst animation.
 * Features:
 * - Prevents unwanted auto-burst on initial composition/load
 * - Disables burst when switching tracks/keys (e.g. Next/Previous)
 * - Elastic spring bounce on the heart icon itself (1.0 -> 1.38 -> 1.0)
 * - Multi-layer particle explosion with shockwave ring and dual-ring sparkles
 * - Subtle deflate bounce on unlike without particles
 */
@Composable
fun LikeBurst(
    trigger: Boolean,
    modifier: Modifier = Modifier.size(42.dp),
    key: Any? = null,
    burstColors: List<Color> = LikeParticleColors,
    content: @Composable () -> Unit
) {
    val burstProgress = remember { Animatable(0f) }
    val iconScale = remember { Animatable(1f) }
    var hasInitiallyComposed by remember { mutableStateOf(false) }
    var previousTrigger by remember { mutableStateOf(trigger) }
    var previousKey by remember { mutableStateOf(key) }

    LaunchedEffect(key, trigger) {
        if (!hasInitiallyComposed) {
            hasInitiallyComposed = true
            previousTrigger = trigger
            previousKey = key
            return@LaunchedEffect
        }

        // When track/key changes, sync states immediately without firing animations
        if (key != previousKey) {
            previousKey = key
            previousTrigger = trigger
            burstProgress.snapTo(0f)
            iconScale.snapTo(1f)
            return@LaunchedEffect
        }

        if (trigger && !previousTrigger) {
            // LIKED: Elastic spring bounce + sparkling particle explosion
            previousTrigger = true
            burstProgress.snapTo(0f)

            // 1. Icon pop & bounce
            launch {
                iconScale.animateTo(
                    targetValue = 1.38f,
                    animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing)
                )
                iconScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            // 2. Radial particle explosion
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        } else if (!trigger && previousTrigger) {
            // UNLIKED: Subtle tactile deflate bounce
            previousTrigger = false
            burstProgress.snapTo(0f)
            launch {
                iconScale.animateTo(
                    targetValue = 0.82f,
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                )
                iconScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Heart icon with physical spring pop
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            },
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        // Multi-layered particle burst & shockwave
        val p = burstProgress.value
        if (p in 0.01f..0.99f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = 38.dp.toPx()

                // 1. Expanding shockwave ring that thins and fades out
                if (p < 0.65f) {
                    val ringProgress = p / 0.65f
                    val ringRadius = ringProgress * (maxRadius * 0.72f)
                    val ringAlpha = ((1f - ringProgress) * 0.75f).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0xFFFF2D55).copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = (2.2f * (1f - ringProgress)).dp.toPx())
                    )
                }

                // 2. 12 radial sparkling particles (dual-ring concentric layout)
                val particleCount = 12

                for (i in 0 until particleCount) {
                    val angle = (i * (2f * Math.PI / particleCount)).toFloat()
                    val isOuter = i % 2 == 0
                    val radiusFactor = if (isOuter) 1.0f else 0.72f
                    val currentRadius = p * maxRadius * radiusFactor

                    val color = burstColors[i % burstColors.size]
                    val particleAlpha = ((1f - p) * 1.1f).coerceIn(0f, 1f)
                    val particleSize = ((1f - p) * (if (isOuter) 3.8.dp else 2.6.dp).toPx()).coerceAtLeast(0.4f)

                    val x = center.x + currentRadius * cos(angle)
                    val y = center.y + currentRadius * sin(angle)

                    drawCircle(
                        color = color.copy(alpha = particleAlpha),
                        radius = particleSize,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/**
 * Centered animated floating heart popup for double-tap gesture on album covers.
 */
@Composable
fun DoubleTapHeartOverlay(
    show: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }

    LaunchedEffect(show) {
        if (show) {
            heartScale.snapTo(0.2f)
            heartAlpha.snapTo(1f)
            launch {
                heartScale.animateTo(
                    targetValue = 1.35f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
                heartScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                delay(320)
                heartAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 240, easing = LinearEasing)
                )
                onAnimationEnd()
            }
        }
    }

    if (heartAlpha.value > 0.01f) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF2D55).copy(alpha = heartAlpha.value),
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = heartScale.value
                        scaleY = heartScale.value
                        alpha = heartAlpha.value
                    }
            )
        }
    }
}

