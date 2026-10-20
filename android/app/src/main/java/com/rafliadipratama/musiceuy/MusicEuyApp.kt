package com.rafliadipratama.musiceuy

import android.app.Application
import android.util.Log
import com.rafliadipratama.musiceuy.data.network.NewPipeDownloader
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe

class MusicEuyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            NewPipe.init(NewPipeDownloader.create())
        } catch (_: Exception) {
            // NewPipe init fallback
        }

        // Initialize yt-dlp native Python engine asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                Log.d("MusicEuyApp", "YoutubeDL initialized successfully, version: ${YoutubeDL.getInstance().version(applicationContext)}")
            } catch (e: Exception) {
                Log.e("MusicEuyApp", "Failed to initialize YoutubeDL", e)
            }
        }
    }
}
