package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald
import com.rafliadipratama.musiceuy.ui.theme.DarkBackground
import com.rafliadipratama.musiceuy.ui.theme.DarkSurface
import com.rafliadipratama.musiceuy.ui.theme.TextMuted
import com.rafliadipratama.musiceuy.ui.theme.TextPrimary
import com.rafliadipratama.musiceuy.ui.theme.TextSecondary

@Composable
fun SleepTimerBottomSheet(
    remainingSeconds: Long?,
    isEndOfTrack: Boolean,
    onSetTimerMinutes: (Int) -> Unit,
    onSetEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTimerActive = remainingSeconds != null || isEndOfTrack

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(DarkBackground)
            .padding(top = 12.dp, bottom = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pengatur Waktu Tidur",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Musik akan berhenti otomatis saat timer habis",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isTimerActive) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Status Timer",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val statusText = if (isEndOfTrack) {
                                "Berhenti di akhir lagu ini"
                            } else {
                                val s = remainingSeconds ?: 0L
                                val mins = s / 60
                                val secs = s % 60
                                "Berhenti dalam %02d:%02d".format(mins, secs)
                            }
                            Text(
                                text = statusText,
                                color = AccentEmerald,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCancelTimer() }
                                .background(Color(0xFFE53935).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TimerOff,
                                contentDescription = "Batalkan",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Matikan",
                                color = Color(0xFFEF5350),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Timer options
            TimerOptionItem(
                icon = Icons.Default.Schedule,
                label = "15 Menit",
                isSelected = remainingSeconds != null && remainingSeconds in (14 * 60)..(15 * 60),
                onClick = { onSetTimerMinutes(15) }
            )
            TimerOptionItem(
                icon = Icons.Default.Schedule,
                label = "30 Menit",
                isSelected = remainingSeconds != null && remainingSeconds in (29 * 60)..(30 * 60),
                onClick = { onSetTimerMinutes(30) }
            )
            TimerOptionItem(
                icon = Icons.Default.Schedule,
                label = "45 Menit",
                isSelected = remainingSeconds != null && remainingSeconds in (44 * 60)..(45 * 60),
                onClick = { onSetTimerMinutes(45) }
            )
            TimerOptionItem(
                icon = Icons.Default.Schedule,
                label = "60 Menit (1 Jam)",
                isSelected = remainingSeconds != null && remainingSeconds in (59 * 60)..(60 * 60),
                onClick = { onSetTimerMinutes(60) }
            )
            TimerOptionItem(
                icon = Icons.Default.MusicNote,
                label = "Akhir Lagu Ini",
                isSelected = isEndOfTrack,
                onClick = { onSetEndOfTrack() }
            )
        }
    }
}

@Composable
private fun TimerOptionItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) AccentEmerald else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = if (isSelected) AccentEmerald else TextPrimary,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Dipilih",
                tint = AccentEmerald,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
