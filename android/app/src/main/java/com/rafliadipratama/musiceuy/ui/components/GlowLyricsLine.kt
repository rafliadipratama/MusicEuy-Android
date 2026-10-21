package com.rafliadipratama.musiceuy.ui.components

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafliadipratama.musiceuy.data.model.SyllableWord
import com.rafliadipratama.musiceuy.lyrics.LyricsAlignment
import com.rafliadipratama.musiceuy.lyrics.LyricsSearchMatch
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

// Keep adjacent lines readable while directing focus to the active line.
private val BlurStageRadii = floatArrayOf(0f, 2f, 5f)
private const val BlurQuantStepPx = 0.5f
private val BlurEffectCache: Array<androidx.compose.ui.graphics.RenderEffect?> =
    arrayOfNulls((BlurStageRadii.max() / BlurQuantStepPx).toInt() + 2)

private fun blurEffectForRadius(radius: Float): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || radius <= 0.05f) return null
    val step = (radius / BlurQuantStepPx).roundToInt().coerceIn(1, BlurEffectCache.lastIndex)
    BlurEffectCache[step]?.let { return it }
    val quantised = step * BlurQuantStepPx
    return android.graphics.RenderEffect
        .createBlurEffect(quantised, quantised, android.graphics.Shader.TileMode.DECAL)
        .asComposeRenderEffect()
        .also { BlurEffectCache[step] = it }
}

private val LyricsFontSize = 26.sp
private val LyricsLineHeight = 36.sp

@Composable
fun GlowLyricsLine(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    words: List<SyllableWord> = emptyList(),
    currentMs: Long = 0L,
    distanceFromActive: Int = 0,
    glowColor: Color = Color(0xFF1DB954),
    alignment: LyricsAlignment = LyricsAlignment.START,
    searchMatch: LyricsSearchMatch? = null,
    isActiveSearchMatch: Boolean = false,
    translation: String? = null,
    onTap: () -> Unit = {},
    positionProvider: (() -> Long)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (pressed) 0.975f else 1f,
        spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "lyricPress"
    )
    val pressAlpha by animateFloatAsState(
        if (pressed) 0.08f else 0f,
        tween(if (pressed) 90 else 240),
        label = "lyricPressHighlight"
    )

    // Map syllable words to character spans in text
    val wordRanges = remember(text, words) {
        var searchFrom = 0
        words.map { word ->
            val trimmed = word.word.trim()
            val start = if (trimmed.isEmpty()) -1 else text.indexOf(trimmed, searchFrom, ignoreCase = true)
            if (start >= 0) {
                val end = start + trimmed.length
                searchFrom = end
                start to end
            } else {
                -1 to -1
            }
        }
    }

    val targetScale = when {
        isActive -> 1.045f
        distanceFromActive <= 1 -> 0.98f
        else -> 0.97f
    }

    val targetAlpha = when {
        isActive || isActiveSearchMatch -> 1f
        searchMatch != null -> 0.88f
        distanceFromActive <= 1 -> 0.58f
        else -> 0.30f
    }

    val targetBlur = when {
        isActive || searchMatch != null -> BlurStageRadii[0]
        distanceFromActive <= 1 -> BlurStageRadii[1]
        else -> BlurStageRadii[2]
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "lyricsScale"
    )

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(
            durationMillis = 400,
            easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        ),
        label = "lyricsAlpha"
    )

    val drop by animateFloatAsState(
        targetValue = if (isActive || words.isNotEmpty()) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "lyricsDrop"
    )

    val blurRadius by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 300),
        label = "lyricsBlur"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.45f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "glowAlpha"
    )

    val lineBorderModifier = if (isActiveSearchMatch) {
        Modifier
            .background(Color(0xFFFFB74D).copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .border(1.5.dp, Color(0xFFFFB74D).copy(alpha = 0.85f), RoundedCornerShape(14.dp))
    } else Modifier

    val textAlign = when (alignment) {
        LyricsAlignment.START -> TextAlign.Start
        LyricsAlignment.CENTER -> TextAlign.Center
        LyricsAlignment.END -> TextAlign.End
    }
    val columnAlign = when (alignment) {
        LyricsAlignment.START -> Alignment.Start
        LyricsAlignment.CENTER -> Alignment.CenterHorizontally
        LyricsAlignment.END -> Alignment.End
    }
    val startPadding = when (alignment) {
        LyricsAlignment.START -> 20.dp
        LyricsAlignment.CENTER -> 20.dp
        LyricsAlignment.END -> 36.dp
    }
    val endPadding = when (alignment) {
        LyricsAlignment.START -> 36.dp
        LyricsAlignment.CENTER -> 20.dp
        LyricsAlignment.END -> 20.dp
    }

    Column(
        horizontalAlignment = columnAlign,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding, top = 2.dp, bottom = 2.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale * pressScale
                scaleY = scale * pressScale
                translationY = drop * 3.dp.toPx()
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    pivotFractionX = when (alignment) {
                        LyricsAlignment.START -> 0f
                        LyricsAlignment.CENTER -> 0.5f
                        LyricsAlignment.END -> 1f
                    },
                    pivotFractionY = 0.5f
                )
                renderEffect = blurEffectForRadius(blurRadius)
            }
    ) {
        Box(
            modifier = Modifier
                .then(lineBorderModifier)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = pressAlpha))
                .clickable(interactionSource = interactionSource, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTap()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Ambient glow backdrop behind active line
            if (glowAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawGlowAura(
                                color = glowColor,
                                alpha = glowAlpha,
                                alignment = alignment
                            )
                        }
                )
            }

            // Build annotated text: search highlights > live character sweep > static text
            val annotatedString = remember(
                text,
                isActive,
                searchMatch,
                isActiveSearchMatch,
                glowColor
            ) {
                if (searchMatch != null && searchMatch.matchRanges.isNotEmpty()) {
                    buildAnnotatedString {
                        var currentIndex = 0
                        val sortedRanges = searchMatch.matchRanges.sortedBy { it.first }

                        for (range in sortedRanges) {
                            if (range.first > currentIndex) {
                                append(text.substring(currentIndex, range.first))
                            }

                            val highlightBg = if (isActiveSearchMatch) {
                                Color(0xFFFFB74D).copy(alpha = 0.55f)
                            } else {
                                Color.White.copy(alpha = 0.28f)
                            }

                            withStyle(
                                style = SpanStyle(
                                    background = highlightBg,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            ) {
                                append(text.substring(range.first, (range.last + 1).coerceAtMost(text.length)))
                            }
                            currentIndex = range.last + 1
                        }
                        if (currentIndex < text.length) {
                            append(text.substring(currentIndex))
                        }
                    }
                } else if (isActive && words.isNotEmpty()) {
                    buildAnnotatedString { append(text) }
                } else {
                    // Inactive line or plain text
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append(text)
                        }
                    }
                }
            }

            var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
            val position = rememberUpdatedState(currentMs)
            val livePosition = rememberUpdatedState(positionProvider)
            val motionModifier = remember(textLayout, text, words, wordRanges, isActive, searchMatch) {
                Modifier.drawWithCache {
                    val layout = textLayout
                    val cells = if (layout == null) emptyList() else text.indices.map { charIndex ->
                        val wordIndex = wordRanges.indexOfFirst {
                            charIndex >= it.first && charIndex < it.second
                        }
                        LyricGlyph(
                            bounds = layout.getBoundingBox(charIndex),
                            wordIndex = wordIndex,
                            characterIndex = charIndex,
                            rtl = layout.getBidiRunDirection(charIndex) == ResolvedTextDirection.Rtl
                        )
                    }
                    val waitDrop = 2.5.dp.toPx()
                    val heldLift = 1.5.dp.toPx()
                    val groups = LinkedHashMap<LyricGlyphPaint, Path>()
                    onDrawWithContent {
                        if (!isActive || words.isEmpty() || searchMatch != null || layout == null) {
                            drawContent()
                        } else {
                            // Merge settled glyphs; only cells around the singing head need separate draws.
                            groups.values.forEach { it.reset() }
                            cells.forEach { cell ->
                                val word = words.getOrNull(cell.wordIndex)
                                val range = wordRanges.getOrNull(cell.wordIndex)
                                val duration = word?.let { (it.endMs - it.startMs).coerceAtLeast(1L) } ?: 1L
                                val progress = if (word == null) 1f else
                                    (((livePosition.value?.invoke() ?: position.value) - word.startMs).toFloat() / duration).coerceIn(0f, 1f)
                                val count = range?.let { it.second - it.first } ?: 1
                                val head = progress * count
                                val local = cell.characterIndex - (range?.first ?: cell.characterIndex)
                                val letterRise = if (progress >= 1f) 1f else smoothLyricStep((head - local + 0.5f) / 2f)
                                val wordRise = smoothLyricStep(progress / 0.85f)
                                val rise = if (word == null) 1f else wordRise * 0.45f + letterRise * 0.55f
                                val held = if (duration >= 900L && progress > 0f && progress < 1f)
                                    sin(PI * progress).toFloat() * smoothLyricStep((head - local + 0.3f) / 1.4f)
                                else 0f
                                val waveDistance = (head - local - 0.5f) / 2f
                                val wave = if (progress > 0f && progress < 1f && kotlin.math.abs(waveDistance) < 1f)
                                    (1f + kotlin.math.cos(PI * waveDistance).toFloat()) * 0.5f *
                                        smoothLyricStep(progress / 0.12f) * smoothLyricStep((1f - progress) / 0.12f)
                                else 0f
                                val dy = ((waitDrop * (1f - rise) - heldLift * (held + wave * 0.65f)) * 2f)
                                    .roundToInt() / 2f
                                val fraction = if (word == null) 1f else (head - local).coerceIn(0f, 1f)
                                val bounds = cell.bounds
                                val paint = LyricGlyphPaint(dy, 0.46f)
                                groups.getOrPut(paint) { Path() }.addRect(bounds)
                                if (fraction > 0f) {
                                    val lit = if (fraction >= 1f) bounds else Rect(
                                        if (cell.rtl) bounds.right - bounds.width * fraction else bounds.left,
                                        bounds.top,
                                        if (cell.rtl) bounds.right else bounds.left + bounds.width * fraction,
                                        bounds.bottom
                                    )
                                    groups.getOrPut(LyricGlyphPaint(dy, 1f)) { Path() }.addRect(lit)
                                }
                            }
                            groups.forEach { (paint, path) ->
                                if (path.isEmpty) return@forEach
                                withTransform({
                                    translate(top = paint.dy)
                                }) {
                                    clipPath(path) {
                                        drawText(layout, color = Color.White.copy(alpha = paint.alpha))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = columnAlign) {
                Text(
                    text = annotatedString,
                    onTextLayout = { textLayout = it },
                    modifier = motionModifier,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = LyricsFontSize,
                    lineHeight = LyricsLineHeight,
                    textAlign = textAlign,
                    letterSpacing = (-0.3).sp,
                    overflow = TextOverflow.Clip
                )

                if (!translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = translation,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = textAlign,
                        color = Color.White.copy(alpha = if (isActive) 0.65f else 0.35f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** Draws soft radial aura for active lyric line, aligned to the singing voice */
private fun DrawScope.drawGlowAura(color: Color, alpha: Float, alignment: LyricsAlignment) {
    val cx = when (alignment) {
        LyricsAlignment.START -> size.width * 0.35f
        LyricsAlignment.CENTER -> size.width * 0.5f
        LyricsAlignment.END -> size.width * 0.65f
    }
    val cy = size.height / 2f
    val radius = maxOf(size.width, size.height) * 0.65f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.40f),
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

private data class LyricGlyph(
    val bounds: Rect,
    val wordIndex: Int,
    val characterIndex: Int,
    val rtl: Boolean
)

private data class LyricGlyphPaint(val dy: Float, val alpha: Float)

private fun smoothLyricStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
