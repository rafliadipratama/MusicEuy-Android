package com.rafliadipratama.musiceuy.playlistcover

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ApplePlaylistStyle(val title: String) {
    GRADIENT_TOP("Gradient Typo"),
    GRADIENT_BOTTOM("Gradient Bottom"),
    BLOOM("Bloom Luminous"),
    BACKDROP("Dark Backdrop")
}

object PlaylistCoverGenerator {

    private val PALETTES = listOf(
        listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
        listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
        listOf(Color(0xFF007991), Color(0xFF78FFD6)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)),
        listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
        listOf(Color(0xFFF857A6), Color(0xFFFF5858)),
        listOf(Color(0xFF4776E6), Color(0xFF8E54E9))
    )

    fun getBrushForPlaylist(title: String, style: ApplePlaylistStyle = ApplePlaylistStyle.GRADIENT_TOP): Brush {
        val hash = Math.abs(title.hashCode())
        val palette = PALETTES[hash % PALETTES.size]

        return when (style) {
            ApplePlaylistStyle.GRADIENT_TOP -> Brush.verticalGradient(palette)
            ApplePlaylistStyle.GRADIENT_BOTTOM -> Brush.verticalGradient(palette.reversed())
            ApplePlaylistStyle.BLOOM -> Brush.radialGradient(palette)
            ApplePlaylistStyle.BACKDROP -> Brush.linearGradient(
                colors = listOf(palette[0].copy(alpha = 0.8f), Color(0xFF0D1117))
            )
        }
    }
}
