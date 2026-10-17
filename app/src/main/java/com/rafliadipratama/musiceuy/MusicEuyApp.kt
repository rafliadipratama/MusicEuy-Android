package com.rafliadipratama.musiceuy

import android.app.Application
import com.rafliadipratama.musiceuy.data.network.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe

class MusicEuyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            NewPipe.init(NewPipeDownloader.create())
        } catch (_: Exception) {
            // NewPipe init fallback
        }
    }
}
