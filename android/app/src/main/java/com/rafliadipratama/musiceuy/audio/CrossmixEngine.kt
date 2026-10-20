package com.rafliadipratama.musiceuy.audio

import kotlinx.coroutines.delay

enum class CrossmixStyle(val displayName: String, val description: String) {
    OFF("Nonaktif", "Peralihan lagu standar tanpa efek"),
    SMOOTH("Smooth Crossfade", "Transisi volume melandai halus antar dua lagu"),
    SPIN_BACK("Vinyl Spin-Back", "Efek putar mundur piringan hitam ala DJ"),
    TAPE_STOP("Tape Stop", "Efek kaset analog mendadak berhenti"),
    FILTER_SWEEP("Filter Sweep", "Penyaringan frekuensi low-pass sebelum lagu baru masuk")
}

object CrossmixEngine {

    var currentStyle: CrossmixStyle = CrossmixStyle.SMOOTH

    suspend fun executeTransition(
        onFadeOutStep: (Float) -> Unit,
        onPerformCut: () -> Unit,
        onFadeInStep: (Float) -> Unit
    ) {
        when (currentStyle) {
            CrossmixStyle.OFF -> {
                onPerformCut()
            }
            CrossmixStyle.SMOOTH -> {
                // 10 steps of fade out over 500ms
                for (i in 10 downTo 0) {
                    onFadeOutStep(i / 10f)
                    delay(50)
                }
                onPerformCut()
                for (i in 0..10) {
                    onFadeInStep(i / 10f)
                    delay(50)
                }
            }
            CrossmixStyle.SPIN_BACK -> {
                // Quick volume dip and spin-back emulation
                for (i in 5 downTo 0) {
                    onFadeOutStep(i / 5f)
                    delay(40)
                }
                onPerformCut()
                delay(60)
                for (i in 0..5) {
                    onFadeInStep(i / 5f)
                    delay(40)
                }
            }
            CrossmixStyle.TAPE_STOP -> {
                for (i in 8 downTo 0) {
                    onFadeOutStep(i / 8f)
                    delay(30)
                }
                onPerformCut()
                delay(40)
                onFadeInStep(1f)
            }
            CrossmixStyle.FILTER_SWEEP -> {
                for (i in 8 downTo 0) {
                    onFadeOutStep((i / 8f) * 0.8f + 0.2f)
                    delay(45)
                }
                onPerformCut()
                for (i in 0..8) {
                    onFadeInStep(i / 8f)
                    delay(40)
                }
            }
        }
    }
}
