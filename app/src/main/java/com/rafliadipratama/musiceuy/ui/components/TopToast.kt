package com.rafliadipratama.musiceuy.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Data model sebuah toast */
data class TopToastData(
    val message: String,
    val icon: ImageVector? = null,
    val iconTint: Color = Color(0xFF1DB954),
    val durationMs: Long = 2800L
)

/** Singleton global agar bisa dipanggil dari mana saja */
object TopToaster {
    private val _current = MutableStateFlow<TopToastData?>(null)
    val current: StateFlow<TopToastData?> = _current.asStateFlow()

    fun show(
        message: String,
        icon: ImageVector? = null,
        iconTint: Color = Color(0xFF1DB954),
        durationMs: Long = 2800L
    ) {
        _current.value = TopToastData(message, icon, iconTint, durationMs)
    }

    fun dismiss() { _current.value = null }
}

/**
 * Letakkan sekali di root Scaffold, di atas semua konten.
 * Pill hitam turun dari atas dengan spring, swipe-up untuk tutup.
 */
@Composable
fun TopToastHost(modifier: Modifier = Modifier) {
    val toast by TopToaster.current.collectAsState()

    var offsetY by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        if (toast != null) {
            offsetY = 0f
            visible = true
            delay(toast!!.durationMs)
            visible = false
            delay(400)
            TopToaster.dismiss()
        } else {
            visible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f)
            .statusBarsPadding()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = visible to toast,
            transitionSpec = {
                (slideInVertically { -it } + scaleIn(initialScale = 0.88f) + fadeIn()) togetherWith
                (slideOutVertically { -it } + fadeOut()) using SizeTransform(clip = false)
            },
            label = "toptoast"
        ) { (isVisible, data) ->
            if (isVisible && data != null) {
                Row(
                    modifier = Modifier
                        .graphicsLayer { translationY = offsetY }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < 0f) offsetY = (offsetY + dragAmount).coerceAtLeast(-200f)
                                },
                                onDragEnd = {
                                    if (offsetY < -40f) {
                                        TopToaster.dismiss()
                                    } else {
                                        offsetY = 0f
                                    }
                                }
                            )
                        }
                        .background(
                            color = Color(0xFF111111),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (data.icon != null) {
                        Icon(
                            imageVector = data.icon,
                            contentDescription = null,
                            tint = data.iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = data.message,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
