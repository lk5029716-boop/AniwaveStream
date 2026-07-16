package com.aniwavestream.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.ui.home.SeeAllKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeeAllUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<Anime> = emptyList()
)

/**
 * Self-contained loader for the "See All" screen. It fetches its own data from the repository
 * (per kind) so it does NOT depend on HomeViewModel's shared state — this is what prevents the
 * "open See All → home silently reloads behind me" bug, where See All was showing the home's
 * seasonal snapshot (often still empty because seasonal loaded last in refresh()).
 */
class SeeAllViewModel(private val repository: AnimeRepository) : ViewModel() {

    private val _state = MutableStateFlow(SeeAllUiState())
    val state: StateFlow<SeeAllUiState> = _state.asStateFlow()

    private var loadedFor: SeeAllKind? = null

    fun load(kind: SeeAllKind) {
        if (loadedFor == kind && _state.value.items.isNotEmpty()) return
        loadedFor = kind
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = when (kind) {
                SeeAllKind.SEASONAL -> repository.seasonalPage(1, 50)
                SeeAllKind.TOP_RATED, SeeAllKind.TOP_100 -> repository.popularPage(1, 50)
                SeeAllKind.TRENDING -> repository.trending(50)
                SeeAllKind.UPCOMING -> repository.upcoming(50)
                SeeAllKind.NEW_RELEASES -> repository.newReleases(50)
                else -> Result.success(emptyList())
            }
            result.onSuccess { items ->
                _state.update { it.copy(loading = false, error = null, items = items) }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to load") }
            }
        }
    }

    fun retry(kind: SeeAllKind) {
        loadedFor = null
        load(kind)
    }

    companion object {
        fun factory(repo: AnimeRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SeeAllViewModel(repo) as T
        }
    }
}
