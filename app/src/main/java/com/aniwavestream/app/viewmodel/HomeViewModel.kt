package com.aniwavestream.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.ContinueItem
import com.aniwavestream.app.data.model.DayAiring
import com.aniwavestream.app.data.model.DemoNewReleases
import com.aniwavestream.app.data.model.DemoNewReleaseEpisodes
import com.aniwavestream.app.data.model.DemoSchedule
import com.aniwavestream.app.data.model.DemoUpcoming
import com.aniwavestream.app.data.model.NewReleaseEpisode
import com.aniwavestream.app.data.model.ScheduleDays
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val hero: Anime? = null,
    val trending: List<Anime> = emptyList(),
    val topRated: List<Anime> = emptyList(),
    val seasonal: List<Anime> = emptyList(),
    val newReleases: List<Anime> = emptyList(),
    val newReleaseEpisodes: List<NewReleaseEpisode> = DemoNewReleaseEpisodes,
    val upcoming: List<Anime> = emptyList(),
    val scheduleDayIndex: Int = 3,
    val schedule: List<DayAiring> = emptyList(),
    val continueWatching: List<ContinueItem> = emptyList()
)

class HomeViewModel(
    private val repository: AnimeRepository,
    private val library: UserLibraryStore
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            combine(library.progressMap, library.myListIds) { progress, _ -> progress }
                .collect { progress ->
                    val items = progress.mapNotNull { (id, pair) ->
                        val anime = repository.cached(id) ?: return@mapNotNull null
                        ContinueItem(anime, pair.first, pair.second)
                    }
                    _state.update { it.copy(continueWatching = items) }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val trending = async { repository.trending() }
            val top = async { repository.topRated() }
            val newRel = async { repository.newReleases() }
            val upc = async { repository.upcoming() }
            // sequential-ish via throttle inside repo; still fire after first batch
            val t = trending.await()
            val r = top.await()
            val nr = newRel.await()
            val up = upc.await()
            val s = repository.seasonal()

            val errors = listOf(t, r, s, nr, up).mapNotNull { it.exceptionOrNull()?.message }
            if (t.isFailure && r.isFailure && s.isFailure) {
                _state.update {
                    it.copy(loading = false, error = errors.firstOrNull() ?: "Failed to load catalog")
                }
                return@launch
            }

            val trendingList = t.getOrDefault(emptyList())
            val topList = r.getOrDefault(emptyList())
            val seasonalList = s.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    loading = false,
                    error = null,
                    hero = seasonalList.firstOrNull() ?: trendingList.firstOrNull() ?: topList.firstOrNull(),
                    trending = trendingList,
                    topRated = topList,
                    seasonal = seasonalList,
                    newReleases = nr.getOrDefault(emptyList()).ifEmpty { DemoNewReleases },
                    newReleaseEpisodes = DemoNewReleaseEpisodes,
                    upcoming = up.getOrDefault(emptyList()).ifEmpty { DemoUpcoming }
                )
            }
            loadSchedule(_state.value.scheduleDayIndex)
        }
    }

    fun setScheduleDayIndex(index: Int) {
        _state.update { it.copy(scheduleDayIndex = index) }
        loadSchedule(index)
    }

    private fun loadSchedule(index: Int) {
        // anivave's weekly schedule is a timed (JST) list; we use the demo slate
        // (the live Jikan /schedules endpoint doesn't return per-show air times).
        val day = ScheduleDays.getOrElse(index) { ScheduleDays[0] }
        _state.update { it.copy(schedule = DemoSchedule[day] ?: emptyList()) }
    }

    companion object {
        fun factory(repo: AnimeRepository, library: UserLibraryStore) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(repo, library) as T
            }
    }
}

class LibraryViewModel(
    private val repository: AnimeRepository,
    private val library: UserLibraryStore
) : ViewModel() {
    private val _items = MutableStateFlow<List<Anime>>(emptyList())
    val items: StateFlow<List<Anime>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            library.myListIds.collect { ids ->
                _loading.value = true
                val resolved = ids.mapNotNull { id ->
                    repository.cached(id) ?: repository.detail(id).getOrNull()
                }
                _items.value = resolved
                _loading.value = false
            }
        }
    }

    companion object {
        fun factory(repo: AnimeRepository, library: UserLibraryStore) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(repo, library) as T
            }
    }
}
