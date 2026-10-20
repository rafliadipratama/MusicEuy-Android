package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Warna shimmer OLED dark */
private val ShimmerBase  = Color(0xFF1A1A1A)
private val ShimmerShine = Color(0xFF2E2E2E)
private val ShimmerHighlight = Color(0xFF3D3D3D)

/**
 * Modifier untuk efek shimmer berkilau dari kiri ke kanan.
 * Gunakan sebagai: Box(modifier = Modifier.shimmerEffect())
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -1000f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return this.background(
        Brush.linearGradient(
            colors = listOf(ShimmerBase, ShimmerShine, ShimmerHighlight, ShimmerShine, ShimmerBase),
            start = Offset(translateX, 0f),
            end   = Offset(translateX + 600f, 600f)
        )
    )
}

/** Skeleton satu baris lagu (thumbnail + dua teks) */
@Composable
fun TrackItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // Thumbnail skeleton squircle
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(SmoothCornerShape(10.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Title skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(14.dp)
                    .clip(SmoothCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Artist skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(11.dp)
                    .clip(SmoothCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
    }
}

/** Skeleton satu kartu album/playlist (thumbnail + teks bawah) */
@Composable
fun AlbumCardSkeleton(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    Column(
        modifier = modifier.width(size),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(SmoothCornerShape(14.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(SmoothCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(10.dp)
                .clip(SmoothCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

/** Kumpulan skeleton list untuk loading state */
@Composable
fun TrackListSkeleton(
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        repeat(count) {
            TrackItemSkeleton()
        }
    }
}

/** Kumpulan skeleton kartu horizontal untuk loading state */
@Composable
fun AlbumCarouselSkeleton(
    count: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            AlbumCardSkeleton()
        }
    }
}
