@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aniwavestream.app.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

/**
 * Phase 3 — Video streaming engine.
 *
 * Central place that owns:
 *  - Rule 9a: a tuned [DefaultLoadControl] that buffers aggressively ahead of
 *    playback so users don't see mid-stream buffering circles.
 *  - Rule 9b: a single process-wide [SimpleCache] with LRU eviction, wired into a
 *    [CacheDataSource.Factory] so rewinding/pausing reads from disk instead of
 *    re-fetching from the network.
 *  - FFmpeg software decoder extension (nextlib) for dual-audio / extra subtitle
 *    codecs the built-in decoders can't handle (Dantotsu-style).
 */
object PlayerModule {

    // ---- Rule 9b: global LRU disk cache (max 256 MB) ------------------------
    private const val MAX_CACHE_BYTES = 256L * 1024 * 1024

    @Volatile
    private var cache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: runCatching {
                SimpleCache(
                    context.applicationContext.cacheDir.resolve("media_cache"),
                    LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                    StandaloneDatabaseProvider(context.applicationContext)
                )
            }.getOrElse { ex ->
                // A stale lock file or bad DB can throw here. Fall back to a
                // fresh, lock-free cache dir so playback can still start.
                SimpleCache(
                    context.applicationContext.cacheDir.resolve("media_cache_${System.currentTimeMillis()}"),
                    LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
                ).also { android.util.Log.w("PlayerModule", "cache fallback: $ex") }
            }.also { cache = it }
        }

    /** Upstream HTTP source with sane timeouts + cross-redirect support. */
    private fun httpDataSourceFactory() = DefaultHttpDataSource.Factory()
        .setUserAgent("AniwaveStream/1.0 (Android)")
        .setConnectTimeoutMs(30_000)
        .setReadTimeoutMs(30_000)
        .setAllowCrossProtocolRedirects(true)

    /** Cache-backed data source: disk first, network on miss (Rule 9b). */
    private fun cacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val upstream = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory())
        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Rule 9a: buffer generously ahead so playback rarely stalls. */
    private fun loadControl(): DefaultLoadControl =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 120_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(/* backBufferDurationMs = */ 30_000, /* retainBackBufferFromKeyframe = */ true)
            .build()

    /**
     * Build an ExoPlayer wired with the tuned load control + cache-backed media
     * source factory. Uses the FFmpeg extension renderer (nextlib) so japanese/eng
     * DUB (dual audio) and extra subtitle codecs play, plus an explicit
     * [DefaultTrackSelector] so the UI can pick audio/subtitle tracks.
     */
    fun buildPlayer(context: Context): ExoPlayer {
        val appContext = context.applicationContext
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory(appContext))
        val renderersFactory = NextRenderersFactory(appContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val trackSelector = DefaultTrackSelector(appContext)
        return ExoPlayer.Builder(appContext)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl())
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
    }

    /**
     * Build a [MediaItem] for a stream URL resolved from the Aniwaves backend.
     *
     * PLAYBACK ROOT CAUSE FIX: the backend returns URLs of the form
     * `.../api/proxy?url=<cdn>...m3u8...` whose last path segment is `proxy`,
     * NOT a `.m3u8` file. ExoPlayer normally chooses the media-source type by
     * sniffing the URI extension (Util.inferContentType) BEFORE fetching the
     * playlist. With no `.m3u8` extension it infers CONTENT_TYPE_OTHER and throws
     * "Unsupported type" — so the video never plays ("it's not a video player").
     * Declaring the MIME type explicitly forces the HlsMediaSource to be used.
     */
    fun buildMediaItem(
        streamUrl: String,
        drmLicenseUrl: String? = null,
        drmHeaders: Map<String, String> = emptyMap()
    ): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(streamUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
        if (!drmLicenseUrl.isNullOrBlank()) {
            builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(drmLicenseUrl)
                    .setLicenseRequestHeaders(drmHeaders)
                    .setMultiSession(true)
                    .build()
            )
        }
        return builder.build()
    }

    /** Release the shared cache (call on app teardown if ever needed). */
    fun releaseCache() {
        synchronized(this) {
            cache?.release()
            cache = null
        }
    }
}
