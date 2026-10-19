package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Wavy progress seekbar terinspirasi Musicfy.
 * - Saat isPlaying=true: gelombang hidup mengalir
 * - Saat pause/seek: merata jadi garis lurus mulus (spring)
 * - Warna track aktif: activeColor, track inaktif: inactiveColor
 */
@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    activeColor: Color = Color(0xFF1DB954),
    inactiveColor: Color = Color(0xFF333333),
    trackHeight: Float = 4f,
    waveAmplitude: Float = 6f,
    waveLength: Float = 28f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(value) }
    val displayValue = if (isDragging) dragValue else value

    // Phase offset: menggerakkan gelombang ke kiri setiap frame
    var phaseOffset by remember { mutableFloatStateOf(0f) }

    // Amplitude animated: tinggi gelombang menyusut saat pause/drag
    val targetAmplitude = if (isPlaying && !isDragging) waveAmplitude else 0f
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "amplitude"
    )

    // Loop phase only saat isPlaying
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withFrameMillis {
                phaseOffset = (phaseOffset + 0.06f) % (2f * PI.toFloat())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newVal = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newVal)
                        onValueChangeFinished?.invoke()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            onValueChange(dragValue)
                            onValueChangeFinished?.invoke()
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            dragValue = (change.position.x / size.width).coerceIn(0f, 1f)
                            if (isDragging) onValueChange(dragValue)
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val splitX = w * displayValue

            fun buildWavePath(fromX: Float, toX: Float): Path {
                val p = Path()
                val steps = ((toX - fromX) / 2f).toInt().coerceAtLeast(1)
                p.moveTo(fromX, cy)
                for (i in 0..steps) {
                    val x = fromX + i * 2f
                    val angle = (x / waveLength) * 2f * PI.toFloat() + phaseOffset
                    val y = cy + animatedAmplitude * sin(angle)
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                return p
            }

            // Track inaktif (kanan dari thumb)
            clipRect(left = splitX, right = w) {
                drawPath(
                    path = buildWavePath(0f, w),
                    color = inactiveColor,
                    style = Stroke(width = trackHeight, cap = StrokeCap.Round)
                )
            }

            // Track aktif (kiri dari thumb)
            clipRect(left = 0f, right = splitX) {
                drawPath(
                    path = buildWavePath(0f, w),
                    color = activeColor,
                    style = Stroke(width = trackHeight, cap = StrokeCap.Round)
                )
            }

            // Thumb dot
            drawCircle(
                color = activeColor,
                radius = 7f,
                center = Offset(splitX.coerceIn(7f, w - 7f), cy)
            )
        }
    }
}
