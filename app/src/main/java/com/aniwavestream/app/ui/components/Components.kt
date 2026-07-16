package com.aniwavestream.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.Character
import com.aniwavestream.app.data.model.Episode
import com.aniwavestream.app.ui.theme.AnivaveArt
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.Bricolage
import com.aniwavestream.app.ui.theme.Cool
import com.aniwavestream.app.ui.theme.Flame
import com.aniwavestream.app.ui.theme.Gold
import com.aniwavestream.app.ui.theme.Hairline
import com.aniwavestream.app.ui.theme.OrangeGlow
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.PlexMono
import com.aniwavestream.app.ui.theme.Purple
import com.aniwavestream.app.ui.theme.Surface
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.SurfaceRaised
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import com.aniwavestream.app.ui.theme.Void

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    // Rule 5: never a bare spinner — fall back to structural shimmer.
    HomeShimmer(modifier.fillMaxSize().background(Background))
}

/**
 * Rule 4 — Consumer-grade full-screen error state.
 * Expressive vector illustration + friendly copy + prominent accent Retry button.
 * Accepts either a raw [Throwable] or a legacy message string; both are mapped
 * through [toFriendlyError] so no raw code ever reaches the layout.
 */
@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    FriendlyErrorScreen(message.toFriendlyError(), onRetry)
}

@Composable
fun ErrorBox(error: Throwable, onRetry: (() -> Unit)? = null) {
    FriendlyErrorScreen(error.toFriendlyError(), onRetry)
}

@Composable
fun FriendlyErrorScreen(error: FriendlyError, onRetry: (() -> Unit)?) {
    val icon: ImageVector = when (error.icon) {
        FriendlyErrorIcon.OFFLINE -> Icons.Default.CloudOff
        FriendlyErrorIcon.TIMEOUT -> Icons.Default.HourglassEmpty
        FriendlyErrorIcon.NOT_FOUND -> Icons.Default.SearchOff
        FriendlyErrorIcon.RATE_LIMIT -> Icons.Default.Speed
        FriendlyErrorIcon.GENERIC -> Icons.Default.SentimentDissatisfied
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(OrangeGlow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            error.title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(28.dp))
            PrimaryPillButton(text = "Retry", onClick = onRetry, leadingIcon = Icons.Default.Refresh)
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        if (onSeeAll != null) {
            Text(
                "See all",
                color = OrangePrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showScore: Boolean = true
) {
    Column(
        modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
        ) {
            AnivaveArt(
                anime = anime,
                modifier = Modifier.fillMaxSize(),
                showImage = true
            )
            if (showScore && anime.score != null) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        String.format("%.1f", anime.score),
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                anime.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(
                onClick = { /* TODO: quick actions (add to list / share) */ },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AnimeRow(
    items: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 32.dp)
) {
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { anime ->
            AnimePosterCard(anime = anime, onClick = { onAnimeClick(anime) })
        }
    }
}

@Composable
fun ContinueCard(
    anime: Anime,
    episode: Int,
    progress: Float,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .background(SurfaceRaised)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 72.dp, height = 100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Background)
        ) {
            AsyncImage(
                model = anime.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                anime.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text("E$episode · Continue", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = OrangePrimary,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
    }
}

/**
 * Rule 3 — Polished media / episode card.
 * Horizontal Coil thumbnail with an integrated watch-progress bar pinned to the
 * bottom edge of the image, plus a cleanly stacked metadata column on the right.
 * Never a plain text block or bare icon row.
 */
@Composable
fun EpisodeCard(
    episode: Episode,
    thumbnailUrl: String?,
    watchProgress: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .background(SurfaceRaised)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(140.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Background)
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Play affordance
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Episode chip
            Text(
                "E${episode.number}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Flame)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            // Integrated watch-progress bar on the bottom edge of the image
            if (watchProgress > 0f) {
                LinearProgressIndicator(
                    progress = { watchProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Flame,
                    trackColor = Color.Black.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Episode ${episode.number}",
                color = Flame,
                fontFamily = PlexMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                episode.title,
                color = TextPrimary,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                episode.durationLabel,
                color = TextMuted,
                fontFamily = PlexMono,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Rule 1 — Immersive hero header.
 * Edge-to-edge artwork that bleeds under the translucent status bar, a
 * bottom-anchored vertical gradient scrim for text legibility, and a Crossfade
 * from shimmer placeholder to the loaded artwork (Rule 7).
 */
@Composable
fun HeroBanner(
    anime: Anime,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    pageCount: Int = 5,
    selectedPage: Int = 0
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(460.dp)
    ) {
        AsyncImage(
            model = anime.bannerUrl ?: anime.posterUrl,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Vertical scrim over the bottom half for deeply readable overlay text.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF121212))
                    )
                )
        )
        // Top scrim so the transparent header (ANIVAVE / Premium demo) stays
        // legible over ANY banner brightness — not just dark ones.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF121212).copy(alpha = 0.85f), Color.Transparent)
                    )
                )
        )
        // Transparent app header bleeding under the status bar (no top app bar).
        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
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
            Text("Premium demo", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Text(
                "FEATURED",
                color = OrangePrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                anime.title,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 0.7f),
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            val meta = listOfNotNull(
                anime.score?.let { "★ ${String.format("%.1f", it)}" },
                anime.year?.toString(),
                anime.type,
                anime.episodes?.let { "$it eps" }
            ).joinToString("  ·  ")
            Text(meta, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                anime.synopsis,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryPillButton(text = "Start Watching E1", onClick = onPlay, leadingIcon = Icons.Default.PlayArrow)
                SecondaryPillButton(text = "Details", onClick = onDetails)
            }
            Spacer(Modifier.height(14.dp))
            // Carousel indicator: short horizontal pill segments signalling a swipeable banner.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pageCount) { index ->
                    val selected = index == selectedPage
                    Box(
                        Modifier
                            .height(4.dp)
                            .width(if (selected) 22.dp else 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) OrangePrimary else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Rules 2 & 7 — Premium Material 3 pill button with spring press physics.
 * RoundedCornerShape(50) pill, brand-orange fill, springs down on press.
 */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "primary-press"
    )
    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(OrangePrimary)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "secondary-press"
    )
    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Text(text, color = TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Anivave signature — auto-rotating premium hero slider.
 * Cycles through [items] every 5s with crossfade, shows indicator dots and a
 * tagline eyebrow, and a flame "Watch Now" + ghost "Watchlist" action row.
 */
@Composable
fun AnivaveHeroSlider(
    items: List<Anime>,
    onPlay: (Anime) -> Unit,
    onDetails: (Anime) -> Unit,
    onWatchlist: (Anime) -> Unit
) {
    if (items.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, items.lastIndex)

    LaunchedEffect(items) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(5000)
            index = (index + 1) % items.size
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(440.dp)
            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Slides
        items.forEachIndexed { i, anime ->
            val visible = i == safeIndex
            androidx.compose.animation.Crossfade(
                targetState = visible,
                animationSpec = androidx.compose.animation.core.tween(800),
                modifier = Modifier.fillMaxSize()
            ) { show ->
                if (show) {
                    Box(Modifier.fillMaxSize()) {
                        AnivaveArt(anime = anime, modifier = Modifier.fillMaxSize())
                        // Top scrim for header legibility
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF08080A).copy(alpha = 0.85f), Color.Transparent),
                                    startY = 0f, endY = 240f
                                )
                            )
                        )
                        // App header bleeding under status bar
                        Row(
                            Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "aniva",
                                color = TextPrimary,
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.6).sp
                            )
                        }
                        // Bottom content
                        Column(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 18.dp, end = 18.dp, bottom = 20.dp)
                        ) {
                            // Tagline eyebrow
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
                                    anime.studios.firstOrNull() ?: "FEATURED",
                                    color = Flame,
                                    fontFamily = PlexMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                anime.title,
                                color = TextPrimary,
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                lineHeight = 30.sp,
                                letterSpacing = (-0.5).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(13.dp), tint = Gold)
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    anime.score?.let { "%.1f".format(it) } ?: "—",
                                    color = Gold,
                                    fontFamily = PlexMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${anime.year ?: ""} · ${anime.type ?: ""}",
                                    color = TextSecondary,
                                    fontFamily = PlexMono,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrimaryPillButton(text = "Watch Now", onClick = { onPlay(anime) }, leadingIcon = Icons.Default.PlayArrow)
                                SecondaryPillButton(text = "Watchlist", onClick = { onWatchlist(anime) })
                            }
                        }
                    }
                }
            }
        }

        // Indicator dots
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { i, _ ->
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (i == safeIndex) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (i == safeIndex) Flame else Color.White.copy(alpha = 0.3f))
                        .clickable { index = i }
                )
            }
        }
    }
}

/**
 * Anivave category chip row. Calls [onSelect] with the chosen category (or null
 * for "all"). Highlights the active chip with a solid flame-on-light look.
 */
@Composable
fun AnivaveChipRow(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AnivaveChip("All", selected == null) { onSelect(null) }
        }
        items(categories, key = { it }) { cat ->
            AnivaveChip(cat, selected == cat) { onSelect(cat) }
        }
    }
}

@Composable
private fun AnivaveChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (active) Flame else Hairline, RoundedCornerShape(10.dp))
            .background(if (active) Flame else Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (active) Void else TextSecondary,
            fontFamily = PlexMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp
        )
    }
}

/**
 * Anivave "section" — a titled card holding a horizontal strip of gradient
 * mini-posters. Used for New Releases / Upcoming rows.
 */
@Composable
fun AnivaveSectionCard(
    title: String,
    items: List<Anime>,
    onItem: (Anime) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        Spacer(Modifier.height(4.dp))
        AnivaveSectionRow(items, onItem)
        Spacer(Modifier.height(8.dp))
    }
}

/** Header-less variant (caller renders its own header). */
@Composable
fun AnivaveSectionCardNoHeader(
    items: List<Anime>,
    onItem: (Anime) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        AnivaveSectionRow(items, onItem)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AnivaveSectionRow(
    items: List<Anime>,
    onItem: (Anime) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            "Nothing here yet.",
            color = TextSecondary,
            fontFamily = PlexMono,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { anime ->
                Column(
                    Modifier
                        .width(130.dp)
                        .clickable { onItem(anime) }
                ) {
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
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = PlexMono,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Anivave character row — circular gradient portraits with name (same art style).
 */
@Composable
fun CharacterRow(
    characters: List<Character>,
    onItem: (Character) -> Unit = {}
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader("Key Characters")
        Spacer(Modifier.height(6.dp))
        if (characters.isEmpty()) {
            Text(
                "No character data.",
                color = TextSecondary,
                fontFamily = PlexMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(characters, key = { it.id }) { ch ->
                    Column(
                        Modifier
                            .width(86.dp)
                            .clickable { onItem(ch) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .border(2.dp, Flame.copy(alpha = 0.7f), CircleShape)
                        ) {
                            if (ch.imageUrl != null) {
                                AsyncImage(
                                    model = ch.imageUrl,
                                    contentDescription = ch.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(SurfaceRaised))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            ch.name,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = PlexMono,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        ch.role?.let {
                            Text(
                                it,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = PlexMono,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Anivave "Upcoming Anime" card — 280-style portrait card with purple
 * UPCOMING badge, cool countdown text, and a bell alert toggle.
 */
@Composable
fun AnivaveUpcomingCard(
    anime: Anime,
    onItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    var alertOn by remember { mutableStateOf(false) }
    Row(
        modifier
            .width(280.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .background(SurfaceRaised.copy(alpha = 0.6f))
            .clickable { onItem() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Portrait art
        Box(
            Modifier
                .size(74.dp, 100.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Hairline, RoundedCornerShape(10.dp))
        ) {
            AnivaveArt(anime = anime, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Purple.copy(alpha = 0.15f))
                    .border(1.dp, Purple.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("UPCOMING", color = Purple, fontFamily = PlexMono, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                anime.title,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Cool, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    anime.year?.let { "AIRING ${it}" } ?: (anime.status ?: "SOON"),
                    color = Cool,
                    fontFamily = PlexMono,
                    fontSize = 9.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                (anime.episodes?.let { "${it} eps" } ?: "TBA") + " · ${anime.type ?: ""}",
                color = TextSecondary,
                fontFamily = PlexMono,
                fontSize = 9.sp
            )
        }
        // Bell alert toggle
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Hairline, RoundedCornerShape(8.dp))
                .background(if (alertOn) Flame.copy(alpha = 0.15f) else SurfaceRaised)
                .clickable { alertOn = !alertOn },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Alert",
                tint = if (alertOn) Flame else TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Anivave "Weekly Schedule" — surface card with timed list of shows.
 */
@Composable
fun AnivaveScheduleCard(
    dayLabel: String,
    shows: List<Anime>,
    onItem: (Anime) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .background(SurfaceRaised.copy(alpha = 0.6f))
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dayLabel, color = TextPrimary, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Cool.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("JST BROADCAST", color = Cool, fontFamily = PlexMono, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (shows.isEmpty()) {
            Text("No airing shows for this day.", color = TextSecondary, fontFamily = PlexMono, fontSize = 12.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shows.take(8).forEach { a ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                            .background(Background.copy(alpha = 0.5f))
                            .clickable { onItem(a) }
                            .padding(8.dp, 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "JST",
                            color = Flame,
                            fontFamily = PlexMono,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(44.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(a.title, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(a.type ?: "Airing", color = TextMuted, fontFamily = PlexMono, fontSize = 8.5.sp)
                        }
                        Text("●", color = Cool, fontSize = 10.sp, fontFamily = PlexMono)
                    }
                }
            }
        }
    }
}

/**
 * Anivave "New Releases" — 2-column grid of landscape tiles.
 */
@Composable
fun AnivaveNewReleasesGrid(
    items: List<Anime>,
    onItem: (Anime) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Text("No new releases right now.", color = TextSecondary, fontFamily = PlexMono, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
    } else {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.take(6).forEach { anime ->
                Column(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .clickable { onItem(anime) }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                    ) {
                        AnivaveArt(anime = anime, modifier = Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                            Text("NEW", color = Flame, fontFamily = PlexMono, fontSize = 8.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Flame.copy(alpha = 0.2f)).border(1.dp, Flame.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(3.dp, 1.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(anime.title, color = TextPrimary, fontSize = 12.5.sp, fontFamily = PlexMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
