package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Tactile touch feedback providing spring compression when pressed
 * and bouncy overshoot restoration on release (Apple Music / musicfy style).
 */
@Composable
fun Modifier.pressBounce(
    interactionSource: InteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = 0.94f
): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interactionSource, pressedScale) {
        var pressed = 0
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressed++
                    launch {
                        scale.animateTo(
                            targetValue = pressedScale,
                            animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)
                        )
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    pressed = (pressed - 1).coerceAtLeast(0)
                    if (pressed == 0) {
                        launch {
                            if (scale.value > pressedScale + 0.02f) {
                                scale.animateTo(pressedScale, tween(60))
                            }
                            scale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = 0.42f, stiffness = 600f)
                            )
                        }
                    }
                }
            }
        }
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

private const val DefaultMaxTilt = 7f
private const val DefaultPressedScale = 0.96f
private val TiltSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 420f,
)

/**
 * 3D perspective tilt & scale response to touch coordinates.
 * Gives rich tactile depth to cards and tiles like Apple TV and musicfy.
 */
fun Modifier.press3D(
    maxTilt: Float = DefaultMaxTilt,
    pressedScale: Float = DefaultPressedScale,
    enabled: Boolean = true,
    origin: TransformOrigin = TransformOrigin.Center
): Modifier = composed {
    if (!enabled) return@composed this

    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    var normalized by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent(PointerEventPass.Initial)
                        .changes.firstOrNull { it.pressed } ?: continue

                    val bounds = this.size
                    if (bounds.width > 0 && bounds.height > 0) {
                        val p = down.position
                        normalized = Offset(
                            x = ((p.x / bounds.width) * 2f - 1f).coerceIn(-1f, 1f),
                            y = ((p.y / bounds.height) * 2f - 1f).coerceIn(-1f, 1f)
                        )
                        size = bounds
                    }
                    scope.launch { press.animateTo(1f, TiltSpring) }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.none { it.pressed }) break
                    }
                    scope.launch { press.animateTo(0f, TiltSpring) }
                }
            }
        }
        .graphicsLayer {
            val t = press.value
            if (t > 0f) {
                val s = 1f - (1f - pressedScale) * t
                scaleX = s
                scaleY = s
                rotationX = -normalized.y * maxTilt * t
                rotationY = normalized.x * maxTilt * t
                cameraDistance = (size.width.takeIf { it > 0 }?.toFloat() ?: 1000f) * 3f
                transformOrigin = origin
            }
        }
}

/** Soft rounded box fade highlight on press without square ripples */
@Immutable
class RoundedHighlight(
    private val cornerRadius: Dp,
    private val spillX: Dp = 0.dp,
    private val spillY: Dp = 0.dp,
    private val color: Color = Color.White.copy(alpha = 0.08f)
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        RoundedHighlightNode(interactionSource, cornerRadius, spillX, spillY, color)

    override fun equals(other: Any?): Boolean =
        other is RoundedHighlight &&
                other.cornerRadius == cornerRadius &&
                other.spillX == spillX &&
                other.spillY == spillY &&
                other.color == color

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + spillX.hashCode()
        result = 31 * result + spillY.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}

private class RoundedHighlightNode(
    private val interactionSource: InteractionSource,
    private val cornerRadius: Dp,
    private val spillX: Dp,
    private val spillY: Dp,
    private val color: Color
) : Modifier.Node(), DrawModifierNode {
    private val strength = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            val presses = ArrayList<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                    else -> return@collect
                }
                if (presses.isNotEmpty()) {
                    launch { strength.animateTo(1f, tween(90)) }
                } else {
                    launch {
                        if (strength.value < 1f) strength.animateTo(1f, tween(70))
                        strength.animateTo(0f, tween(320))
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = strength.value
        if (s > 0f) {
            val dx = spillX.toPx()
            val dy = spillY.toPx()
            drawRoundRect(
                color = color.copy(alpha = color.alpha * s),
                topLeft = Offset(-dx, -dy),
                size = Size(size.width + dx * 2f, size.height + dy * 2f),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
        drawContent()
    }
}

