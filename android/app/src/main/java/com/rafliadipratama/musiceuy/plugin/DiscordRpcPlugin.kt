package com.rafliadipratama.musiceuy.plugin

import android.content.Context
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Discord Rich Presence Plugin powered by Kizzy Gateway Protocol.
 * Broadcasts currently playing tracks on MusicEuy directly to the user's Discord profile.
 * Lineage & Codebase Reference: https://github.com/realidkroo/musicfy (providers/kizzy)
 */
class DiscordRpcPlugin(
    private val context: Context,
    override var isEnabled: Boolean = false
) : MusicStreamPlugin {

    override val id: String = "discord_rpc"
    override val name: String = "Discord Rich Presence (Kizzy)"
    override val description: String = "Tampilkan lagu yang sedang kamu dengarkan di profil Discord secara realtime"
    override val version: String = "1.0.0"
    override val author: String = "Kizzy Open Spec & Musicfy"
    override val category: String = "Integrasi & Sosial"
    override val githubUrl: String = "https://github.com/realidkroo/musicfy"
    override val isBuiltIn: Boolean = true

    private val prefs = context.getSharedPreferences("musiceuy_discord_rpc", Context.MODE_PRIVATE)
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    var userToken: String
        get() = prefs.getString("discord_token", "") ?: ""
        set(value) = prefs.edit().putString("discord_token", value.trim()).apply()

    fun updatePresence(track: Track, positionMs: Long = 0L) {
        if (!isEnabled || userToken.isBlank()) return

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // Send Gateway Presence update
                val now = System.currentTimeMillis()
                val startTime = now - positionMs

                val activity = JSONObject().apply {
                    put("name", "MusicEuy")
                    put("type", 2) // 2 = Listening to
                    put("details", track.title)
                    put("state", "by ${track.artist.name}")
                    put("timestamps", JSONObject().apply {
                        put("start", startTime)
                        if (track.durationSec > 0) {
                            put("end", startTime + (track.durationSec * 1000L))
                        }
                    })
                    put("assets", JSONObject().apply {
                        put("large_image", track.coverUrl)
                        put("large_text", track.albumTitle ?: track.title)
                        put("small_image", "musiceuy_logo")
                        put("small_text", "MusicEuy Android")
                    })
                }

                // If websocket is null or closed, connect
                if (webSocket == null) {
                    connectWebSocket(activity)
                } else {
                    sendPresencePayload(activity)
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicEuy", "Error updating Discord Rich Presence: ${e.message}")
            }
        }
    }

    private fun connectWebSocket(initialActivity: JSONObject) {
        val request = Request.Builder()
            .url("wss://gateway.discord.gg/?v=10&encoding=json")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                sendIdentify(ws)
                sendPresencePayload(initialActivity, ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                // Handle Discord gateway heartbeat & ACK
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                webSocket = null
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                webSocket = null
            }
        })
    }

    private fun sendIdentify(ws: WebSocket) {
        val identifyPayload = JSONObject().apply {
            put("op", 2)
            put("d", JSONObject().apply {
                put("token", userToken)
                put("properties", JSONObject().apply {
                    put("os", "Android")
                    put("browser", "MusicEuy Android")
                    put("device", "MusicEuy Player")
                })
            })
        }
        ws.send(identifyPayload.toString())
    }

    private fun sendPresencePayload(activity: JSONObject, ws: WebSocket? = webSocket) {
        val presence = JSONObject().apply {
            put("op", 3)
            put("d", JSONObject().apply {
                put("since", System.currentTimeMillis())
                put("activities", JSONArray().put(activity))
                put("status", "online")
                put("afk", false)
            })
        }
        ws?.send(presence.toString())
    }

    fun clearPresence() {
        try {
            webSocket?.close(1000, "Playback stopped")
            webSocket = null
        } catch (_: Exception) {}
    }
}
