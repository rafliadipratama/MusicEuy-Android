package com.rafliadipratama.musiceuy.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

enum class AppLanguage(val code: String, val title: String, val subtitle: String) {
    SYSTEM("system", "Ikuti Sistem HP", "Follow System Language"),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian"),
    ENGLISH("en", "English", "Global English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

object LanguageHelper {
    private const val PREFS_NAME = "musiceuy_language_prefs"
    private const val KEY_LANGUAGE = "selected_language_code"

    fun getSavedLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "system") ?: "system"
    }

    fun saveLanguageCode(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
    }

    fun applyLanguage(context: Context, languageCode: String) {
        saveLanguageCode(context, languageCode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (languageCode == "system") {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
            }
        } else {
            val locale = if (languageCode == "system") {
                context.resources.configuration.locales[0] ?: Locale.getDefault()
            } else {
                Locale(languageCode)
            }
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    fun getLocalizedContext(context: Context, languageCode: String): Context {
        if (languageCode == "system") {
            return context
        }
        val locale = Locale(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
