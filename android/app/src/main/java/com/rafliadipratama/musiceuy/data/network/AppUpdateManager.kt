package com.rafliadipratama.musiceuy.data.network

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.rafliadipratama.musiceuy.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val htmlUrl: String,
    val publishedAt: String = ""
)

class AppUpdateManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val GITHUB_OWNER = "rafliadipratama"
        private const val GITHUB_REPO = "MusicEuy-Android"
        private const val PREFS_NAME = "musiceuy_update_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_update_check_time"
        private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    }

    fun getCurrentVersionName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: BuildConfig.VERSION_NAME
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
            }
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    fun checkForUpdate(isManual: Boolean = false): AppUpdateInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)

        // Jika pengecekan otomatis dan belum lewat 24 jam, lewati untuk menghemat kuota dan API limit
        if (!isManual && (now - lastCheck < ONE_DAY_MS)) {
            return null
        }

        val currentVersion = getCurrentVersionName()
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MusicEuy-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return null
            }

            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            val rawTag = json.optString("tag_name", "").trim()
            val latestVersion = rawTag.removePrefix("v").removePrefix("V")
            val releaseNotes = json.optString("body", "Pembaruan rilis terbaru telah tersedia.").trim()
            val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
            val publishedAt = json.optString("published_at", "")

            // Cari asset APK dari daftar assets rilis GitHub
            var downloadUrl = htmlUrl
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", htmlUrl)
                        break
                    }
                }
            }

            val isUpdateAvailable = isNewerVersion(currentVersion, latestVersion)

            // Catat waktu sukses memeriksa pembaruan
            prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()

            AppUpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                htmlUrl = htmlUrl,
                publishedAt = publishedAt
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Membandingkan semantic versioning (misal: 1.8.1 lebih tinggi dari 1.8.0).
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currParts = current.removePrefix("v").removePrefix("V")
                .split(".")
                .map { part -> part.filter { it.isDigit() }.toIntOrNull() ?: 0 }
            val latestParts = latest.removePrefix("v").removePrefix("V")
                .split(".")
                .map { part -> part.filter { it.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(currParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val c = currParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
