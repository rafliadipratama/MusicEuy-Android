package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Badge outlined elegan di bawah judul lagu di PlayerScreen.
 * Contoh: "OPUS", "AAC · 256 kbps", "LOSSLESS"
 */
@Composable
fun AudioFormatBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.50f)
) {
    if (text.isBlank()) return
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(20.dp)
            .border(
                width = 1.dp,
                color = color,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 7.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}
