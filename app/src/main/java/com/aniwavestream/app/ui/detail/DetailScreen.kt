package com.aniwavestream.app.ui.detail

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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.aniwavestream.app.ui.theme.AnivaveArt
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.Bricolage
import com.aniwavestream.app.ui.theme.Flame
import com.aniwavestream.app.ui.theme.Gold
import com.aniwavestream.app.ui.theme.Hairline
import com.aniwavestream.app.ui.theme.PlexMono
import com.aniwavestream.app.ui.theme.SurfaceRaised
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import com.aniwavestream.app.ui.theme.Void
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
                            .height(360.dp)
                            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
                    ) {
                        AnivaveArt(anime = a, modifier = Modifier.fillMaxSize())
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Background),
                                        startY = 40f, endY = 460f
                                    )
                                )
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(12.dp)
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
                        // Studio / source eyebrow
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Flame)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                a.studios.firstOrNull() ?: "FEATURED",
                                color = Flame,
                                fontFamily = PlexMono,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            a.title,
                            color = TextPrimary,
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            lineHeight = 30.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.height(8.dp))
                        // Rating + type/status row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (a.score != null) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "%.1f".format(a.score),
                                    color = Gold,
                                    fontFamily = PlexMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                listOfNotNull(a.year?.toString(), a.type, a.status).joinToString(" · "),
                                color = TextSecondary,
                                fontFamily = PlexMono,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        // Genre pills
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(0.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(a.genres) { g ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                                        .background(SurfaceRaised)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(g, color = TextSecondary, fontFamily = PlexMono, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryPillButton(text = "Play E1", leadingIcon = Icons.Default.PlayArrow, onClick = { onPlay(1) })
                            SecondaryPillButton(
                                text = if (inList) "In List" else "My List",
                                leadingIcon = if (inList) Icons.Default.Check else Icons.Default.Add,
                                onClick = { scope.launch { library.toggleMyList(animeId) } }
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        // Info grid (Source / Status / Episodes)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                                .background(SurfaceRaised)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            InfoCell("SOURCE", "Jikan")
                            InfoCell("STATUS", a.status ?: "—")
                            InfoCell("EPISODES", (a.episodes ?: 0).toString())
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("Synopsis", color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                            fontFamily = PlexMono,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Text("Episodes", color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
private fun InfoCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextMuted, fontFamily = PlexMono, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}
