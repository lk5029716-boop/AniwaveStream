package com.aniwavestream.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.ui.components.AnimeRow
import com.aniwavestream.app.ui.components.ContinueCard
import com.aniwavestream.app.ui.components.ErrorBox
import com.aniwavestream.app.ui.components.HeroBanner
import com.aniwavestream.app.ui.components.LoadingBox
import com.aniwavestream.app.ui.components.SectionHeader
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (Anime) -> Unit,
    onPlay: (Anime) -> Unit,
    onContinue: (Anime, Int) -> Unit
) {
    val state by viewModel.state.collectAsState()

    PullToRefreshBox(
        isRefreshing = state.loading && state.hero != null,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when {
            state.loading && state.hero == null -> LoadingBox()
            state.error != null && state.hero == null -> ErrorBox(state.error!!) { viewModel.refresh() }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "ANIVAVE",
                            color = OrangePrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            letterSpacing = 2.sp
                        )
                        Text("Premium demo", color = TextPrimary.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
                state.hero?.let { hero ->
                    item {
                        HeroBanner(
                            anime = hero,
                            onPlay = { onPlay(hero) },
                            onDetails = { onAnimeClick(hero) }
                        )
                    }
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
                if (state.trending.isNotEmpty()) {
                    item { SectionHeader("Trending Now") }
                    item {
                        AnimeRow(state.trending, onAnimeClick)
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
                if (state.topRated.isNotEmpty()) {
                    item { SectionHeader("Top Rated") }
                    item {
                        AnimeRow(state.topRated, onAnimeClick)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
