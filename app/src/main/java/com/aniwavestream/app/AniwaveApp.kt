package com.aniwavestream.app

import android.app.Application
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore

class AniwaveApp : Application() {
    lateinit var repository: AnimeRepository
        private set
    lateinit var library: UserLibraryStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AnimeRepository()
        library = UserLibraryStore(this)
    }
}
