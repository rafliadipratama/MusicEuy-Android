package com.rafliadipratama.musiceuy.lyrics

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
