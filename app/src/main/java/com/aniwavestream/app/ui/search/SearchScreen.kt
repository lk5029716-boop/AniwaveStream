package com.aniwavestream.app.ui.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.ui.components.EpisodeListShimmer
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.Bricolage
import com.aniwavestream.app.ui.theme.Flame
import com.aniwavestream.app.ui.theme.Hairline
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.PlexMono
import com.aniwavestream.app.ui.theme.SurfaceRaised
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import com.aniwavestream.app.ui.theme.Void
import kotlinx.coroutines.delay

private val TYPE_FILTERS = listOf("All", "TV", "Movie", "OVA", "ONA", "Special", "Music")

@Composable
fun SearchScreen(
    repository: AnimeRepository,
    onAnimeClick: (Anime) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var typeFilter by remember { mutableStateOf("All") }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            error = null
            loading = false
            return@LaunchedEffect
        }
        delay(400)
        loading = true
        error = null
        repository.search(query)
            .onSuccess {
                results = it
                loading = false
            }
            .onFailure {
                error = it.message
                loading = false
            }
    }

    val filtered = if (typeFilter == "All") results else results.filter {
        it.type?.equals(typeFilter, ignoreCase = true) == true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(top = 12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search anime…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OrangePrimary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = OrangePrimary,
                focusedContainerColor = SurfaceRaised,
                unfocusedContainerColor = SurfaceRaised
            )
        )
        Spacer(Modifier.height(12.dp))

        // Type filter chips (single-select, horizontally scrollable).
        if (query.isNotBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TYPE_FILTERS) { type ->
                    val isSel = type == typeFilter
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) Flame else SurfaceRaised)
                            .border(1.dp, if (isSel) Flame else Hairline, RoundedCornerShape(20.dp))
                            .clickable { typeFilter = type }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            type,
                            color = if (isSel) Void else TextPrimary,
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        when {
            loading -> EpisodeListShimmer(modifier = Modifier.fillMaxSize())
            query.isBlank() -> BoxHint("Find titles, genres vibes, or classics")
            error != null -> BoxHint(error!!)
            filtered.isEmpty() -> BoxHint(
                if (typeFilter == "All") "No results for “$query”" else "No $typeFilter results for “$query”"
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { anime ->
                    SearchCard(anime) { onAnimeClick(anime) }
                }
            }
        }
    }
}

@Composable
private fun SearchCard(anime: Anime, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Bottom scrim + title overlay for legibility on bright posters.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to androidx.compose.ui.graphics.Color.Transparent,
                            0.72f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
                            1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f)
                        )
                    )
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    anime.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = listOfNotNull(
                    anime.year?.toString(),
                    anime.score?.let { "★ ${"%.1f".format(it)}" }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        meta,
                        color = TextSecondary,
                        fontFamily = PlexMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxHint(text: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = TextSecondary)
    }
}
