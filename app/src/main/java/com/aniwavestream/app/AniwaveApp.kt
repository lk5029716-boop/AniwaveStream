package com.aniwavestream.app

import android.app.Application
import android.content.Intent
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

        // Global safety net: any uncaught exception on the main thread would
        // otherwise force-close the WHOLE app ("app closes itself" bug). Catch
        // it, log it, and restart at a safe root instead of dying to the launcher.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("AniwaveApp", "Uncaught exception on ${thread.name}", throwable)
            if (thread === Looper.getMainLooper().thread) {
                // Restart the app at the launcher activity so the user lands
                // somewhere safe instead of a dead process.
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    intent?.let { startActivity(it) }
                } catch (e: Throwable) {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
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
            .placeholder(android.graphics.ColorDrawable(android.graphics.Color.TRANSPARENT))
            .error(android.graphics.ColorDrawable(android.graphics.Color.TRANSPARENT))
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
