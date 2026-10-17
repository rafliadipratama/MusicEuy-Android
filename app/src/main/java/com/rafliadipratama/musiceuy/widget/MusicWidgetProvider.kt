package com.rafliadipratama.musiceuy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.widget.RemoteViews
import com.rafliadipratama.musiceuy.MainActivity
import com.rafliadipratama.musiceuy.R
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.rafliadipratama.musiceuy.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.rafliadipratama.musiceuy.ACTION_NEXT"
        const val ACTION_PREV = "com.rafliadipratama.musiceuy.ACTION_PREV"

        // State holder to restore widget when recreated
        private var lastTrack: Track? = null
        private var lastIsPlaying: Boolean = false

        // Listener for VM / Service interaction
        var playbackActionHandler: ((action: String) -> Unit)? = null

        private val httpClient = OkHttpClient()

        fun updateWidget(context: Context, track: Track?, isPlaying: Boolean) {
            lastTrack = track
            lastIsPlaying = isPlaying

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MusicWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (widgetIds.isEmpty()) return

            val views = RemoteViews(context.packageName, R.layout.widget_music_player)

            views.setTextViewText(R.id.widget_track_title, track?.title ?: "MusicEuy")
            views.setTextViewText(R.id.widget_artist_name, track?.artist?.name ?: "Sentuh untuk memutar musik")
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            // Setup Intents
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Prev Action
            val prevIntent = Intent(context, MusicWidgetProvider::class.java).apply { action = ACTION_PREV }
            val prevPending = PendingIntent.getBroadcast(
                context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

            // Play/Pause Action
            val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply { action = ACTION_PLAY_PAUSE }
            val playPausePending = PendingIntent.getBroadcast(
                context, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePending)

            // Next Action
            val nextIntent = Intent(context, MusicWidgetProvider::class.java).apply { action = ACTION_NEXT }
            val nextPending = PendingIntent.getBroadcast(
                context, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

            // Load Artwork Asynchronously
            val coverUrl = track?.coverUrl
            if (!coverUrl.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val req = Request.Builder().url(coverUrl).build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bytes = resp.body?.bytes()
                                if (bytes != null) {
                                    val rawBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    val roundedBmp = getRoundedCornerBitmap(rawBmp, 20f, 150, 150)
                                    views.setImageViewBitmap(R.id.widget_cover_image, roundedBmp)
                                    appWidgetManager.updateAppWidget(widgetIds, views)
                                }
                            }
                        }
                    } catch (_: Exception) {
                        views.setImageViewResource(R.id.widget_cover_image, R.drawable.ic_widget_music_default)
                        appWidgetManager.updateAppWidget(widgetIds, views)
                    }
                }
            } else {
                views.setImageViewResource(R.id.widget_cover_image, R.drawable.ic_widget_music_default)
                appWidgetManager.updateAppWidget(widgetIds, views)
            }
        }

        private fun getRoundedCornerBitmap(src: Bitmap, cornerRadius: Float, targetW: Int, targetH: Int): Bitmap {
            val scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true)
            val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }
            val rect = Rect(0, 0, targetW, targetH)
            val rectF = RectF(rect)
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, rect, rect, paint)
            return output
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidget(context, lastTrack, lastIsPlaying)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        when (action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV -> {
                playbackActionHandler?.invoke(action)
            }
        }
    }
}
