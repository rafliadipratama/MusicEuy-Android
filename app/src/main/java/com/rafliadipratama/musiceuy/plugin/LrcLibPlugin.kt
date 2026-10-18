package com.rafliadipratama.musiceuy.plugin

import com.rafliadipratama.musiceuy.data.model.Lyrics
import com.rafliadipratama.musiceuy.data.network.LrcLibClient

/**
 * LrcLib Plugin - Synchronized / Karaoke Lyrics provider.
 * Uses the free, open-source LRCLIB API.
 * GitHub Repo: https://github.com/boc-the-developer/lrclib
 */
class LrcLibPlugin(
    private val lrcClient: LrcLibClient,
    override var isEnabled: Boolean = true
) : MusicStreamPlugin {

    override val id: String = "lrclib"
    override val name: String = "LRCLIB Synced Lyrics"
    override val description: String = "Lirik tersinkronisasi otomatis (Karaoke synced scroll) dari basis data publik global & Indonesia"
    override val version: String = "1.1.0"
    override val author: String = "boc-the-developer"
    override val category: String = "Lirik & Teks"
    override val githubUrl: String = "https://github.com/boc-the-developer/lrclib"
    override val isBuiltIn: Boolean = true

    suspend fun fetchLyrics(artist: String, title: String): Lyrics {
        if (!isEnabled) return Lyrics()
        return lrcClient.fetchLyrics(artist, title)
    }
}
