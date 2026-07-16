package com.aniwavestream.app.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

@Composable
fun BrowseScreen(
    repository: AnimeRepository,
    onAnimeClick: (Anime) -> Unit
) {
    var selectedGenre by remember { mutableIntStateOf(BrowseGenres.first().id) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<Anime>>(emptyList()) }

    LaunchedEffect(selectedGenre) {
        loading = true
        error = null
        repository.byGenre(selectedGenre)
            .onSuccess {
                items = it
                loading = false
            }
            .onFailure {
                error = it.message ?: "Failed to load genre"
                loading = false
            }
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
                        .border(
                            1.dp,
                            if (selected) Flame else Hairline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedGenre = genre.id }
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
        Spacer(Modifier.height(12.dp))
        when {
            loading -> PosterGridShimmer(modifier = Modifier.fillMaxSize())
            error != null -> ErrorBox(error!!) {
                val g = selectedGenre; selectedGenre = -1; selectedGenre = g
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
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
