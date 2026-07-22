package com.aniwavestream.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.ui.components.LoadingBox
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceElevated
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    repository: AnimeRepository,
    onAnimeClick: (Anime) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

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
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated
            )
        )
        Spacer(Modifier.height(12.dp))
        when {
            loading -> LoadingBox()
            query.isBlank() -> BoxHint("Find titles, genres vibes, or classics")
            error != null -> BoxHint(error!!)
            results.isEmpty() -> BoxHint("No results for \"$query\"")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results, key = { it.id }) { anime ->
                    SearchRow(anime) { onAnimeClick(anime) }
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

@Composable
private fun SearchRow(anime: Anime, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalDensity.current)
                .data(anime.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(8.dp))
                .aspectRatio(2f / 3f, matchHeightConstraintsFirst = true)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                anime.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(
                anime.type,
                anime.year?.toString(),
                anime.score?.let { "★ ${"%.1f".format(it)}" }
            ).joinToString(" · ")
            Text(meta, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            if (anime.genres.isNotEmpty()) {
                Text(
                    anime.genres.take(3).joinToString(", "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
