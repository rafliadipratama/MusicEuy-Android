package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.DarkBackground
import com.rafliadipratama.musiceuy.ui.theme.DarkSurface
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsBottomSheet(
    track: Track,
    isSleepTimerActive: Boolean,
    sleepTimerRemainingSeconds: Long?,
    onDismiss: () -> Unit,
    onStartRadioClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit,
    onShareStoryClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onOpenArtistClick: () -> Unit,
    onNotInterestedClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Pill drag handle
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Track Header Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist.name,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DarkSurface, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Options list
            val sleepTimerSubtitle = if (isSleepTimerActive) {
                if (sleepTimerRemainingSeconds != null) {
                    val mins = sleepTimerRemainingSeconds / 60
                    val secs = sleepTimerRemainingSeconds % 60
                    "Aktif • Tersisa %02d:%02d".format(mins, secs)
                } else {
                    "Aktif • Berhenti di akhir lagu"
                }
            } else {
                "Atur timer otomatis berhenti putar lagu"
            }

            OptionItem(
                icon = Icons.Default.AllInclusive,
                title = "Mulai Radio Lagu",
                subtitle = "Putar otomatis lagu-lagu dengan vibe dan artis serupa",
                isHighlight = true,
                onClick = {
                    onDismiss()
                    onStartRadioClick()
                }
            )

            OptionItem(
                icon = Icons.Default.Bedtime,
                title = "Pengatur Waktu Tidur (Sleep Timer)",
                subtitle = sleepTimerSubtitle,
                isHighlight = isSleepTimerActive,
                onClick = {
                    onDismiss()
                    onSleepTimerClick()
                }
            )

            OptionItem(
                icon = Icons.Default.Share,
                title = "Bagikan Story Card",
                subtitle = "Format vertikal 9:16 untuk IG / WhatsApp Story",
                onClick = {
                    onDismiss()
                    onShareStoryClick()
                }
            )

            OptionItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = "Tambah ke Playlist",
                subtitle = "Simpan ke daftar putar favoritmu",
                onClick = {
                    onDismiss()
                    onAddToPlaylistClick()
                }
            )

            OptionItem(
                icon = Icons.Default.GraphicEq,
                title = "Equalizer & Efek Suara",
                subtitle = "Sesuaikan 5-band EQ, Bass Boost, & Kualitas Bitrate",
                onClick = {
                    onDismiss()
                    onEqualizerClick()
                }
            )

            OptionItem(
                icon = Icons.Default.Person,
                title = "Buka Profil Artis",
                subtitle = "Jelajahi lagu populer & album ${track.artist.name}",
                onClick = {
                    onDismiss()
                    onOpenArtistClick()
                }
            )

            OptionItem(
                icon = Icons.Default.Block,
                title = "Tidak Tertarik dengan Lagu Ini",
                subtitle = "Sembunyikan lagu & kurangi rekomendasi ${track.artist.name}",
                onClick = {
                    onDismiss()
                    onNotInterestedClick()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isHighlight) AccentEmerald.copy(alpha = 0.18f) else DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) AccentEmerald else TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isHighlight) AccentEmerald else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
