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
import com.aniwavestream.app.ui.components.AnimeRankedRow
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
import com.aniwavestream.app.ui.home.SeeAllKind
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
    onViewAll: (SeeAllKind) -> Unit = { _: SeeAllKind -> },
    onViewSchedule: () -> Unit = {},
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

                    if (state.continueWatching.isNotEmpty()) {
                        item { SectionHeader("Continue Watching", onSeeAll = { onViewAll(SeeAllKind.CONTINUE) }) }
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

                    item { SectionHeader(category ?: "Trending Now", onSeeAll = { onViewAll(SeeAllKind.TRENDING) }) }
                    item {
                        if (filtered.isNotEmpty()) AnimeRow(filtered, onAnimeClick)
                        else SectionPlaceholder("Trending is loading…")
                        Spacer(Modifier.height(8.dp))
                    }

                    item { SectionHeader("POPULAR THIS SEASON", onSeeAll = { onViewAll(SeeAllKind.SEASONAL) }) }
                    item {
                        if (state.seasonal.isNotEmpty()) AnimeRow(state.seasonal, onAnimeClick)
                        else SectionPlaceholder("Popular This Season is loading…")
                        Spacer(Modifier.height(8.dp))
                    }

                    item { SectionHeader("ALL TIME POPULAR", onSeeAll = { onViewAll(SeeAllKind.TOP_RATED) }) }
                    item {
                        if (state.topRated.isNotEmpty()) AnimeRow(state.topRated, onAnimeClick)
                        else SectionPlaceholder("All Time Popular is loading…")
                        Spacer(Modifier.height(8.dp))
                    }

                    // TOP 100 ANIME — ranked cards (number 1-100 on the card, name on the card)
                    item { SectionHeader("TOP 100 ANIME", onSeeAll = { onViewAll(SeeAllKind.TOP_100) }) }
                    item {
                        if (state.top100.isNotEmpty()) {
                            AnimeRankedRow(items = state.top100.take(20), onAnimeClick = onAnimeClick)
                        } else {
                            SectionPlaceholder("Top 100 is loading…")
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // New Releases (bottom, top untouched) — anivave landscape tiles
                    item {
                        Column(Modifier.fillMaxWidth()) {
                            SectionHeader("New Releases", onSeeAll = { onViewAll(SeeAllKind.NEW_RELEASES) })
                            Spacer(Modifier.height(4.dp))
                            AnivaveNewReleasesGrid(state.newReleaseEpisodes, onItem = { onAnimeClick(it.anime) })
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // Upcoming Anime (bottom) — anivave portrait cards + bell
                    item {
                        Column(Modifier.fillMaxWidth()) {
                            SectionHeader("Upcoming Anime", onSeeAll = { onViewAll(SeeAllKind.UPCOMING) })
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
                            SectionHeader("Weekly Schedule", onSeeAll = onViewSchedule)
                            Spacer(Modifier.height(6.dp))
                            AnivaveScheduleCard(
                                activeDayIndex = state.scheduleDayIndex,
                                onDay = { viewModel.setScheduleDayIndex(it) },
                                shows = state.schedule,
                                maxRows = 4,
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

@Composable
private fun SectionPlaceholder(text: String) {
    Text(
        text,
        color = TextSecondary,
        fontFamily = PlexMono,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    )
}
