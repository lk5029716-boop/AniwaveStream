package com.aniwavestream.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.DayAiring
import com.aniwavestream.app.data.model.DemoSchedule
import com.aniwavestream.app.data.model.ScheduleDays
import com.aniwavestream.app.ui.components.AnimePosterCard
import com.aniwavestream.app.ui.components.AnimeRankedCard
import com.aniwavestream.app.ui.components.AnivaveScheduleCard
import com.aniwavestream.app.ui.components.SecondaryPillButton
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.Bricolage
import com.aniwavestream.app.ui.theme.Flame
import com.aniwavestream.app.ui.theme.Hairline
import com.aniwavestream.app.ui.theme.PlexMono
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.viewmodel.HomeViewModel
import com.aniwavestream.app.viewmodel.SeeAllViewModel

enum class SeeAllKind { TRENDING, TOP_RATED, SEASONAL, NEW_RELEASES, UPCOMING, CONTINUE, TOP_100 }

private const val PAGE_SIZE = 50

/** Custom type filter shown as a nav row in every See All screen. */
private val TYPE_FILTERS = listOf("All", "TV", "Movie", "Airing")

/**
 * "View All" screen for a Home section.
 * - 3-column poster grid
 * - up to 50 anime per page, with Prev/Next + page-number pagination
 * - a custom type-nav row (All / TV / Movie / Airing) to filter the list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    kind: SeeAllKind,
    viewModel: SeeAllViewModel,
    homeViewModel: HomeViewModel,
    onAnimeClick: (Anime) -> Unit,
    onBack: () -> Unit
) {
    // Network-loaded kinds fetch their own data via SeeAllViewModel (independent of Home, so
    // opening this screen never triggers a home reload). Home-derived kinds (Continue Watching,
    // New Releases) still read from the shared HomeViewModel snapshot.
    val seesAll by viewModel.state.collectAsState()
    val homeState by homeViewModel.state.collectAsState()

    val networkKind = kind in setOf(
        SeeAllKind.TRENDING, SeeAllKind.TOP_RATED, SeeAllKind.SEASONAL,
        SeeAllKind.UPCOMING, SeeAllKind.TOP_100
    )
    LaunchedEffect(kind) {
        if (networkKind) viewModel.load(kind)
    }

    val all: List<Anime> = when (kind) {
        SeeAllKind.TRENDING -> if (networkKind) seesAll.items else homeState.trending
        SeeAllKind.TOP_RATED -> if (networkKind) seesAll.items else homeState.topRated
        SeeAllKind.SEASONAL -> if (networkKind) seesAll.items else homeState.seasonal
        SeeAllKind.UPCOMING -> if (networkKind) seesAll.items else homeState.upcoming
        SeeAllKind.TOP_100 -> if (networkKind) seesAll.items else homeState.top100
        SeeAllKind.CONTINUE -> homeState.continueWatching.map { it.anime }
        SeeAllKind.NEW_RELEASES -> homeState.newReleaseEpisodes.map { it.anime }
    }
    val title = when (kind) {
        SeeAllKind.TRENDING -> "Trending Now"
        SeeAllKind.TOP_RATED -> "All Time Popular"
        SeeAllKind.SEASONAL -> "Popular This Season"
        SeeAllKind.UPCOMING -> "Upcoming Anime"
        SeeAllKind.CONTINUE -> "Continue Watching"
        SeeAllKind.NEW_RELEASES -> "New Releases"
        SeeAllKind.TOP_100 -> "Top 100 Anime"
    }

    var typeFilter by remember { mutableStateOf("All") }
    var page by remember { mutableIntStateOf(0) }

    // While a network kind is still loading (and we have nothing yet), show a shimmer — do not
    // show an empty grid that looks like the app "refreshed itself".
    if (networkKind && seesAll.loading && seesAll.items.isEmpty()) {
        SeeAllLoading(title = title, onBack = onBack)
        return
    }
    if (networkKind && seesAll.error != null && seesAll.items.isEmpty()) {
        SeeAllError(error = seesAll.error ?: "Failed", onRetry = { viewModel.retry(kind) }, onBack = onBack)
        return
    }

    // Apply the custom type filter.
    val filtered = remember(all, typeFilter) {
        if (typeFilter == "All") all else all.filter { matchesType(it, typeFilter) }
    }
    val pageCount = maxOf(1, (filtered.size + PAGE_SIZE - 1) / PAGE_SIZE)
    val safePage = page.coerceIn(0, pageCount - 1)
    val pageItems = filtered.drop(safePage * PAGE_SIZE).take(PAGE_SIZE)

    // Pad to a full multiple of 3 so the last row is never left dangling (no dead space).
    val fillers = (3 - (pageItems.size % 3)) % 3
    val displayItems = pageItems + List(fillers) { null }

    // Infinite scroll: auto-append the next page when the user nears the end.
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, safePage, pageCount) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastIndex ->
                val total = displayItems.size + 2 // + chips + pagination footer
                if (lastIndex >= total - 4 && safePage < pageCount - 1) {
                    page = safePage + 1
                }
            }
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        title,
                        color = TextPrimary,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter chips (full-width header row)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TYPE_FILTERS.forEach { t ->
                        val active = t == typeFilter
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (active) Flame else Color.Transparent)
                                .border(1.dp, Hairline, RoundedCornerShape(50.dp))
                                .clickable { typeFilter = t; page = 0 }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                t,
                                color = if (active) Color.White else TextMuted,
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Poster grid (3 fixed columns, cards fill their cell). Nulls render as empty
            // placeholder cells so the final row is always complete.
            items(displayItems, key = { it?.id ?: "filler-${displayItems.indexOf(it)}" }) { anime ->
                if (anime != null) {
                    if (kind == SeeAllKind.TOP_100) {
                        val rank = all.indexOf(anime) + 1
                        AnimeRankedCard(
                            anime = anime,
                            rank = rank,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(Modifier.height(1.dp))
                }
            }

            // Pagination footer (full-width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                PaginationBar(
                    page = safePage,
                    pageCount = pageCount,
                    onPage = { page = it }
                )
            }
        }
    }
}

private fun matchesType(anime: Anime, type: String): Boolean {
    return when (type) {
        "TV" -> anime.type.equals("TV", true) || anime.type.equals("TV Series", true)
        "Movie" -> anime.type.equals("Movie", true) || anime.type.equals("Film", true)
        "Airing" -> anime.status.equals("Airing", true) || anime.status.equals("Currently Airing", true)
        else -> true
    }
}

@Composable
private fun PaginationBar(page: Int, pageCount: Int, onPage: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = { onPage((page - 1).coerceAtLeast(0)) },
            enabled = page > 0,
            colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
        ) { Text("‹ Prev") }

        // page number chips (max 7 shown, centered on current)
        val window = pageWindow(page, pageCount)
        window.forEach { p ->
            if (p < 0) {
                Text("…", color = TextMuted, modifier = Modifier.padding(horizontal = 6.dp))
            } else {
                val active = p == page
                TextButton(
                    onClick = { onPage(p) },
                    colors = ButtonDefaults.textButtonColors(contentColor = if (active) Flame else TextMuted)
                ) {
                    Text((p + 1).toString(), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        TextButton(
            onClick = { onPage((page + 1).coerceAtMost(pageCount - 1)) },
            enabled = page < pageCount - 1,
            colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
        ) { Text("Next ›") }
    }
}

private fun pageWindow(current: Int, total: Int): List<Int> {
    if (total <= 7) return (0 until total).toList()
    val set = linkedSetOf(0, total - 1, current - 1, current, current + 1)
    set.removeIf { it < 0 || it >= total }
    val sorted = set.sorted()
    val out = mutableListOf<Int>()
    var prev = -2
    for (p in sorted) {
        if (p - prev > 1) out.add(-1) // ellipsis marker
        out.add(p)
        prev = p
    }
    return out
}

/** Shimmer placeholder shown while a See All screen fetches its own data (so it never looks
 *  like the app "refreshed itself" with an empty/transient grid). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeeAllLoading(title: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(title, color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Flame)
        }
    }
}

/** Dark error screen for a failed See All fetch — Retry + Back, no white flash. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeeAllError(error: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Error", color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(error, color = Flame, fontFamily = PlexMono, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            SecondaryPillButton(text = "Retry", leadingIcon = Icons.Default.Refresh, onClick = onRetry)
        }
    }
}

/**
 * Weekly Schedule "View All" — SAME card design as Home (day pills + timed rows),
 * just opened full-screen with its own day selector. Selecting a day shows that
 * day's full broadcast list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    onAnimeClick: (Anime) -> Unit,
    onBack: () -> Unit
) {
    var activeDayIndex by remember { mutableIntStateOf(3) }
    val day = ScheduleDays.getOrElse(activeDayIndex) { ScheduleDays[0] }
    val shows: List<DayAiring> = DemoSchedule[day] ?: emptyList()

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Weekly Schedule",
                        color = TextPrimary,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            item {
                // Reuse the exact Home card design, but show ALL rows for the selected day.
                AnivaveScheduleCard(
                    activeDayIndex = activeDayIndex,
                    onDay = { activeDayIndex = it },
                    shows = shows,
                    onItem = { onAnimeClick(Anime(title = it.title)) },
                    maxRows = Int.MAX_VALUE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
