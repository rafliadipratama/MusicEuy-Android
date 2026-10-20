package com.rafliadipratama.musiceuy.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BitmapLoader implementation powered by Coil to load and decode album artwork
 * for Android MediaStyle notification and lockscreen media controls.
 */
@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    private fun createFallbackBitmap(): Bitmap = createBitmap(64, 64)

    private fun Bitmap.copyIfNeeded(): Bitmap {
        return if (isRecycled) {
            createFallbackBitmap()
        } else {
            try {
                copy(Bitmap.Config.ARGB_8888, false) ?: createFallbackBitmap()
            } catch (_: Exception) {
                createFallbackBitmap()
            }
        }
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                future.set(bitmap?.copyIfNeeded() ?: createFallbackBitmap())
            } catch (_: Exception) {
                future.set(createFallbackBitmap())
            }
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false)
                    .build()

                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: result.drawable.toBitmap()
                    future.set(bitmap.copyIfNeeded())
                } else {
                    future.set(createFallbackBitmap())
                }
            } catch (_: Exception) {
                future.set(createFallbackBitmap())
            }
        }
        return future
    }
}
