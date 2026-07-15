package com.aniwavestream.app.ui.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aniwavestream.app.data.model.Anime
import com.aniwavestream.app.ui.components.AnimePosterCard
import com.aniwavestream.app.ui.components.LoadingBox
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.ui.theme.TextSecondary
import com.aniwavestream.app.viewmodel.LibraryViewModel

@Composable
fun MyListScreen(
    viewModel: LibraryViewModel,
    onAnimeClick: (Anime) -> Unit
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Text(
            "My List",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(16.dp)
        )
        when {
            loading && items.isEmpty() -> LoadingBox()
            items.isEmpty() -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your list is empty", color = TextPrimary)
                Text("Add titles from the detail screen", color = TextSecondary)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.id }) { anime ->
                    AnimePosterCard(anime = anime, onClick = { onAnimeClick(anime) })
                }
            }
        }
    }
}
