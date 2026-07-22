package com.aniwavestream.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.model.DayAiring
import com.aniwavestream.app.data.model.NewReleaseEpisode
import com.aniwavestream.app.data.model.ScheduleDays
import com.aniwavestream.app.data.model.currentWeekPills
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
    modifier: Modifier = Modifier.width(120.dp),
    showScore: Boolean = true
) {
    Column(
        modifier
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
            // Fallback: when there is no poster image, show the title centered on the
            // gradient wash so the card reads as intentional (not broken/blank).
            if (anime.posterUrl == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        anime.title,
                        color = TextPrimary,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
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

/**
 * Ranked poster card for the "TOP 100 ANIME" row — a deliberately DIFFERENT
 * design from [AnimePosterCard]: a large rank number (1-100) is overlaid on the
 * top-left of the poster, and the anime name is printed ON the card (bottom
 * gradient strip) instead of below it. Rank tier tint: 1-3 gold, 4-10 flame,
 * 11+ muted accent.
 */
@Composable
fun AnimeRankedCard(
    anime: Anime,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(120.dp)
) {
    val rankColor = when {
        rank <= 3 -> Gold
        rank <= 10 -> Flame
        else -> TextPrimary
    }
    Column(modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
        ) {
            AnivaveArt(anime = anime, modifier = Modifier.fillMaxSize(), showImage = true)
            // Rank badge (big number, top-left — tucked into the card corner)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 5.dp, start = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    "#$rank",
                    color = rankColor,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-1).sp
                )
            }
            if (anime.score != null) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(String.format("%.1f", anime.score), color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
            // Name ON the card (bottom gradient strip) — distinct from AnimePosterCard
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                            startY = 0f, endY = 90f
                        )
                    )
                    .padding(start = 10.dp, end = 10.dp, bottom = 9.dp, top = 22.dp)
            ) {
                Text(
                    anime.title,
                    color = TextPrimary,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AnimeRankedRow(
    items: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 32.dp)
) {
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { anime ->
            val rank = items.indexOf(anime) + 1
            AnimeRankedCard(anime = anime, rank = rank, onClick = { onAnimeClick(anime) })
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
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { items.size })

    // Auto-rotate: advance the pager every 5s, but pause while the user is dragging.
    LaunchedEffect(items, pagerState) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(440.dp)
            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Swipeable pages
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val anime = items[page]
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
                            color = Cool,
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
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

        // Indicator dots (synced to pager, tap to jump)
        val scope = rememberCoroutineScope()
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { i, _ ->
                val active = i == pagerState.currentPage
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (active) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) Flame else Color.White.copy(alpha = 0.3f))
                        .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
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
    // Blurred art backdrop behind the card (home "Upcoming Anime" row only).
    // Light blur + slow Ken-Burns pan so a long cover drifts and reveals every part.
    val kb = rememberInfiniteTransition()
    val kbScale by kb.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse)
    )
    val kbX by kb.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse)
    )
    val kbY by kb.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse)
    )
    Box(
        modifier
            .width(280.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        AsyncImage(
            model = anime.bannerUrl ?: anime.posterUrl,
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = kbScale
                    scaleY = kbScale
                    translationX = kbX.dp.toPx()
                    translationY = kbY.dp.toPx()
                }
                .blur(0.6.dp)
                .clipToBounds()
        )
        Row(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Hairline, RoundedCornerShape(18.dp))
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
                    .background(Purple)
                    .border(1.dp, Purple.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("UPCOMING", color = Color.White, fontFamily = PlexMono, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                anime.title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(0f, 0f), blurRadius = 10f))
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Cool, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    anime.year?.let { "AIRING ${it}" } ?: (anime.status ?: "SOON"),
                    color = Cool,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(0f, 0f), blurRadius = 8f))
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                (anime.episodes?.let { "${it} eps" } ?: "TBA") + " · ${anime.type ?: ""}",
                color = Color.White,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(0f, 0f), blurRadius = 8f))
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
}

/**
 * Anivave "New Releases" — horizontal scroll of landscape episode tiles
 * (220x124 art, flame "time ago" badge, cool duration, flame progress bar,
 * "Ep X: Title" + rating). Mirrors anivave.html .episodes-grid.
 */
@Composable
fun AnivaveNewReleasesGrid(
    items: List<NewReleaseEpisode>,
    onItem: (NewReleaseEpisode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.anime.id to it.epNum }) { ep ->
            Column(
                Modifier
                    .width(220.dp)
                    .clickable { onItem(ep) }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                ) {
                    AnivaveArt(anime = ep.anime, modifier = Modifier.fillMaxSize())
                    // cool duration badge (bottom-right)
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(ep.duration, color = Cool, fontFamily = PlexMono, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                    // flame "time ago" badge (top-left)
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Flame)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(ep.timeAgo, color = Color.White, fontFamily = PlexMono, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                    // flame progress bar (bottom)
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(ep.progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(Flame)
                    )
                    // bottom scrim so the title strip below stays legible on any art
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                    startY = 0f, endY = 28f
                                )
                            )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${ep.epNum}: ${ep.epTitle}",
                    color = TextPrimary,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        ep.anime.title,
                        color = TextSecondary,
                        fontFamily = Bricolage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "★ ${ep.anime.score ?: "—"}",
                        color = Flame,
                        fontFamily = PlexMono,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Anivave "Weekly Schedule" — surface card (20dp radius) with a calendar
 * row of day-pills (day name + date number) and a timed list beneath.
 * Mirrors anivave.html .schedule-container.
 */
/**
 * Weekly Schedule card — redesigned.
 *
 * - Large active-day pill with the weekday + bold date, rest are compact Mon/Sun chips.
 * - Each broadcast is a clean horizontal tile: cover thumbnail (left) + time · title · EP/genre
 *   badges (right). Cover is shown full-bleed and crisp (no skew, no gradient wash).
 * - `maxRows` caps the visible tiles (home = 4). Used by both Home and WeeklyScheduleScreen.
 */
@Composable
fun AnivaveScheduleCard(
    activeDayIndex: Int,
    onDay: (Int) -> Unit,
    shows: List<DayAiring>,
    onItem: (DayAiring) -> Unit = {},
    modifier: Modifier = Modifier,
    maxRows: Int = 4
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .background(Surface)
            .padding(14.dp)
    ) {
        // --- Day selector (original: 7 equal pills, Mon + date, active highlighted) ---
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            currentWeekPills().forEachIndexed { idx, (name, num) ->
                val active = idx == activeDayIndex
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) TextPrimary else SurfaceRaised)
                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        .clickable { onDay(idx) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        name.uppercase(),
                        color = if (active) Void else TextMuted,
                        fontFamily = PlexMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "$num",
                        color = if (active) Void else TextMuted,
                        fontFamily = FontFamily.Default,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // --- Broadcast list ---
        if (shows.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceRaised)
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nothing airing on ${ScheduleDays[activeDayIndex].replaceFirstChar { it.uppercaseChar() }}",
                    color = TextMuted, fontFamily = PlexMono, fontSize = 12.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shows.take(maxRows).forEach { s -> ScheduleRow(s) { onItem(s) } }
            }
        }
    }
}

// ── Shared schedule-row geometry ──────────────────────────────────────────────
private val ROW_HEIGHT = 76.dp
private val COVER_SIZE = 54.dp
private val ROW_CORNER = 14.dp
private val ROW_PAD = 10.dp

/** A single timed broadcast row: cover thumbnail · time · title · EP/genre badges. */
@Composable
fun ScheduleRow(
    s: DayAiring,
    onItem: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clip(RoundedCornerShape(ROW_CORNER))
            .background(SurfaceRaised)
            .clickable { onItem() }
            .padding(ROW_PAD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover thumbnail (left) — crisp, full-bleed, rounded square.
        if (!s.cover.isNullOrBlank()) {
            AsyncImage(
                model = s.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = s.posterFocal,
                modifier = Modifier
                    .size(COVER_SIZE)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
        }

        // Time + title + badges.
        Column(Modifier.weight(1f)) {
            // Time (flame) + status badge on the same line.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.time, color = Flame, fontFamily = PlexMono,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                if (s.status.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.status, color = Cool, fontFamily = PlexMono,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                s.title,
                color = TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // EP badge (right) — only when known.
        if (s.episode > 0) {
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .background(Flame, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    "EP ${s.episode}", color = Void, fontFamily = PlexMono,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

