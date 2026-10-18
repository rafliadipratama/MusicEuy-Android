package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LikeBurst(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    burstColor: Color = Color(0xFF1ED760),
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        content()

        if (progress.value in 0.01f..0.99f) {
            Canvas(modifier = Modifier.size(70.dp)) {
                val p = progress.value
                val center = Offset(size.width / 2, size.height / 2)
                val particleCount = 8
                val radius = p * (size.minDimension / 2.2f)
                val particleRadius = (1f - p) * 3.5.dp.toPx()

                for (i in 0 until particleCount) {
                    val angle = (i * (2 * Math.PI / particleCount)).toFloat()
                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)
                    drawCircle(
                        color = burstColor.copy(alpha = 1f - p),
                        radius = particleRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
