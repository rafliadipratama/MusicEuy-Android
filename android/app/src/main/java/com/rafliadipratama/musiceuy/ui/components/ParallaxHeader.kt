package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.min

/**
 * Cover image dengan parallax scroll + progressive blur.
 * Saat list di-scroll ke atas, gambar mengecil, memudar, dan blur bertambah.
 * @param coverUrl URL gambar cover
 * @param lazyListState state dari LazyColumn untuk membaca scroll offset
 * @param headerHeight tinggi maksimal header
 * @param overlayColor warna gradient overlay (biasanya background gelap)
 */
@Composable
fun ParallaxCoverHeader(
    coverUrl: String?,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    headerHeight: Dp = 280.dp,
    overlayColor: Color = Color(0xFF000000),
    content: @Composable BoxScope.() -> Unit
) {
    // Menghitung scroll offset dari LazyColumn
    val scrollOffset by remember {
        derivedStateOf {
            val firstVisible = lazyListState.firstVisibleItemIndex
            val firstOffset = lazyListState.firstVisibleItemScrollOffset
            if (firstVisible == 0) firstOffset.toFloat() else Float.MAX_VALUE
        }
    }

    // Normalisasi: 0f = atas (tidak di-scroll), 1f = sudah ter-scroll penuh
    val progress by remember {
        derivedStateOf {
            (scrollOffset / 600f).coerceIn(0f, 1f)
        }
    }

    val imageScale = 1f + (0.1f * (1f - progress)) // sedikit scale down saat scroll
    val imageAlpha = (1f - progress * 1.4f).coerceIn(0f, 1f)
    val blurRadius = (progress * 12f).dp
    val contentAlpha = (1f - progress * 2f).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxWidth().height(headerHeight)) {
        // Background image dengan parallax
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                        alpha = imageAlpha
                        translationY = scrollOffset * -0.3f // efek parallax
                    }
                    .then(if (blurRadius > 0.5.dp) Modifier.blur(blurRadius) else Modifier)
            )
        }

        // Progressive gradient overlay bawah → gelap
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to overlayColor.copy(alpha = 0.1f),
                            0.5f to overlayColor.copy(alpha = 0.3f),
                            1.0f to overlayColor
                        )
                    )
                )
        )

        // Konten header (judul, info, dll) yang juga memudar saat scroll
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(contentAlpha),
            content = content
        )
    }
}
