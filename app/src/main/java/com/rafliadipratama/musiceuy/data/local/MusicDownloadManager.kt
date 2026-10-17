package com.rafliadipratama.musiceuy.data.local

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rafliadipratama.musiceuy.data.model.Artist
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

    fun getDownloadedTracks(): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DISPLAY_NAME
                )
                val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("Music/MusicEuy%", "%.m4a")
                val uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                context.contentResolver.query(uri, projection, selection, selectionArgs, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val rawTitle = cursor.getString(titleCol) ?: ""
                        val rawArtist = cursor.getString(artistCol) ?: ""
                        val album = cursor.getString(albumCol) ?: "MusicEuy"
                        val duration = (cursor.getInt(durationCol) / 1000).coerceAtLeast(0)
                        val size = cursor.getLong(sizeCol)
                        val displayName = cursor.getString(nameCol) ?: ""

                        var finalTitle = rawTitle
                        var finalArtist = rawArtist
                        if (finalArtist.isBlank() || finalArtist.equals("<unknown>", ignoreCase = true)) {
                            val cleanName = displayName.removeSuffix(".m4a").removeSuffix(".mp3")
                            if (cleanName.contains(" - ")) {
                                val parts = cleanName.split(" - ", limit = 2)
                                finalArtist = parts[0].trim()
                                finalTitle = parts[1].trim()
                            } else {
                                finalArtist = "MusicEuy Offline"
                                finalTitle = cleanName
                            }
                        }

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        tracks.add(
                            Track(
                                id = "local_$id",
                                title = finalTitle,
                                artist = Artist(id = "artist_$finalArtist", name = finalArtist),
                                albumTitle = album,
                                durationSec = duration,
                                audioUrl = contentUri.toString(),
                                coverUrl = "",
                                isLiked = false,
                                isVerified = true,
                                isDownloaded = true,
                                fileSizeBytes = size,
                                sourceLabel = "Penyimpanan HP (Offline)"
                            )
                        )
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MusicEuy")
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { f -> f.extension.equals("m4a", ignoreCase = true) || f.extension.equals("mp3", ignoreCase = true) }?.forEach { f ->
                        val cleanName = f.nameWithoutExtension
                        var artist = "MusicEuy Offline"
                        var title = cleanName
                        if (cleanName.contains(" - ")) {
                            val parts = cleanName.split(" - ", limit = 2)
                            artist = parts[0].trim()
                            title = parts[1].trim()
                        }
                        tracks.add(
                            Track(
                                id = "local_${f.name.hashCode()}",
                                title = title,
                                artist = Artist(id = "artist_$artist", name = artist),
                                albumTitle = "MusicEuy",
                                durationSec = 0,
                                audioUrl = Uri.fromFile(f).toString(),
                                coverUrl = "",
                                isLiked = false,
                                isVerified = true,
                                isDownloaded = true,
                                fileSizeBytes = f.length(),
                                sourceLabel = "Penyimpanan HP (Offline)"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicDownloadManager", "Error querying downloaded tracks", e)
        }
        return tracks
    }

    fun deleteDownloadedTrack(track: Track): Boolean {
        return try {
            if (track.audioUrl.startsWith("content://")) {
                val uri = Uri.parse(track.audioUrl)
                context.contentResolver.delete(uri, null, null) > 0
            } else if (track.audioUrl.startsWith("file://")) {
                val file = File(Uri.parse(track.audioUrl).path ?: "")
                file.delete()
            } else {
                val safeName = getSafeFileName(track)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(safeName)
                    val uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    context.contentResolver.delete(uri, selection, selectionArgs) > 0
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MusicEuy")
                    File(dir, safeName).delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicDownloadManager", "Error deleting track: ${track.title}", e)
            false
        }
    }

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
            android.util.Log.d("MusicDownloadManager", "Starting download: $safeName from $directAudioUrl")

            val request = Request.Builder()
                .url(directAudioUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                android.util.Log.e("MusicDownloadManager", "Download failed with HTTP ${response.code}")
                return@withContext Result.failure(Exception("Gagal mengunduh audio (HTTP ${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Body respons kosong"))
            val contentLength = body.contentLength()
            android.util.Log.d("MusicDownloadManager", "Content-Length: $contentLength bytes")
            val inputStream = body.byteStream()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(track, safeName, inputStream, contentLength, onProgress)
            } else {
                saveToLegacyStorage(track, safeName, inputStream, contentLength, onProgress)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicDownloadManager", "Exception in downloadTrack", e)
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
            android.util.Log.d("MusicDownloadManager", "Created MediaStore URI: $uri")
            resolver.openOutputStream(uri)?.use { outputStream ->
                copyStreamWithProgress(inputStream, outputStream, contentLength, onProgress)
            } ?: return Result.failure(Exception("Gagal membuka stream output MediaStore"))

            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            val updated = resolver.update(uri, contentValues, null, null)
            android.util.Log.d("MusicDownloadManager", "Cleared IS_PENDING, updated rows: $updated")

            return Result.success("Music/MusicEuy/$fileName")
        } catch (e: Exception) {
            android.util.Log.e("MusicDownloadManager", "Error saving to MediaStore URI: $uri", e)
            try {
                resolver.delete(uri, null, null)
            } catch (delEx: Exception) {
                android.util.Log.e("MusicDownloadManager", "Failed to cleanup URI", delEx)
            }
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
        val buffer = ByteArray(16 * 1024)
        var bytesCopied = 0L
        var read: Int

        input.use { inStream ->
            while (inStream.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                bytesCopied += read
                if (totalBytes > 0) {
                    val progress = (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress)
                }
            }
            output.flush()
        }
    }

    private fun getSafeFileName(track: Track): String {
        val raw = "${track.artist.name} - ${track.title}"
        val sanitized = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return "$sanitized.m4a"
    }
}
