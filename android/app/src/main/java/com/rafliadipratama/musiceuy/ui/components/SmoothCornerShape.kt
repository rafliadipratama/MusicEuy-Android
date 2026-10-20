package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Superellipse continuous squircle shape inspired by Apple iOS design.
 * Meets straight edges with zero change of curvature for an ultra-smooth silhouette.
 */
@Immutable
data class SmoothCornerShape(
    val radius: Dp = 16.dp,
    val exponent: Float = 4f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        if (r <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        return Outline.Generic(
            Path().apply { addSmoothRoundedRect(size.width, size.height, r, exponent.toDouble()) }
        )
    }
}

private const val CornerSteps = 12

private fun Path.addSmoothRoundedRect(w: Float, h: Float, r: Float, exponent: Double) {
    val power = 2.0 / exponent

    fun along(t: Double): Float = (r * abs(cos(t)).pow(power)).toFloat()
    fun across(t: Double): Float = (r * abs(sin(t)).pow(power)).toFloat()

    moveTo(r, 0f)
    lineTo(w - r, 0f)

    // Top Right
    for (i in 1..CornerSteps) {
        val t = PI / 2 * i / CornerSteps
        lineTo(w - r + across(t), r - along(t))
    }
    lineTo(w, h - r)

    // Bottom Right
    for (i in 1..CornerSteps) {
        val t = PI / 2 * i / CornerSteps
        lineTo(w - r + along(t), h - r + across(t))
    }
    lineTo(r, h)

    // Bottom Left
    for (i in 1..CornerSteps) {
        val t = PI / 2 * i / CornerSteps
        lineTo(r - across(t), h - r + along(t))
    }
    lineTo(0f, r)

    // Top Left
    for (i in 1..CornerSteps) {
        val t = PI / 2 * i / CornerSteps
        lineTo(r - along(t), r - across(t))
    }
    close()
}
