package com.aniwavestream.app.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.DemoStreams
import com.aniwavestream.app.data.model.Episode
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.ui.components.EpisodeCard
import com.aniwavestream.app.ui.components.EpisodeListShimmer
import com.aniwavestream.app.ui.components.ErrorBox
import com.aniwavestream.app.ui.components.PrimaryPillButton
import com.aniwavestream.app.ui.components.SecondaryPillButton
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    animeId: Int,
    repository: AnimeRepository,
    library: UserLibraryStore,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onRelated: (Int) -> Unit
) {
    var anime by remember { mutableStateOf<Anime?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val myList by library.myListIds.collectAsState(initial = emptySet())
    val inList = animeId in myList
    val scope = rememberCoroutineScope()

    LaunchedEffect(animeId) {
        loading = true
        error = null
        repository.detail(animeId)
            .onSuccess {
                anime = it
                episodes = DemoStreams.buildEpisodes(it)
                loading = false
            }
            .onFailure {
                error = it
                loading = false
            }
    }

    when {
        loading -> EpisodeListShimmer(modifier = Modifier.fillMaxSize().background(Background))
        error != null -> ErrorBox(error!!) {
            scope.launch {
                loading = true
                error = null
                repository.detail(animeId)
                    .onSuccess {
                        anime = it
                        episodes = DemoStreams.buildEpisodes(it)
                        loading = false
                    }
                    .onFailure { e -> error = e; loading = false }
            }
        }
        anime != null -> {
            val a = anime!!
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .background(Background)
            ) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        AsyncImage(
                            model = a.bannerUrl ?: a.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Background)
                                    )
                                )
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.45f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(a.title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        val meta = listOfNotNull(
                            a.score?.let { "★ ${"%.1f".format(it)}" },
                            a.year?.toString(),
                            a.type,
                            a.status,
                            a.episodes?.let { "$it eps" }
                        ).joinToString("  ·  ")
                        Text(meta, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        if (a.genres.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(a.genres.joinToString("  ·  "), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryPillButton(text = "▶  Play E1", onClick = { onPlay(1) })
                            SecondaryPillButton(
                                text = if (inList) "✓ In List" else "+ My List",
                                onClick = { scope.launch { library.toggleMyList(animeId) } }
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("Synopsis", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            a.synopsis.ifBlank { "No synopsis available." },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Demo notice: episode streams are public sample videos, not licensed anime.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(20.dp))
                        Text("Episodes", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(episodes, key = { it.number }) { ep ->
                    EpisodeCard(
                        episode = ep,
                        thumbnailUrl = a.bannerUrl ?: a.posterUrl,
                        watchProgress = 0f,
                        onClick = { onPlay(ep.number) }
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun EpisodeRow(ep: Episode, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OrangePrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OrangePrimary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ep.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(ep.durationLabel, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text("E${ep.number}", color = OrangePrimary, style = MaterialTheme.typography.labelLarge)
    }
}
