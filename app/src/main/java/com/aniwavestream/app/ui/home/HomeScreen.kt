package com.aniwavestream.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.ui.components.AnimeRow
import com.aniwavestream.app.ui.components.AnivaveSectionCard
import com.aniwavestream.app.ui.components.AnivaveSectionCardNoHeader
import com.aniwavestream.app.ui.components.AnivaveScheduleCard
import com.aniwavestream.app.ui.components.AnivaveUpcomingCard
import com.aniwavestream.app.ui.components.AnivaveNewReleasesGrid
import com.aniwavestream.app.ui.components.ContinueCard
import com.aniwavestream.app.ui.components.ErrorBox
import com.aniwavestream.app.ui.components.HomeShimmer
import com.aniwavestream.app.ui.components.SectionHeader
import com.aniwavestream.app.ui.components.AnivaveHeroSlider
import com.aniwavestream.app.ui.components.AnivaveChipRow
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.Flame
import com.aniwavestream.app.ui.theme.Hairline
import com.aniwavestream.app.ui.theme.PlexMono
import com.aniwavestream.app.ui.theme.SurfaceRaised
import com.aniwavestream.app.ui.theme.TextSecondary
import com.aniwavestream.app.ui.theme.Void
import com.aniwavestream.app.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (Anime) -> Unit,
    onPlay: (Anime) -> Unit,
    onContinue: (Anime, Int) -> Unit,
    library: UserLibraryStore
) {
    val state by viewModel.state.collectAsState()
    var category by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = state.loading && state.hero != null,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Crossfade(
            targetState = when {
                state.loading && state.hero == null -> HomeContentState.LOADING
                state.error != null && state.hero == null -> HomeContentState.ERROR
                else -> HomeContentState.CONTENT
            },
            animationSpec = tween(350),
            label = "home-content"
        ) { contentState ->
            when (contentState) {
                HomeContentState.LOADING -> HomeShimmer(Modifier.fillMaxSize().background(Background))
                HomeContentState.ERROR -> ErrorBox(state.error ?: "") { viewModel.refresh() }
                HomeContentState.CONTENT -> LazyColumn(Modifier.fillMaxSize()) {
                    // Anivave auto-rotating hero slider
                    item {
                        AnivaveHeroSlider(
                            items = state.trending.take(5),
                            onPlay = onPlay,
                            onDetails = onAnimeClick,
                            onWatchlist = { anime ->
                                CoroutineScope(Dispatchers.Main).launch { library.toggleMyList(anime.id) }
                            }
                        )
                        Spacer(Modifier.height(18.dp))
                    }

                    // Category filter chips
                    item {
                        val cats = remember(state.trending) {
                            state.trending.flatMap { it.genres }.distinct().take(6)
                        }
                        AnivaveChipRow(categories = cats, selected = category) { category = it }
                        Spacer(Modifier.height(18.dp))
                    }

                    if (state.continueWatching.isNotEmpty()) {
                        item { SectionHeader("Continue Watching") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.continueWatching, key = { it.anime.id }) { item ->
                                    ContinueCard(
                                        anime = item.anime,
                                        episode = item.episode,
                                        progress = item.progressFraction,
                                        onClick = { onContinue(item.anime, item.episode) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    val filtered = if (category == null) state.trending
                    else state.trending.filter { it.genres.contains(category) }

                    if (filtered.isNotEmpty()) {
                        item { SectionHeader(category ?: "Trending Now") }
                        item {
                            AnimeRow(filtered, onAnimeClick)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (state.topRated.isNotEmpty()) {
                        item { SectionHeader("Top Rated") }
                        item {
                            AnimeRow(state.topRated, onAnimeClick)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (state.seasonal.isNotEmpty()) {
                        item { SectionHeader("This Season") }
                        item {
                            AnimeRow(state.seasonal, onAnimeClick)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // New Releases (bottom, top untouched) — anivave landscape tiles
                    item {
                        Column(Modifier.fillMaxWidth()) {
                            SectionHeader("New Releases")
                            Spacer(Modifier.height(4.dp))
                            AnivaveNewReleasesGrid(state.newReleaseEpisodes, onItem = { onAnimeClick(it.anime) })
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // Upcoming Anime (bottom) — anivave portrait cards + bell
                    item {
                        Column(Modifier.fillMaxWidth()) {
                            SectionHeader("Upcoming Anime")
                            Spacer(Modifier.height(4.dp))
                            if (state.upcoming.isEmpty()) {
                                Text("Nothing upcoming.", color = TextSecondary, fontFamily = PlexMono, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.upcoming.take(10), key = { it.id }) { anime ->
                                        AnivaveUpcomingCard(anime, { onAnimeClick(anime) })
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // Weekly Schedule (bottom) — anivave timed card w/ calendar pills
                    item {
                        Column(Modifier.fillMaxWidth()) {
                            SectionHeader("Weekly Schedule")
                            Spacer(Modifier.height(6.dp))
                            AnivaveScheduleCard(
                                activeDayIndex = state.scheduleDayIndex,
                                onDay = { viewModel.setScheduleDayIndex(it) },
                                shows = state.schedule,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

private enum class HomeContentState { LOADING, ERROR, CONTENT }
