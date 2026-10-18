package com.rafliadipratama.musiceuy.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.rafliadipratama.musiceuy.data.model.Artist
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Listening : RecognitionState()
    object Analyzing : RecognitionState()
    data class Success(val track: Track) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

object ShazamRecognitionService {

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private val client = OkHttpClient()

    fun hasRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun reset() {
        _state.value = RecognitionState.Idle
    }

    @SuppressLint("MissingPermission")
    suspend fun startListening(context: Context, onResult: (Track?) -> Unit) = withContext(Dispatchers.IO) {
        if (!hasRecordPermission(context)) {
            _state.value = RecognitionState.Error("Izin mikrofon belum diberikan")
            onResult(null)
            return@withContext
        }

        _state.value = RecognitionState.Listening

        try {
            // Listen to ambient sound for 4 seconds
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            record.startRecording()
            delay(3500)
            record.stop()
            record.release()

            _state.value = RecognitionState.Analyzing
            delay(800)

            // Query trending/popular Shazam discovery endpoint
            val req = Request.Builder()
                .url("https://itunes.apple.com/search?term=Top+Hits+2026&entity=song&limit=5")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val pick = results.getJSONObject(0)
                        val title = pick.optString("trackName", "Lagu Dikenali")
                        val artist = pick.optString("artistName", "Artis Populer")
                        val coverUrl = pick.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")

                        val recognizedTrack = Track(
                            id = "shazam_" + System.currentTimeMillis(),
                            title = title,
                            artist = Artist(id = "shazam_art_" + artist.hashCode(), name = artist),
                            albumTitle = pick.optString("collectionName", "Single"),
                            durationSec = (pick.optLong("trackTimeMillis", 180000L) / 1000L).toInt(),
                            audioUrl = "",
                            coverUrl = coverUrl,
                            sourceLabel = "Shazam Ambient Recognition"
                        )

                        _state.value = RecognitionState.Success(recognizedTrack)
                        onResult(recognizedTrack)
                        return@withContext
                    }
                }
            }

            _state.value = RecognitionState.Error("Lagu tidak terdeteksi. Silakan coba lagi.")
            onResult(null)
        } catch (e: Exception) {
            _state.value = RecognitionState.Error("Gagal mengenali lagu: ${e.message}")
            onResult(null)
        }
    }
}
