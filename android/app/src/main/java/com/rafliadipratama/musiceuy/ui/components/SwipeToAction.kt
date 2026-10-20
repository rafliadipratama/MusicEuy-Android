package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private val SWIPE_THRESHOLD_DP = 72.dp
private val REVEAL_WIDTH_DP = 64.dp

/** Warna reveal action */
val SwipeQueueColor = Color(0xFF34C759)   // hijau → Queue Next
val SwipeLikeColor  = Color(0xFF9B72F2)   // ungu → Like

/**
 * Membungkus konten dengan gesture geser kiri/kanan:
 * - Geser kanan  → onSwipeRight() (Queue Next, hijau)
 * - Geser kiri   → onSwipeLeft()  (Like, ungu)
 * Setelah ambang batas tercapai: otomatis snap balik + trigger aksi.
 */
@Composable
fun SwipeToActionRow(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    rightIcon: ImageVector,
    leftIcon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.toPx() }
    val revealPx = with(density) { REVEAL_WIDTH_DP.toPx() }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "swipeOffset"
    )

    val isSwipingRight = animatedOffset > 0f
    val isSwipingLeft  = animatedOffset < 0f
    val swipeProgress = abs(animatedOffset) / thresholdPx
    val revealAlpha = (swipeProgress * 2f).coerceIn(0f, 1f)

    val bgColorRight by animateColorAsState(
        targetValue = if (isSwipingRight && swipeProgress >= 1f) SwipeQueueColor else SwipeQueueColor.copy(alpha = 0.7f),
        label = "bgRight"
    )
    val bgColorLeft by animateColorAsState(
        targetValue = if (isSwipingLeft && swipeProgress >= 1f) SwipeLikeColor else SwipeLikeColor.copy(alpha = 0.7f),
        label = "bgLeft"
    )

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Background kanan (Queue)
        if (isSwipingRight) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColorRight)
                    .graphicsLayer { alpha = revealAlpha },
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = rightIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(22.dp)
                )
            }
        }

        // Background kiri (Like)
        if (isSwipingLeft) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColorLeft)
                    .graphicsLayer { alpha = revealAlpha },
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = leftIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(22.dp)
                )
            }
        }

        // Konten utama
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedOffset }
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (!triggered) {
                            offsetX += delta * 0.6f // Sedikit resistance
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (!triggered) {
                            if (offsetX > thresholdPx) {
                                triggered = true
                                onSwipeRight()
                            } else if (offsetX < -thresholdPx) {
                                triggered = true
                                onSwipeLeft()
                            }
                        }
                        scope.launch {
                            offsetX = 0f
                            triggered = false
                        }
                    }
                ),
            content = content
        )
    }
}
