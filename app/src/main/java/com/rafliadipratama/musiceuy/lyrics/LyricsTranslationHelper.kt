package com.rafliadipratama.musiceuy.lyrics

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

object LyricsTranslationHelper {

    private val client = OkHttpClient()
    private val translationCache = mutableMapOf<String, String>()

    suspend fun translateLine(text: String, targetLang: String = "id"): String = withContext(Dispatchers.IO) {
        if (text.isBlank() || targetLang == "off") return@withContext text
        val key = "$targetLang:$text"
        translationCache[key]?.let { return@withContext it }

        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return@withContext text
                val root = JSONArray(body)
                val parts = root.optJSONArray(0) ?: return@withContext text
                val translatedBuilder = StringBuilder()
                for (i in 0 until parts.length()) {
                    val p = parts.optJSONArray(i)
                    if (p != null) {
                        translatedBuilder.append(p.optString(0, ""))
                    }
                }
                val translated = translatedBuilder.toString().trim()
                if (translated.isNotBlank()) {
                    translationCache[key] = translated
                    return@withContext translated
                }
            }
        } catch (_: Exception) {
            // Silently fallback to original text
        }
        text
    }
}

/**
 * Animated Bouncing Interlude Dots (Adopted from Musicfy).
 * Displays 3 rhythmic bouncing dots during instrumental solos between singing lines.
 */
@Composable
fun LyricsInterludeDots(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.7f)
) {
    val transition = rememberInfiniteTransition(label = "interlude_dots")

    val dot1Scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(dot1Scale)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(dot2Scale)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(dot3Scale)
                .clip(CircleShape)
                .background(color)
        )
    }
}
