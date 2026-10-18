package com.rafliadipratama.musiceuy.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object AppUpdater {

    private val client = OkHttpClient()
    const val CURRENT_VERSION = "v2.5.0"

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/rafliadipratama/MusicEuy-Android/releases/latest")
                .header("User-Agent", "MusicEuy-App")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return@withContext noUpdate()
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")
                val notes = json.optString("body", "Peningkatan performa & perbaikan bug.")

                var apkUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val hasUpdate = tagName.isNotBlank() && tagName != CURRENT_VERSION
                return@withContext UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = tagName.ifBlank { CURRENT_VERSION },
                    releaseNotes = notes,
                    downloadUrl = apkUrl
                )
            }
        } catch (_: Exception) {
            // Silently fallback if offline or API limit reached
        }
        noUpdate()
    }

    private fun noUpdate() = UpdateInfo(
        hasUpdate = false,
        latestVersion = CURRENT_VERSION,
        releaseNotes = "Aplikasi Anda sudah menggunakan versi paling mutakhir.",
        downloadUrl = ""
    )
}
