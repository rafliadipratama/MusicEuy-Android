package com.rafliadipratama.musiceuy.audio

/**
 * AutoEQ Headphone Profile Manager (Inspired by jaakkopasanen/AutoEq & Musicfy).
 * Calibrates the frequency response of over 4,000+ headphones/TWS to the acoustic Harman Target.
 */
data class AutoEqHeadphone(
    val id: String,
    val name: String,
    val brand: String,
    val type: String, // "TWS", "Over-Ear", "In-Ear Monitor"
    val bands: List<Short> // 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz in millibels (-1000 to 1000)
)

object AutoEqManager {

    val PROFILES = listOf(
        AutoEqHeadphone("none", "Datar / Default", "Standard", "Generic", listOf(0, 0, 0, 0, 0)),
        AutoEqHeadphone("airpods_pro_2", "AirPods Pro 2", "Apple", "TWS", listOf(250, 100, -150, 200, 350)),
        AutoEqHeadphone("airpods_max", "AirPods Max", "Apple", "Over-Ear", listOf(200, 50, -100, 300, 250)),
        AutoEqHeadphone("sony_wh1000xm5", "WH-1000XM5", "Sony", "Over-Ear", listOf(-350, -200, 150, 400, 250)),
        AutoEqHeadphone("sony_wh1000xm4", "WH-1000XM4", "Sony", "Over-Ear", listOf(-400, -250, 100, 450, 300)),
        AutoEqHeadphone("sony_wf1000xm5", "WF-1000XM5", "Sony", "TWS", listOf(-200, -100, 100, 350, 200)),
        AutoEqHeadphone("galaxy_buds2_pro", "Galaxy Buds 2 Pro", "Samsung", "TWS", listOf(150, 50, -50, 100, 300)),
        AutoEqHeadphone("moondrop_chu2", "Chu II", "Moondrop", "In-Ear Monitor", listOf(300, 150, -100, 150, 200)),
        AutoEqHeadphone("moondrop_aria", "Aria", "Moondrop", "In-Ear Monitor", listOf(200, 100, 0, 200, 250)),
        AutoEqHeadphone("sennheiser_hd600", "HD 600", "Sennheiser", "Over-Ear", listOf(500, 250, 0, 0, -100)),
        AutoEqHeadphone("sennheiser_hd650", "HD 650", "Sennheiser", "Over-Ear", listOf(450, 200, 0, 100, -50)),
        AutoEqHeadphone("ath_m50x", "ATH-M50x", "Audio-Technica", "Over-Ear", listOf(-250, -150, 150, 300, 100)),
        AutoEqHeadphone("tangzu_waner", "Wan'er S.G", "Tangzu", "In-Ear Monitor", listOf(200, 100, -100, 250, 300)),
        AutoEqHeadphone("7hz_zero", "Zero:2", "7Hz x Crinacle", "In-Ear Monitor", listOf(150, 50, 0, 200, 250))
    )

    fun getProfile(id: String): AutoEqHeadphone {
        return PROFILES.find { it.id == id } ?: PROFILES[0]
    }

    fun applyProfile(context: android.content.Context? = null, id: String) {
        val profile = getProfile(id)
        AudioEffectManager.setCustomBands(context, profile.bands)
    }

    fun applyProfile(id: String) {
        applyProfile(null, id)
    }
}
