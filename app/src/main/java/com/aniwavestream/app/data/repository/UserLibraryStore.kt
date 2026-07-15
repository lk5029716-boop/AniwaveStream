package com.aniwavestream.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("aniwave_library")

class UserLibraryStore(private val context: Context) {
    private val myListKey = stringSetPreferencesKey("my_list_ids")
    private val progressKey = stringSetPreferencesKey("progress") // "id:ep:fraction"

    val myListIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[myListKey].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    /** animeId -> (episode, progress 0f..1f) */
    val progressMap: Flow<Map<Int, Pair<Int, Float>>> = context.dataStore.data.map { prefs ->
        prefs[progressKey].orEmpty().mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size < 3) return@mapNotNull null
            val id = parts[0].toIntOrNull() ?: return@mapNotNull null
            val ep = parts[1].toIntOrNull() ?: return@mapNotNull null
            val frac = parts[2].toFloatOrNull() ?: return@mapNotNull null
            id to (ep to frac)
        }.toMap()
    }

    suspend fun toggleMyList(id: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[myListKey].orEmpty().toMutableSet()
            val key = id.toString()
            if (!current.add(key)) current.remove(key)
            prefs[myListKey] = current
        }
    }

    suspend fun saveProgress(animeId: Int, episode: Int, fraction: Float) {
        context.dataStore.edit { prefs ->
            val current = prefs[progressKey].orEmpty()
                .filterNot { it.startsWith("$animeId:") }
                .toMutableSet()
            current.add("$animeId:$episode:${fraction.coerceIn(0f, 1f)}")
            prefs[progressKey] = current
        }
    }
}
