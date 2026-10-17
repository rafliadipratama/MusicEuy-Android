package com.rafliadipratama.musiceuy.plugin

import android.content.Context
import com.rafliadipratama.musiceuy.data.model.Track
import com.rafliadipratama.musiceuy.data.network.MusicStreamSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PluginManager(
    private val context: Context,
    private val streamSource: MusicStreamSource = MusicStreamSource()
) {

    private val prefs = context.getSharedPreferences("musiceuy_plugins", Context.MODE_PRIVATE)

    private val _plugins = MutableStateFlow<List<MusicStreamPlugin>>(emptyList())
    val plugins: StateFlow<List<MusicStreamPlugin>> = _plugins.asStateFlow()

    init {
        loadPlugins()
    }

    private fun loadPlugins() {
        val ytPlugin = YouTubeMusicPlugin(
            streamSource = streamSource,
            isEnabled = prefs.getBoolean("plugin_enabled_ytmusic", true)
        )

        val saavnPlugin = JioSaavnPlugin(
            streamSource = streamSource,
            isEnabled = prefs.getBoolean("plugin_enabled_jiosaavn", true)
        )

        val spotubeBridgePlugin = CustomScriptPlugin(
            id = "spotube_bridge",
            name = "Spotube Bridge Resolver",
            description = "Protokol resolver kompatibel format Spotube .smplug untuk sinkronisasi audio eksternal",
            version = "1.0.1",
            author = "Audion & Spotube Spec",
            endpointUrl = "",
            isEnabled = prefs.getBoolean("plugin_enabled_spotube_bridge", false),
            isBuiltIn = true
        )

        _plugins.value = listOf(ytPlugin, saavnPlugin, spotubeBridgePlugin)
    }

    fun togglePlugin(pluginId: String, isEnabled: Boolean) {
        prefs.edit().putBoolean("plugin_enabled_$pluginId", isEnabled).apply()
        _plugins.update { current ->
            current.map { plugin ->
                if (plugin.id == pluginId) {
                    plugin.isEnabled = isEnabled
                }
                plugin
            }
        }
    }

    fun registerCustomPlugin(
        name: String,
        description: String,
        endpointUrl: String
    ): CustomScriptPlugin {
        val id = "custom_" + System.currentTimeMillis()
        val customPlugin = CustomScriptPlugin(
            id = id,
            name = name,
            description = description,
            version = "1.0.0",
            author = "Kustom",
            endpointUrl = endpointUrl,
            isEnabled = true,
            isBuiltIn = false
        )
        prefs.edit().putBoolean("plugin_enabled_$id", true).apply()
        _plugins.update { it + customPlugin }
        return customPlugin
    }

    /**
     * Resolves a playable direct stream URL using the first enabled plugin that can resolve it.
     */
    suspend fun resolveStream(track: Track): String? {
        val enabledPlugins = _plugins.value.filter { it.isEnabled }

        // 1. Try plugin matching track's prefix
        if (track.id.startsWith("yt:") || track.audioUrl.startsWith("yt:") ||
            track.id.startsWith("sp:") || track.audioUrl.startsWith("sp:")) {
            val ytPlugin = enabledPlugins.firstOrNull { it.id == "ytmusic" }
            val stream = ytPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream
        }

        // 2. Try all other enabled plugins in priority order
        for (plugin in enabledPlugins) {
            val stream = plugin.resolveStream(track)
            if (!stream.isNullOrBlank() && stream.startsWith("http")) {
                return stream
            }
        }

        return null
    }

    /**
     * Search across enabled plugins, prioritizing primary resolvers.
     */
    suspend fun search(query: String): List<Track> {
        val enabledPlugins = _plugins.value.filter { it.isEnabled }

        for (plugin in enabledPlugins) {
            val results = plugin.search(query)
            if (results.isNotEmpty()) {
                return results
            }
        }

        return emptyList()
    }
}
