package com.rafliadipratama.musiceuy.data.local

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

sealed class DownloadProgress {
    object Idle : DownloadProgress()
    data class Downloading(val progress: Float) : DownloadProgress()
    data class Completed(val uri: Uri?, val filePath: String) : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}

class MusicDownloadManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    fun isTrackDownloaded(track: Track): Boolean {
        val safeName = getSafeFileName(track)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(safeName)
            val uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MusicEuy")
            File(dir, safeName).exists()
        }
    }

    suspend fun downloadTrack(
        track: Track,
        directAudioUrl: String,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeName = getSafeFileName(track)

            val request = Request.Builder()
                .url(directAudioUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengunduh audio (HTTP ${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Body respons kosong"))
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(track, safeName, inputStream, contentLength, onProgress)
            } else {
                saveToLegacyStorage(track, safeName, inputStream, contentLength, onProgress)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToMediaStore(
        track: Track,
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        onProgress: (Float) -> Unit
    ): Result<String> {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, track.title)
            put(MediaStore.Audio.Media.ARTIST, track.artist.name)
            put(MediaStore.Audio.Media.ALBUM, track.albumTitle ?: "MusicEuy")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MusicEuy")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
            ?: return Result.failure(Exception("Gagal membuat entri penyimpanan MediaStore"))

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                copyStreamWithProgress(inputStream, outputStream, contentLength, onProgress)
            } ?: return Result.failure(Exception("Gagal membuka stream output MediaStore"))

            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            return Result.success("Music/MusicEuy/$fileName")
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return Result.failure(e)
        }
    }

    private fun saveToLegacyStorage(
        track: Track,
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        onProgress: (Float) -> Unit
    ): Result<String> {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MusicEuy")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, fileName)
        FileOutputStream(file).use { outputStream ->
            copyStreamWithProgress(inputStream, outputStream, contentLength, onProgress)
        }

        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("audio/mp4"), null)
        return Result.success(file.absolutePath)
    }

    private fun copyStreamWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (Float) -> Unit
    ) {
        val buffer = ByteArray(8 * 1024)
        var bytesCopied = 0L
        var read: Int

        input.use { inStream ->
            output.use { outStream ->
                while (inStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                    bytesCopied += read
                    if (totalBytes > 0) {
                        val progress = (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }
                outStream.flush()
            }
        }
    }

    private fun getSafeFileName(track: Track): String {
        val raw = "${track.artist.name} - ${track.title}"
        val sanitized = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return "$sanitized.m4a"
    }
}
