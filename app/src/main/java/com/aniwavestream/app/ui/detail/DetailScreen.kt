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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.DemoStreams
import com.aniwavestream.app.data.model.Character
import com.aniwavestream.app.data.model.Episode
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.ui.components.EpisodeCard
import com.aniwavestream.app.ui.components.AnivaveSectionCard
import com.aniwavestream.app.ui.components.CharacterRow
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
import com.aniwavestream.app.ui.theme.Purple
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
    var characters by remember { mutableStateOf<List<Character>>(emptyList()) }
    var related by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var crash by remember { mutableStateOf<Throwable?>(null) }
    var synopsisExpanded by remember { mutableStateOf(false) }
    val myList by library.myListIds.collectAsState(initial = emptySet())
    val inList = animeId in myList
    val scope = rememberCoroutineScope()

    fun loadAll() {
        loading = true
        error = null
        scope.launch {
            repository.detail(animeId)
                .onSuccess { data ->
                    runCatching {
                        anime = data
                        episodes = DemoStreams.buildEpisodes(data)
                    }.onFailure { crash = it }
                    loading = false
                }
                .onFailure { e ->
                    error = e
                    loading = false
                }
            // characters/recommendations already return Result<> (internally runCatching),
            // so a failed fetch lands in onFailure and never crashes the screen.
            repository.characters(animeId).onSuccess { characters = it }
            repository.recommendations(animeId).onSuccess { related = it }
        }
    }

    LaunchedEffect(animeId) { loadAll() }

    when {
        loading -> EpisodeListShimmer(modifier = Modifier.fillMaxSize().background(Background))
        error != null -> ErrorBox(error!!) {
            loadAll()
        }
        crash != null -> DetailCrashScreen(crash!!) { loadAll() }
        anime != null -> {
            val a = anime!!
            Box(Modifier.fillMaxSize().background(Background)) {
                // Blurred backdrop is confined to a SINGLE hero block that ends exactly at the
                // genre row. Below the genre tags everything is solid Background (Play E1, Info
                // grid, Synopsis) — never blurred. No fixed band, no black gap above the poster.
                val bgUrl = a.posterUrl ?: a.bannerUrl

                // Back button (overlay, top-left)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.45f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Scrolling content
                LazyColumn(Modifier.fillMaxSize()) {
                    // ---- HERO (blurred backdrop covers this whole item: arrow -> poster/title/rating -> genre chips) ----
                    item {
                        // Local blurred backdrop confined to the hero item only.
                        Box(Modifier.fillMaxWidth()) {
                            if (bgUrl != null) {
                                AsyncImage(
                                    model = bgUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(320.dp)
                                        .blur(6.dp)
                                        .clipToBounds()
                                )
                            } else {
                                AnivaveArt(anime = a, modifier = Modifier.fillMaxWidth().height(320.dp).blur(6.dp).clipToBounds())
                            }
                            // Fade the very bottom of the hero into solid Background so the
                            // transition to the Play E1 row (solid black) is seamless.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            1.0f to Background
                                        )
                                    )
                            )
                            // Title card + genres sit ON TOP of the blurred image (no offset hack).
                            Column(Modifier.fillMaxWidth().padding(top = 72.dp, bottom = 16.dp)) {
                                // Left mini poster card + right-side title block
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Box(
                                        Modifier
                                            .width(100.dp)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
                                            .background(SurfaceRaised)
                                    ) {
                                        AsyncImage(
                                            model = a.posterUrl,
                                            contentDescription = a.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        // Studio / source eyebrow
                                        Row(
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.4f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(5.dp).clip(CircleShape).background(Flame)
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
                                            fontSize = 24.sp,
                                            lineHeight = 28.sp,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Spacer(Modifier.height(8.dp))
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
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                // Genre pills
                                androidx.compose.foundation.lazy.LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
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
                            }
                        }
                    }

                    // ---- BELOW GENRE ROW: solid Background, never blurred ----
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Spacer(Modifier.height(8.dp))
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
                            InfoCell("SOURCE", "AniList")
                            InfoCell("STATUS", a.status ?: "—")
                            InfoCell("EPISODES", (a.episodes ?: 0).toString())
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("Synopsis", color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        val full = a.synopsis.ifBlank { "No synopsis available." }
                        val display = if (synopsisExpanded || full.length <= 160) full else full.take(160).trimEnd() + "…"
                        Text(
                            display,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (full.length > 160) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (synopsisExpanded) "Less" else "More",
                                color = Purple,
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Demo notice: episode streams are public sample videos, not licensed anime.",
                            color = TextMuted,
                            fontFamily = PlexMono,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        CharacterRow(characters = characters)
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
                item {
                    Spacer(Modifier.height(20.dp))
                    AnivaveSectionCard("More Like This", related) { onRelated(it.id) }
                    Spacer(Modifier.height(24.dp))
                }
                }
            }
        }
    }
}

@Composable
private fun DetailCrashScreen(error: Throwable, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "This anime failed to render",
            color = TextPrimary,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error.message ?: error.toString(),
            color = Flame,
            fontFamily = PlexMono,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
        SecondaryPillButton(text = "Retry", leadingIcon = Icons.Default.Refresh, onClick = onRetry)
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