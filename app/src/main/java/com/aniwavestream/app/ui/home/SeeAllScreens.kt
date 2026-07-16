package com.aniwavestream.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.DayAiring
import com.aniwavestream.app.data.model.DemoSchedule
import com.aniwavestream.app.data.model.ScheduleDays
import com.aniwavestream.app.ui.components.AnimeRow
import com.aniwavestream.app.ui.components.AnivaveNewReleasesGrid
import com.aniwavestream.app.ui.components.AnivaveUpcomingCard
import com.aniwavestream.app.ui.components.ScheduleRow
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.Bricolage
import com.aniwavestream.app.ui.theme.Hairline
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.viewmodel.HomeViewModel

enum class SeeAllKind { TRENDING, TOP_RATED, SEASONAL, NEW_RELEASES, UPCOMING, CONTINUE }

/** Generic "View All" screen — shows the FULL list for a given Home section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    kind: SeeAllKind,
    viewModel: HomeViewModel,
    onAnimeClick: (Anime) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val (title, items) = when (kind) {
        SeeAllKind.TRENDING -> "Trending Now" to state.trending
        SeeAllKind.TOP_RATED -> "Top Rated" to state.topRated
        SeeAllKind.SEASONAL -> "This Season" to state.seasonal
        SeeAllKind.UPCOMING -> "Upcoming Anime" to state.upcoming
        SeeAllKind.CONTINUE -> "Continue Watching" to state.continueWatching.map { it.anime }
        SeeAllKind.NEW_RELEASES -> "New Releases" to state.newReleaseEpisodes.map { it.anime }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(title, color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                when (kind) {
                    SeeAllKind.NEW_RELEASES -> {
                        AnivaveNewReleasesGrid(
                            items = state.newReleaseEpisodes,
                            onItem = { onAnimeClick(it.anime) },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
                    }
                    SeeAllKind.UPCOMING -> {
                        Column(Modifier.fillMaxWidth()) {
                            if (items.isEmpty()) {
                                Text("Nothing upcoming.", color = TextMuted, modifier = Modifier.padding(16.dp))
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(items, key = { it.id }) { anime ->
                                        AnivaveUpcomingCard(anime, { onAnimeClick(anime) })
                                    }
                                }
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
                        }
                    }
                    else -> {
                        if (items.isEmpty()) {
                            Text("Nothing here yet.", color = TextMuted, modifier = Modifier.padding(16.dp))
                        } else {
                            AnimeRow(items, onAnimeClick)
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Full Weekly Schedule screen — every day's broadcasts, same card/row design as Home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    onAnimeClick: (Anime) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule", color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScheduleDays.forEach { day ->
                val shows: List<DayAiring> = DemoSchedule[day] ?: emptyList()
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    ) {
                        Text(
                            day.replaceFirstChar { it.uppercase() },
                            color = TextPrimary,
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                        if (shows.isEmpty()) {
                            Text("No streams.", color = TextMuted, fontSize = 12.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                shows.forEach { s -> ScheduleRow(s) { onAnimeClick(Anime(title = s.title)) } }
                            }
                        }
                    }
                }
            }
        }
    }
}
