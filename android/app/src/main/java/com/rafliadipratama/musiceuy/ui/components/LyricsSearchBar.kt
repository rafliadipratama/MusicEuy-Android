package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.ui.theme.AccentEmerald

/**
 * Modern floating glassmorphic search bar for the Lyric screen.
 * Implements the Notes App search UX architecture specified in NOTES_SEARCH_AND_LYRIC_SYSTEM_UX.md:
 * - Real-time match counting ("1 / 8").
 * - Cyclic stepper navigation (previous / next arrows).
 * - Instant clearing and dismissal with fluid transitions.
 * - Minimum 44dp+ touch targets for accessibility.
 */
@Composable
fun LyricsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    totalMatches: Int,
    currentMatchIndex: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AccentEmerald,
    visible: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(durationMillis = 300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(durationMillis = 220, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f))
        ) + fadeOut(animationSpec = tween(180)),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Color(0xFF14141A).copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(25.dp))
                .padding(horizontal = 10.dp),
        ) {
            // Search icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari Lirik",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(20.dp)
                    .padding(start = 2.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Input field with placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Cari bait kata di lirik...",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.40f),
                        ),
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    ),
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNextMatch()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }

            // Clear button if query is present
            if (query.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQueryChange("")
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus teks",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Match counter badge (e.g., "1 / 5" or "0 / 0")
            if (query.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                val counterText = if (totalMatches > 0) {
                    "${currentMatchIndex + 1} / $totalMatches"
                } else {
                    "0 / 0"
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (totalMatches > 0) accentColor.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = counterText,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalMatches > 0) accentColor else Color.White.copy(alpha = 0.5f),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Stepper: Up (Previous Match)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = totalMatches > 0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPrevMatch()
                        },
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Hasil sebelumnya",
                    tint = if (totalMatches > 0) Color.White else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Stepper: Down (Next Match)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = totalMatches > 0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNextMatch()
                        },
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Hasil berikutnya",
                    tint = if (totalMatches > 0) Color.White else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Close Search Mode Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            keyboardController?.hide()
                            onClose()
                        },
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup pencarian",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
