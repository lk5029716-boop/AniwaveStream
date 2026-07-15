package com.aniwavestream.app

import android.app.Application
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
    }

    /**
     * Rule 8 — Advanced image caching polish.
     * A single global ImageLoader with crossfade enabled so grid/list scrolling
     * never shows sharp, jarring image snaps. Memory + disk caches keep massive
     * anime grids buttery on re-scroll.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .crossfade(220)
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
