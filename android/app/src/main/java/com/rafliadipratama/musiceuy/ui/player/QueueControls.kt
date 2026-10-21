package com.rafliadipratama.musiceuy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary
import com.rafliadipratama.musiceuy.ui.viewmodel.RepeatMode

@Composable
internal fun QueueModeButtons(
    uiState: com.rafliadipratama.musiceuy.ui.viewmodel.PlayerUiState,
    actions: QueuePlayerActions
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val modes = listOf(
            Triple(Icons.Default.Shuffle, "Acak", uiState.isShuffle),
            Triple(if (uiState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                if (uiState.repeatMode == RepeatMode.ONE) "Ulang satu" else "Ulang", uiState.repeatMode != RepeatMode.OFF),
            Triple(Icons.Default.AllInclusive, "Putar terus", uiState.isAutoplayEnabled)
        )
        modes.forEachIndexed { index, (icon, label, active) ->
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) AccentEmerald.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f))
                    .semantics { selected = active }
                    .clickable(onClickLabel = label) {
                        when (index) {
                            0 -> actions.toggleShuffle()
                            1 -> actions.toggleRepeat()
                            else -> actions.toggleAutoplay()
                        }
                    }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, null, tint = if (active) AccentEmerald else TextSecondary, modifier = Modifier.size(22.dp))
                Text(label, color = if (active) AccentEmerald else TextSecondary, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

internal data class QueuePlayerActions(
    val collapse: () -> Unit,
    val openArtist: () -> Unit,
    val toggleLike: () -> Unit,
    val download: () -> Unit,
    val playTrack: (Track) -> Unit,
    val removeTrack: (String) -> Unit,
    val clearQueue: () -> Unit,
    val reorderUpcoming: (Int, Int) -> Unit,
    val moveToNext: (String) -> Unit,
    val closeQueue: () -> Unit,
    val toggleLyrics: () -> Unit,
    val toggleQueue: () -> Unit,
    val previous: () -> Unit,
    val togglePlayPause: () -> Unit,
    val next: () -> Unit,
    val toggleShuffle: () -> Unit,
    val toggleRepeat: () -> Unit,
    val toggleAutoplay: () -> Unit
)
