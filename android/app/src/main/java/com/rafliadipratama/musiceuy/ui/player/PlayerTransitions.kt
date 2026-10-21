package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

internal object PlayerMotion {
    val queueMorph = tween<Float>(420, easing = CubicBezierEasing(0.22f, 0f, 0.16f, 1f))
}
