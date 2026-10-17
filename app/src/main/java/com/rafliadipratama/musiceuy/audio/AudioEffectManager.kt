package com.rafliadipratama.musiceuy.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioQuality(val title: String, val subtitle: String, val targetBitrateKbps: Int) {
    HEMAT("Hemat Kuota", "64 kbps • Hemat data & cepat buffering", 64),
    STANDAR("Standar", "128 kbps • Kualitas seimbang jernih", 128),
    TINGGI("Kualitas Tinggi", "256 kbps • Audio fidelitas tinggi (HQ)", 256);

    companion object {
        fun fromString(value: String): AudioQuality {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: STANDAR
        }
    }
}

data class AudioEffectState(
    val isEqEnabled: Boolean = false,
    val currentPreset: String = "Datar",
    val bandLevels: List<Short> = listOf(0, 0, 0, 0, 0),
    val minBandLevel: Short = -1000,
    val maxBandLevel: Short = 1000,
    val isBassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0, // 0 to 1000
    val isLoudnessEnabled: Boolean = false,
    val audioQuality: AudioQuality = AudioQuality.STANDAR
)

object AudioEffectManager {
    private const val TAG = "AudioEffectManager"
    private const val PREFS_NAME = "musiceuy_audio_effects"
    private const val KEY_EQ_ENABLED = "eq_enabled"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_BAND_PREFIX = "eq_band_"
    private const val KEY_BASS_ENABLED = "bass_enabled"
    private const val KEY_BASS_STRENGTH = "bass_strength"
    private const val KEY_LOUDNESS_ENABLED = "loudness_enabled"
    private const val KEY_AUDIO_QUALITY = "audio_quality"

    val PRESET_MAP = mapOf(
        "Datar" to listOf<Short>(0, 0, 0, 0, 0),
        "Bass Boost" to listOf<Short>(600, 400, 0, 0, -200),
        "Vokal Jernih" to listOf<Short>(-200, 200, 500, 300, 100),
        "Akustik" to listOf<Short>(300, 200, 100, 300, 400),
        "Elektronik / EDM" to listOf<Short>(500, 300, 0, 200, 500),
        "Rock" to listOf<Short>(400, 200, -100, 200, 500),
        "Pop" to listOf<Short>(-100, 200, 500, 200, -100)
    )

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0

    private val _state = MutableStateFlow(AudioEffectState())
    val state: StateFlow<AudioEffectState> = _state.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun initFromPrefs(context: Context) {
        val prefs = getPrefs(context)
        val eqEnabled = prefs.getBoolean(KEY_EQ_ENABLED, false)
        val preset = prefs.getString(KEY_PRESET, "Datar") ?: "Datar"
        val bassEnabled = prefs.getBoolean(KEY_BASS_ENABLED, false)
        val bassStrength = prefs.getInt(KEY_BASS_STRENGTH, 0).toShort()
        val loudnessEnabled = prefs.getBoolean(KEY_LOUDNESS_ENABLED, false)
        val qualityStr = prefs.getString(KEY_AUDIO_QUALITY, AudioQuality.STANDAR.name) ?: AudioQuality.STANDAR.name
        val quality = AudioQuality.fromString(qualityStr)

        val defaultLevels = PRESET_MAP[preset] ?: listOf<Short>(0, 0, 0, 0, 0)
        val levels = (0 until 5).map { i ->
            prefs.getInt("$KEY_BAND_PREFIX$i", defaultLevels.getOrElse(i) { 0 }.toInt()).toShort()
        }

        _state.value = AudioEffectState(
            isEqEnabled = eqEnabled,
            currentPreset = preset,
            bandLevels = levels,
            isBassBoostEnabled = bassEnabled,
            bassBoostStrength = bassStrength,
            isLoudnessEnabled = loudnessEnabled,
            audioQuality = quality
        )
    }

    fun attachAudioSession(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (currentSessionId == audioSessionId && equalizer != null) return

        currentSessionId = audioSessionId
        releaseEffects()
        initFromPrefs(context)

        try {
            // Setup Equalizer
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEqEnabled
            }
            val numBands = eq.numberOfBands.toInt()
            val minRange = eq.bandLevelRange[0]
            val maxRange = eq.bandLevelRange[1]

            val currentLevels = _state.value.bandLevels.toMutableList()
            for (i in 0 until minOf(numBands, currentLevels.size)) {
                try {
                    val target = currentLevels[i].coerceIn(minRange, maxRange)
                    eq.setBandLevel(i.toShort(), target)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set band $i: ${e.message}")
                }
            }
            equalizer = eq
            _state.value = _state.value.copy(
                minBandLevel = minRange,
                maxBandLevel = maxRange
            )
            Log.d(TAG, "Equalizer attached to session $audioSessionId with $numBands bands")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Equalizer: ${e.message}")
        }

        try {
            // Setup BassBoost
            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.isBassBoostEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength)
                }
            }
            bassBoost = bb
            Log.d(TAG, "BassBoost attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BassBoost: ${e.message}")
        }

        try {
            // Setup LoudnessEnhancer (Normalisasi Volume)
            val le = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(300) // 3dB boost for normalization
                enabled = _state.value.isLoudnessEnabled
            }
            loudnessEnhancer = le
            Log.d(TAG, "LoudnessEnhancer attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LoudnessEnhancer: ${e.message}")
        }
    }

    fun toggleEqualizer(context: Context, enabled: Boolean) {
        equalizer?.enabled = enabled
        getPrefs(context).edit().putBoolean(KEY_EQ_ENABLED, enabled).apply()
        _state.value = _state.value.copy(isEqEnabled = enabled)
    }

    fun setBandLevel(context: Context, bandIndex: Int, level: Short) {
        val eq = equalizer
        val clampedLevel = level.coerceIn(_state.value.minBandLevel, _state.value.maxBandLevel)
        if (eq != null && bandIndex < eq.numberOfBands) {
            try {
                eq.setBandLevel(bandIndex.toShort(), clampedLevel)
            } catch (e: Exception) {
                Log.w(TAG, "setBandLevel failed: ${e.message}")
            }
        }
        val newLevels = _state.value.bandLevels.toMutableList()
        if (bandIndex < newLevels.size) {
            newLevels[bandIndex] = clampedLevel
        }
        getPrefs(context).edit()
            .putInt("$KEY_BAND_PREFIX$bandIndex", clampedLevel.toInt())
            .putString(KEY_PRESET, "Kustom")
            .apply()

        _state.value = _state.value.copy(
            bandLevels = newLevels,
            currentPreset = "Kustom"
        )
    }

    fun applyPreset(context: Context, presetName: String) {
        val levels = PRESET_MAP[presetName] ?: return
        val eq = equalizer
        val prefs = getPrefs(context).edit().putString(KEY_PRESET, presetName)

        val newLevels = levels.toMutableList()
        for (i in 0 until minOf(newLevels.size, (eq?.numberOfBands?.toInt() ?: 5))) {
            val lvl = newLevels[i].coerceIn(_state.value.minBandLevel, _state.value.maxBandLevel)
            newLevels[i] = lvl
            try {
                eq?.setBandLevel(i.toShort(), lvl)
            } catch (_: Exception) {}
            prefs.putInt("$KEY_BAND_PREFIX$i", lvl.toInt())
        }
        prefs.apply()
        _state.value = _state.value.copy(
            currentPreset = presetName,
            bandLevels = newLevels
        )
    }

    fun toggleBassBoost(context: Context, enabled: Boolean) {
        bassBoost?.enabled = enabled
        getPrefs(context).edit().putBoolean(KEY_BASS_ENABLED, enabled).apply()
        _state.value = _state.value.copy(isBassBoostEnabled = enabled)
    }

    fun setBassBoostStrength(context: Context, strength: Short) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            bassBoost?.setStrength(clamped)
        } catch (_: Exception) {}
        getPrefs(context).edit().putInt(KEY_BASS_STRENGTH, clamped.toInt()).apply()
        _state.value = _state.value.copy(bassBoostStrength = clamped)
    }

    fun toggleLoudness(context: Context, enabled: Boolean) {
        loudnessEnhancer?.enabled = enabled
        getPrefs(context).edit().putBoolean(KEY_LOUDNESS_ENABLED, enabled).apply()
        _state.value = _state.value.copy(isLoudnessEnabled = enabled)
    }

    fun setAudioQuality(context: Context, quality: AudioQuality) {
        getPrefs(context).edit().putString(KEY_AUDIO_QUALITY, quality.name).apply()
        _state.value = _state.value.copy(audioQuality = quality)
    }

    fun getAudioQuality(context: Context): AudioQuality {
        val qualityStr = getPrefs(context).getString(KEY_AUDIO_QUALITY, AudioQuality.STANDAR.name) ?: AudioQuality.STANDAR.name
        return AudioQuality.fromString(qualityStr)
    }

    fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
    }
}
