package com.rafliadipratama.musiceuy.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.rafliadipratama.musiceuy.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object StoryCardGenerator {

    private val client = OkHttpClient()

    /**
     * Generates a 1080x1920 Instagram / WhatsApp Story card bitmap and returns its FileProvider Uri.
     */
    suspend fun generateAndShareStoryCard(context: Context, track: Track) = withContext(Dispatchers.IO) {
        try {
            // Fetch cover bitmap if available
            var coverBmp: Bitmap? = null
            if (track.coverUrl.isNotBlank()) {
                try {
                    val request = Request.Builder().url(track.coverUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null) {
                                coverBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("StoryCardGenerator", "Failed to fetch cover bitmap: ${e.message}")
                }
            }

            val width = 1080
            val height = 1920
            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            // Extract dominant color if available, or use deep obsidian & emerald gradient
            val topColor = coverBmp?.let { bmp ->
                val pixel = bmp.getPixel(bmp.width / 2, bmp.height / 4)
                Color.argb(255, (Color.red(pixel) * 0.45f).toInt(), (Color.green(pixel) * 0.45f).toInt(), (Color.blue(pixel) * 0.45f).toInt())
            } ?: Color.parseColor("#131c31")

            val bottomColor = Color.parseColor("#080c14")

            // 1. Draw Background Gradient
            val bgPaint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, height.toFloat(), topColor, bottomColor, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 2. Draw Top App Branding Pill
            val brandingPaint = Paint().apply {
                color = Color.parseColor("#26FFFFFF")
                style = Paint.Style.FILL
            }
            val pillRect = RectF(width / 2f - 200f, 180f, width / 2f + 200f, 250f)
            canvas.drawRoundRect(pillRect, 35f, 35f, brandingPaint)

            val brandTextPaint = Paint().apply {
                color = Color.parseColor("#10B981") // Emerald
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("SoraFy • Sedang Memutar", width / 2f, 226f, brandTextPaint)

            // 3. Draw Album Cover with Rounded Corners & Subtle Shadow
            val coverSize = 740f
            val coverLeft = (width - coverSize) / 2f
            val coverTop = 380f
            val coverRect = RectF(coverLeft, coverTop, coverLeft + coverSize, coverTop + coverSize)

            // Shadow
            val shadowPaint = Paint().apply {
                color = Color.parseColor("#44000000")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val shadowRect = RectF(coverLeft - 10f, coverTop + 15f, coverLeft + coverSize + 10f, coverTop + coverSize + 30f)
            canvas.drawRoundRect(shadowRect, 48f, 48f, shadowPaint)

            if (coverBmp != null) {
                val roundedCover = getRoundedCornerBitmap(coverBmp!!, 44f, coverSize.toInt(), coverSize.toInt())
                canvas.drawBitmap(roundedCover, coverLeft, coverTop, null)
            } else {
                val placeholderPaint = Paint().apply {
                    color = Color.parseColor("#1F2937")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(coverRect, 44f, 44f, placeholderPaint)
            }

            // 4. Draw Song Title
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 58f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val titleY = coverTop + coverSize + 130f
            val displayTitle = if (track.title.length > 32) track.title.take(30) + "..." else track.title
            canvas.drawText(displayTitle, width / 2f, titleY, titlePaint)

            // 5. Draw Artist Name
            val artistPaint = Paint().apply {
                color = Color.parseColor("#D1D5DB")
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val artistY = titleY + 68f
            canvas.drawText(track.artist.name, width / 2f, artistY, artistPaint)

            // 6. Draw Audio Visualizer Bars
            val barPaint = Paint().apply {
                color = Color.parseColor("#10B981") // Emerald
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val barWidth = 14f
            val barSpacing = 16f
            val barHeights = listOf(24f, 48f, 75f, 52f, 95f, 64f, 110f, 80f, 92f, 68f, 105f, 58f, 78f, 45f, 30f)
            val totalBarsWidth = barHeights.size * barWidth + (barHeights.size - 1) * barSpacing
            var startX = (width - totalBarsWidth) / 2f
            val baseVisualizerY = artistY + 140f

            for (h in barHeights) {
                val barRect = RectF(startX, baseVisualizerY - h / 2f, startX + barWidth, baseVisualizerY + h / 2f)
                canvas.drawRoundRect(barRect, 8f, 8f, barPaint)
                startX += barWidth + barSpacing
            }

            // 7. Footer Tagline
            val footerPaint = Paint().apply {
                color = Color.parseColor("#6B7280")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Dengarkan di SoraFy App", width / 2f, height - 120f, footerPaint)

            // Save to cache directory
            val cardsDir = File(context.cacheDir, "story_cards")
            if (!cardsDir.exists()) cardsDir.mkdirs()
            val file = File(cardsDir, "story_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Launch Share Intent
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "Sedang mendengarkan \"${track.title}\" oleh ${track.artist.name} di SoraFy 🎵")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan ke Story / Teman").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            android.util.Log.e("StoryCardGenerator", "Error generating story card: ${e.message}", e)
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
