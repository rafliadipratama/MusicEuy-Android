package com.rafliadipratama.musiceuy.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.data.network.MusicStreamSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Listening : RecognitionState()
    object Analyzing : RecognitionState()
    data class Success(val track: Track) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

object ShazamRecognitionService {

    private const val TAG = "ShazamRecognition"
    private const val SAMPLE_RATE = 16000
    private const val RECORD_DURATION_MS = 4500L

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val streamSource = MusicStreamSource()

    private var activeJob: Job? = null

    fun hasRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun reset() {
        activeJob?.cancel()
        activeJob = null
        _state.value = RecognitionState.Idle
    }

    @SuppressLint("MissingPermission")
    suspend fun startListening(context: Context, onResult: (Track?) -> Unit) = withContext(Dispatchers.IO) {
        if (!hasRecordPermission(context)) {
            _state.value = RecognitionState.Error("Izin mikrofon diperlukan untuk mendengarkan lagu.")
            onResult(null)
            return@withContext
        }

        activeJob = currentCoroutineContext()[Job]
        _state.value = RecognitionState.Listening

        var audioRecord: AudioRecord? = null
        val audioData = ByteArrayOutputStream()

        try {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = RecognitionState.Error("Gagal menginisialisasi mikrofon.")
                onResult(null)
                return@withContext
            }

            audioRecord.startRecording()
            Log.d(TAG, "Recording started for Shazam recognition ($RECORD_DURATION_MS ms)...")

            val buffer = ByteArray(bufferSize)
            val startTime = System.currentTimeMillis()

            while (isActive && (System.currentTimeMillis() - startTime) < RECORD_DURATION_MS) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    audioData.write(buffer, 0, read)
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Audio recording was cancelled")
            return@withContext
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord error: ${e.message}", e)
            _state.value = RecognitionState.Error("Terjadi kesalahan saat merekam: ${e.localizedMessage}")
            onResult(null)
            return@withContext
        } finally {
            try {
                audioRecord?.stop()
            } catch (_: Exception) {}
            try {
                audioRecord?.release()
            } catch (_: Exception) {}
        }

        val pcmBytes = audioData.toByteArray()
        if (pcmBytes.isEmpty() || !isActive) {
            if (isActive) {
                _state.value = RecognitionState.Error("Tidak ada suara yang terdeteksi.")
                onResult(null)
            }
            return@withContext
        }

        _state.value = RecognitionState.Analyzing
        Log.d(TAG, "Analyzing recorded PCM (${pcmBytes.size} bytes)...")

        try {
            val signature = ShazamSignatureGenerator.fromI16(pcmBytes)
            val sampleDurationMs = (pcmBytes.size / (SAMPLE_RATE * 2.0) * 1000).toLong()

            val recognizedTrack = queryShazam(signature, sampleDurationMs)
            if (recognizedTrack != null && isActive) {
                _state.value = RecognitionState.Success(recognizedTrack)
                onResult(recognizedTrack)
            } else if (isActive) {
                _state.value = RecognitionState.Error("Lagu tidak dikenali. Coba dekatkan smartphone ke speaker.")
                onResult(null)
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Shazam recognition was cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Shazam query error: ${e.message}", e)
            if (isActive) {
                _state.value = RecognitionState.Error("Gagal mencocokkan lagu: ${e.localizedMessage ?: "Koneksi bermasalah"}")
                onResult(null)
            }
        }
    }

    private suspend fun queryShazam(signature: String, sampleDurationMs: Long): Track? = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis() / 1000
        val uuid1 = UUID.randomUUID().toString().uppercase()
        val uuid2 = UUID.randomUUID().toString()

        val jsonPayload = JSONObject().apply {
            put("geolocation", JSONObject().apply {
                put("altitude", Random.nextDouble(100.0, 500.0))
                put("latitude", Random.nextDouble(-90.0, 90.0))
                put("longitude", Random.nextDouble(-180.0, 180.0))
            })
            put("signature", JSONObject().apply {
                put("samplems", sampleDurationMs)
                put("timestamp", timestamp)
                put("uri", signature)
            })
            put("timestamp", timestamp)
            put("timezone", "Asia/Jakarta")
        }

        val url = "https://amp.shazam.com/discovery/v5/en/US/android/-/tag/$uuid1/$uuid2" +
                "?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true&video=v3"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Content-Language", "en_US")
            .header("Content-Type", "application/json; charset=utf-8")
            .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Shazam API HTTP ${response.code}: ${response.message}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)

            val trackObj = root.optJSONObject("track") ?: return@withContext null
            val title = trackObj.optString("title", "").trim()
            val artistName = trackObj.optString("subtitle", "").trim()

            if (title.isBlank()) return@withContext null

            Log.d(TAG, "Shazam recognized: '$title' by '$artistName'")

            val images = trackObj.optJSONObject("images")
            val coverUrl = images?.optString("coverarthq")?.takeIf { it.isNotBlank() }
                ?: images?.optString("coverart")?.takeIf { it.isNotBlank() }
                ?: ""

            // Extract YouTube Video ID from hub options if present
            var youtubeVideoId: String? = null
            val hub = trackObj.optJSONObject("hub")
            val options = hub?.optJSONArray("options")
            if (options != null) {
                for (i in 0 until options.length()) {
                    val opt = options.optJSONObject(i)
                    val actions = opt?.optJSONArray("actions")
                    if (actions != null) {
                        for (j in 0 until actions.length()) {
                            val action = actions.optJSONObject(j)
                            val uri = action?.optString("uri", "") ?: ""
                            if (uri.contains("v=")) {
                                youtubeVideoId = uri.substringAfterLast("v=").take(11)
                                break
                            } else if (uri.contains("youtu.be/")) {
                                youtubeVideoId = uri.substringAfterLast("youtu.be/").take(11)
                                break
                            }
                        }
                    }
                    if (!youtubeVideoId.isNullOrBlank()) break
                }
            }

            // Look up high-fidelity YouTube Music stream in MusicEuy
            val searchQuery = "$title $artistName".trim()
            val ytTracks = try {
                streamSource.searchTracks(searchQuery)
            } catch (e: Exception) {
                Log.w(TAG, "YouTube search failed for '$searchQuery': ${e.message}")
                emptyList()
            }

            val matchedTrack = ytTracks.firstOrNull()
            if (matchedTrack != null) {
                Log.d(TAG, "Resolved to YouTube Music track: ${matchedTrack.title} (${matchedTrack.id})")
                return@withContext matchedTrack
            }

            // Fallback if YouTube search yielded no items
            val finalId = if (!youtubeVideoId.isNullOrBlank()) "yt:$youtubeVideoId" else "shazam_${System.currentTimeMillis()}"
            val finalAudioUrl = if (!youtubeVideoId.isNullOrBlank()) "yt:$youtubeVideoId" else ""

            return@withContext Track(
                id = finalId,
                title = title,
                artist = Artist(id = "shazam_art_${artistName.hashCode()}", name = artistName),
                albumTitle = "Shazam",
                durationSec = 210,
                audioUrl = finalAudioUrl,
                coverUrl = coverUrl
            )
        }
    }
}
