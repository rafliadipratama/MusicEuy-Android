package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Satu baris lirik dengan efek glow Apple Music.
 * - Baris aktif: teks putih terang, ukuran lebih besar, aura glow memancar
 * - Baris tidak aktif: redup dan lebih kecil
 * - Animasi spring smooth saat berpindah baris
 */
@Composable
fun GlowLyricsLine(
    text: String,
    isActive: Boolean,
    glowColor: Color = Color(0xFF1DB954),
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animasi semua properties dengan spring
    val textAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.28f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "alpha"
    )
    val textSize by animateFloatAsState(
        targetValue = if (isActive) 22f else 17f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "size"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.45f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "glow"
    )
    val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold

    // Pulsing glow animation saat aktif
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 20.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow layer (digambar sebelum teks)
        if (glowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawGlowAura(
                            color = glowColor,
                            alpha = glowAlpha * if (isActive) pulseScale else 1f
                        )
                    }
            )
        }

        Text(
            text = text,
            color = Color.White.copy(alpha = textAlpha),
            fontSize = textSize.sp,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            lineHeight = (textSize * 1.4f).sp
        )
    }
}

/** Menggambar halo cahaya di sekitar baris aktif */
private fun DrawScope.drawGlowAura(color: Color, alpha: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = maxOf(size.width, size.height) * 0.6f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.35f),
                color.copy(alpha = alpha * 0.12f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}
