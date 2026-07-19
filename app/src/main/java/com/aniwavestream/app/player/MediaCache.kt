package com.aniwavestream.app.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Global Media3 playback engine.
 *
 *  - A single process-wide [SimpleCache] with an LRU evictor so every episode
 *    shares one on-disk segment store. Scrubbing / rewinding reads cached
 *    segments straight from device storage instead of re-hitting the CDN.
 *  - A [CacheDataSource.Factory] wraps the HTTP upstream so segments are
 *    cached on download and served from cache on replay.
 *  - An aggressive [DefaultLoadControl]: buffers up to 50s ahead and starts
 *    playback after only 2.5s so cellular drops are masked.
 *
 * The cache is intentionally global (lives for the app's lifetime) — only the
 * [ExoPlayer] instance is released per-screen via DisposableEffect.
 */
@UnstableApi
object MediaCache {

    private const val CACHE_DIR = "aniwave_media"
    private const val CACHE_SIZE_BYTES = 500L * 1024 * 1024 // 500 MB LRU window
    private const val MAX_BUFFER_MS = 50_000
    private const val MIN_BUFFER_MS = 30_000
    private const val BUFFER_FOR_PLAYBACK_MS = 2_500
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000

    private val cacheRef = AtomicReference<SimpleCache?>()

    fun getCache(context: Context): SimpleCache {
        cacheRef.get()?.let { return it }
        synchronized(this) {
            cacheRef.get()?.let { return it }
            val cacheDir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
            val db: DatabaseProvider = StandaloneDatabaseProvider(context)
            val cache = SimpleCache(cacheDir, evictor, db)
            cacheRef.set(cache)
            return cache
        }
    }

    /** HTTP upstream -> cache -> player. Ignores the cache on upstream error. */
    fun buildDataSourceFactory(context: Context): CacheDataSource.Factory {
        val upstream = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("AniwaveStream/1.0")
        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Aggressive buffering: 50s ahead, spin up within 2.5s. */
    fun buildLoadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

    /** Fully wired ExoPlayer with cache + aggressive load control. */
    fun buildPlayer(context: Context): ExoPlayer =
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(buildDataSourceFactory(context))
            )
            .setLoadControl(buildLoadControl())
            .build()

    /** Release the global cache (call from Application.onTerminate / low-memory). */
    fun releaseCache() {
        cacheRef.getAndSet(null)?.release()
    }
}
