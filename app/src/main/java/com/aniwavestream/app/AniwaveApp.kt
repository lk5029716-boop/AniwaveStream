package com.aniwavestream.app

import android.app.Application
import android.os.Looper
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore

class AniwaveApp : Application(), ImageLoaderFactory {
    lateinit var repository: AnimeRepository
        private set
    lateinit var library: UserLibraryStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AnimeRepository()
        library = UserLibraryStore(this)

        // Diagnostics only: log uncaught exceptions but DO NOT restart the app.
        // The previous handler force-restarted the launcher activity on ANY
        // main-thread throw — including transient player glitches — which made
        // the app appear to "close itself" / jump to home (the reported
        // force-close symptom). Masking the crash like that hides the real bug
        // and produces worse UX than a normal crash. Log it, then let the OS
        // handle the exception normally so the true root cause is visible.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("AniwaveApp", "Uncaught exception on ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Rule 8 — Advanced image caching polish.
     * A single global ImageLoader with crossfade enabled so grid/list scrolling
     * never shows sharp, jarring image snaps. Memory + disk caches keep massive
     * anime grids buttery on re-scroll.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(220)
            .placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.03)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
}
