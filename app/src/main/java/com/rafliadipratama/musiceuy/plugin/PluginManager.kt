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

        val ytDlpPlugin = YtDlpPlugin(
            context = context,
            isEnabled = prefs.getBoolean("plugin_enabled_ytdlp_engine", true)
        )

        val soundCloudPlugin = SoundCloudPlugin(
            isEnabled = prefs.getBoolean("plugin_enabled_soundcloud", true)
        )

        val deezerPlugin = DeezerPlugin(
            isEnabled = prefs.getBoolean("plugin_enabled_deezer", true)
        )

        val bandcampPlugin = BandcampJamendoPlugin(
            isEnabled = prefs.getBoolean("plugin_enabled_bandcamp_jamendo", true)
        )

        val saavnPlugin = JioSaavnPlugin(
            streamSource = streamSource,
            isEnabled = prefs.getBoolean("plugin_enabled_jiosaavn", true)
        )

        val lrcLibPlugin = LrcLibPlugin(
            lrcClient = com.rafliadipratama.musiceuy.data.network.LrcLibClient(cacheDir = context.cacheDir),
            isEnabled = prefs.getBoolean("plugin_enabled_lrclib", true)
        )

        val spotifyImporterPlugin = SpotifyImporterPlugin(
            playlistImporter = com.rafliadipratama.musiceuy.data.network.PlaylistImporter(),
            isEnabled = prefs.getBoolean("plugin_enabled_spotify_importer", true)
        )

        val equalizerDspPlugin = EqualizerDspPlugin(
            context = context,
            isEnabled = prefs.getBoolean("plugin_enabled_equalizer_dsp", true)
        )

        val lastFmPlugin = LastFmPlugin(
            context = context,
            isEnabled = prefs.getBoolean("plugin_enabled_lastfm_scrobbler", false)
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

        val appleCanvasPlugin = AppleCanvasPlugin(
            context = context,
            isEnabled = prefs.getBoolean("plugin_enabled_apple_canvas", true)
        )

        val discordRpcPlugin = DiscordRpcPlugin(
            context = context,
            isEnabled = prefs.getBoolean("plugin_enabled_discord_rpc", false)
        )

        val monochromePlugin = MonochromeLosslessPlugin()
        monochromePlugin.isEnabled = prefs.getBoolean("plugin_enabled_monochrome_lossless", true)

        _plugins.value = listOf(
            monochromePlugin,
            ytPlugin,
            ytDlpPlugin,
            appleCanvasPlugin,
            soundCloudPlugin,
            deezerPlugin,
            bandcampPlugin,
            saavnPlugin,
            lrcLibPlugin,
            spotifyImporterPlugin,
            equalizerDspPlugin,
            lastFmPlugin,
            discordRpcPlugin,
            spotubeBridgePlugin
        )
    }

    fun isPluginEnabled(pluginId: String): Boolean {
        return _plugins.value.firstOrNull { it.id == pluginId }?.isEnabled ?: true
    }

    fun getYtDlpPlugin(): YtDlpPlugin? = _plugins.value.filterIsInstance<YtDlpPlugin>().firstOrNull()
    fun getLrcLibPlugin(): LrcLibPlugin? = _plugins.value.filterIsInstance<LrcLibPlugin>().firstOrNull()
    fun getSpotifyImporterPlugin(): SpotifyImporterPlugin? = _plugins.value.filterIsInstance<SpotifyImporterPlugin>().firstOrNull()
    fun getLastFmPlugin(): LastFmPlugin? = _plugins.value.filterIsInstance<LastFmPlugin>().firstOrNull()
    fun getEqualizerDspPlugin(): EqualizerDspPlugin? = _plugins.value.filterIsInstance<EqualizerDspPlugin>().firstOrNull()

    fun togglePlugin(pluginId: String, isEnabled: Boolean) {
        prefs.edit().putBoolean("plugin_enabled_$pluginId", isEnabled).apply()
        _plugins.update { current ->
            current.map { plugin ->
                if (plugin.id == pluginId) {
                    plugin.isEnabled = isEnabled
                    if (plugin is EqualizerDspPlugin) {
                        plugin.toggleEffect(isEnabled)
                    }
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

        // 1. Direct audioUrl if already valid HTTP
        if (track.audioUrl.startsWith("http") && !track.audioUrl.startsWith("yt:") && !track.audioUrl.startsWith("sc:")) {
            return track.audioUrl
        }

        // 2. Route matching track's explicit source prefix
        if (track.id.startsWith("mono_") || track.audioUrl.startsWith("mono_")) {
            val monoPlugin = enabledPlugins.firstOrNull { it.id == "monochrome_lossless" }
            val stream = monoPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream
        }
        if (track.id.startsWith("ytdlp:") || track.audioUrl.startsWith("ytdlp:")) {
            val ytdlpPlugin = enabledPlugins.firstOrNull { it.id == "ytdlp_engine" }
            val stream = ytdlpPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream
        }

        if (track.id.startsWith("sc:") || track.audioUrl.startsWith("sc:") || track.audioUrl.contains("soundcloud.com")) {
            val scPlugin = enabledPlugins.firstOrNull { it.id == "soundcloud" }
            val stream = scPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream
        }

        if (track.id.startsWith("dz:") || track.audioUrl.startsWith("dz:") || track.audioUrl.contains("deezer.com")) {
            val dzPlugin = enabledPlugins.firstOrNull { it.id == "deezer" }
            val stream = dzPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream
        }

        if (track.id.startsWith("bc:") || track.audioUrl.startsWith("bc:") || track.audioUrl.contains("bandcamp.com")) {
            val bcPlugin = enabledPlugins.firstOrNull { it.id == "bandcamp_jamendo" }
            val stream = bcPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream
        }

        if (track.id.startsWith("js:") || track.audioUrl.startsWith("js:")) {
            val saavnPlugin = enabledPlugins.firstOrNull { it.id == "jiosaavn" }
            val stream = saavnPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream
        }

        if (track.id.startsWith("yt:") || track.audioUrl.startsWith("yt:") ||
            track.id.startsWith("sp:") || track.audioUrl.startsWith("sp:") ||
            !track.id.contains(":")) {
            // 1st Priority: NewPipeExtractor (Fast ~200ms)
            val ytPlugin = enabledPlugins.firstOrNull { it.id == "ytmusic" }
            val stream = ytPlugin?.resolveStream(track)
            if (!stream.isNullOrBlank()) return stream

            // 2nd Priority: Automatic resilient fallback to yt-dlp Universal Engine!
            val ytdlpPlugin = enabledPlugins.firstOrNull { it.id == "ytdlp_engine" }
            val ytdlpStream = ytdlpPlugin?.resolveStream(track)
            if (!ytdlpStream.isNullOrBlank()) return ytdlpStream
        }

        // 3. Try all other enabled audio stream plugins in priority order
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
