package com.rafliadipratama.musiceuy.data.local

import android.content.Context
import android.content.SharedPreferences

data class UserProfile(
    val name: String = "MusicEuy VIP",
    val avatarUri: String = "",
    val bio: String = "Penikmat musik sejati • Audiophile",
    val cardNumber: String = "EUY-2026-8891",
    val joinedDate: String = "Oktober 2026",
    val audioQualityTier: String = "Hi-Res Lossless FLAC",
    val autoEqModel: String = "None",
    val crossmixMode: String = "Smooth Crossfade",
    val translateLyricsLang: String = "id",
    val interludeDotsEnabled: Boolean = true,
    val poTokenBypassEnabled: Boolean = true
)

class UserProfileManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("musiceuy_user_profile", Context.MODE_PRIVATE)

    fun getProfile(): UserProfile {
        val defaultCardNumber = "EUY-2026-" + (1000 + (Math.random() * 9000).toInt())
        val savedCardNumber = prefs.getString("card_number", null) ?: run {
            prefs.edit().putString("card_number", defaultCardNumber).apply()
            defaultCardNumber
        }

        return UserProfile(
            name = prefs.getString("user_name", "MusicEuy VIP") ?: "MusicEuy VIP",
            avatarUri = prefs.getString("avatar_uri", "") ?: "",
            bio = prefs.getString("user_bio", "Penikmat musik sejati • Audiophile") ?: "Penikmat musik sejati • Audiophile",
            cardNumber = savedCardNumber,
            joinedDate = prefs.getString("joined_date", "Oktober 2026") ?: "Oktober 2026",
            audioQualityTier = prefs.getString("audio_quality_tier", "Hi-Res Lossless FLAC") ?: "Hi-Res Lossless FLAC",
            autoEqModel = prefs.getString("auto_eq_model", "None") ?: "None",
            crossmixMode = prefs.getString("crossmix_mode", "Smooth Crossfade") ?: "Smooth Crossfade",
            translateLyricsLang = prefs.getString("translate_lyrics_lang", "id") ?: "id",
            interludeDotsEnabled = prefs.getBoolean("interlude_dots_enabled", true),
            poTokenBypassEnabled = prefs.getBoolean("po_token_bypass_enabled", true)
        )
    }

    fun updateProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_name", profile.name)
            .putString("avatar_uri", profile.avatarUri)
            .putString("user_bio", profile.bio)
            .putString("audio_quality_tier", profile.audioQualityTier)
            .putString("auto_eq_model", profile.autoEqModel)
            .putString("crossmix_mode", profile.crossmixMode)
            .putString("translate_lyrics_lang", profile.translateLyricsLang)
            .putBoolean("interlude_dots_enabled", profile.interludeDotsEnabled)
            .putBoolean("po_token_bypass_enabled", profile.poTokenBypassEnabled)
            .apply()
    }
}
