package com.rafliadipratama.musiceuy.data.local

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object MusicCacheManager {
    private const val CACHE_SIZE = 500L * 1024L * 1024L // 500 MB
    private var simpleCache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "exoplayer_music_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider!!)
        }
        return simpleCache!!
    }

    fun createCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val okHttpDataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15")

        val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, okHttpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun getCacheSizeBytes(context: Context): Long {
        val cacheDir = File(context.cacheDir, "exoplayer_music_cache")
        return simpleCache?.cacheSpace ?: getFolderSize(cacheDir)
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (!file.isDirectory) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFolderSize(it) }
        return size
    }

    fun clearCache(context: Context): Boolean {
        return try {
            val cache = getCache(context)
            val keys = cache.keys.toList()
            for (key in keys) {
                try {
                    cache.removeResource(key)
                } catch (_: Exception) {}
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("MusicEuy", "Error clearing cache: ${e.message}")
            false
        }
    }
}

