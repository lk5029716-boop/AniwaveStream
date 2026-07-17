package com.aniwavestream.app.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.BrowseGenres
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.ui.components.ErrorBox
import com.aniwavestream.app.ui.components.PosterGridShimmer
import com.aniwavestream.app.ui.theme.AnivaveArt
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.Bricolage
import com.aniwavestream.app.ui.theme.Flame
import com.aniwavestream.app.ui.theme.Hairline
import com.aniwavestream.app.ui.theme.PlexMono
import com.aniwavestream.app.ui.theme.SurfaceRaised
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import com.aniwavestream.app.ui.theme.Void

/** Top-level filter modes for the Browse hub. */
private enum class BrowseMode { GENRE, LETTER, YEAR }

private val LETTER_RANGES = listOf(
    "ALL",
    "A–E", "F–J", "K–O", "P–T", "U–Z"
)
private fun rangeLetters(range: String): List<String> = when (range) {
    "ALL" -> emptyList()
    "A–E" -> ('A'..'E').map { it.toString() }
    "F–J" -> ('F'..'J').map { it.toString() }
    "K–O" -> ('K'..'O').map { it.toString() }
    "P–T" -> ('P'..'T').map { it.toString() }
    "U–Z" -> ('U'..'Z').map { it.toString() }
    else -> emptyList()
}
private val YEARS = (2006..2026).toList().reversed()

@Composable
fun BrowseScreen(
    repository: AnimeRepository,
    onAnimeClick: (Anime) -> Unit
) {
    // Filter selection state.
    var mode by remember { mutableStateOf(BrowseMode.GENRE) }
    var selectedGenre by remember { mutableIntStateOf(BrowseGenres.first().id) }
    // Two-step All-Anime selector: a range ("ALL" / "A–E" ...), then a single letter.
    var selectedRange by remember { mutableStateOf<String?>(null) }
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<Anime>>(emptyList()) }

    // (Re)load whenever the active filter changes.
    LaunchedEffect(mode, selectedGenre, selectedRange, selectedLetter, selectedYear) {
        loading = true
        error = null
        val result = when (mode) {
            BrowseMode.GENRE -> repository.byGenre(selectedGenre)
            BrowseMode.LETTER -> repository.byLetter(selectedLetter ?: "All")
            BrowseMode.YEAR -> repository.byYear(selectedYear ?: 2026)
        }
        result
            .onSuccess { items = it; loading = false }
            .onFailure { error = it.message ?: "Failed to load"; loading = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Text(
            "Browse",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontFamily = Bricolage,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(16.dp)
        )

        // Top filter boxes: All Anime | Genre | Release Year (same line, consistent design).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrowseTopBox(
                label = "All Anime",
                selected = mode == BrowseMode.LETTER,
                onClick = {
                    mode = BrowseMode.LETTER
                    if (selectedRange == null) {
                        selectedRange = "ALL"
                        selectedLetter = "All"
                    }
                }
            )
            BrowseTopBox(
                label = "Genre",
                selected = mode == BrowseMode.GENRE,
                onClick = { mode = BrowseMode.GENRE }
            )
            BrowseTopBox(
                label = "Release Year",
                selected = mode == BrowseMode.YEAR,
                onClick = {
                    mode = BrowseMode.YEAR
                    if (selectedYear == null) selectedYear = 2026
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Contextual sub-filter panel under the selected top box.
        when (mode) {
            BrowseMode.GENRE -> GenreChips(
                selectedGenre = selectedGenre,
                onSelect = { selectedGenre = it }
            )
            BrowseMode.LETTER -> LetterRangeSelector(
                selectedRange = selectedRange,
                selectedLetter = selectedLetter,
                onRange = { range ->
                    selectedRange = range
                    if (range == "ALL") selectedLetter = "All" else selectedLetter = null
                },
                onLetter = { selectedLetter = it }
            )
            BrowseMode.YEAR -> YearGrid(
                selected = selectedYear,
                onSelect = { selectedYear = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        when {
            loading -> PosterGridShimmer(modifier = Modifier.fillMaxSize())
            error != null -> ErrorBox(error!!) {
                // Retry: force a reload by toggling selectedLetter/Year if set.
                val m = mode; mode = BrowseMode.GENRE; mode = m
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { anime ->
                    Column(Modifier.clickable { onAnimeClick(anime) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                        ) {
                            AnivaveArt(anime = anime, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            anime.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseTopBox(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Flame else SurfaceRaised)
            .border(1.dp, if (selected) Flame else Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (selected) Void else TextPrimary,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun GenreChips(selectedGenre: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(BrowseGenres, key = { it.id }) { genre ->
            val selected = genre.id == selectedGenre
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Flame else SurfaceRaised)
                    .border(1.dp, if (selected) Flame else Hairline, RoundedCornerShape(10.dp))
                    .clickable { onSelect(genre.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    genre.name,
                    color = if (selected) Void else TextSecondary,
                    fontFamily = PlexMono,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
private fun LetterRangeSelector(
    selectedRange: String?,
    selectedLetter: String?,
    onRange: (String) -> Unit,
    onLetter: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // Step 1 — range segments. Only one active at a time.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LETTER_RANGES) { range ->
                val isSel = range == selectedRange
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) Flame else SurfaceRaised)
                        .border(1.dp, if (isSel) Flame else Hairline, RoundedCornerShape(10.dp))
                        .clickable { onRange(range) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        range,
                        color = if (isSel) Void else TextPrimary,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Step 2 — letters for the chosen range, animated in.
        AnimatedVisibility(
            visible = selectedRange != null && selectedRange != "ALL",
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            val letters = rangeLetters(selectedRange ?: "")
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(letters) { ch ->
                    val isSel = ch == selectedLetter
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) Flame else SurfaceRaised)
                            .border(1.dp, if (isSel) Flame else Hairline, RoundedCornerShape(20.dp))
                            .clickable { onLetter(ch) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            ch,
                            color = if (isSel) Void else TextPrimary,
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearGrid(selected: Int?, onSelect: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        items(YEARS, key = { it }) { year ->
            val isSel = year == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) Flame else SurfaceRaised)
                    .border(1.dp, if (isSel) Flame else Hairline, RoundedCornerShape(10.dp))
                    .clickable { onSelect(year) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    year.toString(),
                    color = if (isSel) Void else TextPrimary,
                    fontFamily = PlexMono,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}
