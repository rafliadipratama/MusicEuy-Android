package com.rafliadipratama.musiceuy.plugin

import android.content.Context
import com.rafliadipratama.musiceuy.audio.AudioEffectManager

/**
 * 10-Band Equalizer & Bass Boost DSP Plugin.
 * Hardware-accelerated audio effects using Android AudioFX.
 * GitHub Repo: https://github.com/fast4x/RiMusic
 */
class EqualizerDspPlugin(
    private val context: Context,
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "equalizer_dsp"
    override val name: String = "10-Band Equalizer & Bass Boost"
    override val description: String = "Digital Signal Processor (DSP), Equalizer multi-band, Bass Boost, dan Virtualizer 3D audio"
    override val version: String = "1.0.0"
    override val author: String = "RiMusic & Android AudioFX"
    override val category: String = "Efek Suara (DSP)"
    override val githubUrl: String = "https://github.com/fast4x/RiMusic"
    override val isBuiltIn: Boolean = true

    fun toggleEffect(enable: Boolean) {
        isEnabled = enable
        AudioEffectManager.toggleEqualizer(context, enable)
    }
}
