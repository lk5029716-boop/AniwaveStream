package com.aniwavestream.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aniwavestream.app.data.repository.AnimeRepository
import com.aniwavestream.app.data.repository.UserLibraryStore
import com.aniwavestream.app.ui.browse.BrowseScreen
import com.aniwavestream.app.ui.detail.DetailScreen
import com.aniwavestream.app.ui.home.HomeScreen
import com.aniwavestream.app.ui.mylist.MyListScreen
import com.aniwavestream.app.ui.player.PlayerScreen
import com.aniwavestream.app.ui.profile.ProfileScreen
import com.aniwavestream.app.ui.schedule.ScheduleScreen
import com.aniwavestream.app.ui.search.SearchScreen
import com.aniwavestream.app.ui.theme.Background
import com.aniwavestream.app.ui.theme.OrangePrimary
import com.aniwavestream.app.ui.theme.SurfaceDark
import com.aniwavestream.app.ui.theme.TextMuted
import com.aniwavestream.app.ui.theme.TextPrimary
import com.aniwavestream.app.viewmodel.HomeViewModel
import com.aniwavestream.app.viewmodel.LibraryViewModel

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Browse : Route("browse")
    data object Search : Route("search")
    data object Schedule : Route("schedule")
    data object MyList : Route("mylist")
    data object Profile : Route("profile")
    data object Detail : Route("detail/{id}") {
        fun create(id: Int) = "detail/$id"
    }
    data object Player : Route("player/{id}/{ep}") {
        fun create(id: Int, ep: Int) = "player/$id/$ep"
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val selected: ImageVector,
    val unselected: ImageVector
)

private val tabs = listOf(
    Tab(Route.Home.path, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Route.Browse.path, "Browse", Icons.Filled.GridView, Icons.Outlined.GridView),
    Tab(Route.Search.path, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    Tab(Route.Schedule.path, "Schedule", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    Tab(Route.MyList.path, "My List", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    Tab(Route.Profile.path, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun AniwaveNavHost(
    repository: AnimeRepository,
    library: UserLibraryStore
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = tabs.any { it.route == current }

    val homeVm: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(repository, library)
    )
    val libraryVm: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(repository, library)
    )

    fun openDetail(id: Int) = nav.navigate(Route.Detail.create(id))
    fun openPlayer(id: Int, ep: Int) = nav.navigate(Route.Player.create(id, ep))

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = SurfaceDark) {
                    tabs.forEach { tab ->
                        val selected = current == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.selected else tab.unselected,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OrangePrimary,
                                selectedTextColor = OrangePrimary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    viewModel = homeVm,
                    onAnimeClick = { openDetail(it.id) },
                    onPlay = { openPlayer(it.id, 1) },
                    onContinue = { anime, ep -> openPlayer(anime.id, ep) }
                )
            }
            composable(Route.Browse.path) {
                BrowseScreen(
                    repository = repository,
                    onAnimeClick = { openDetail(it.id) }
                )
            }
            composable(Route.Search.path) {
                SearchScreen(
                    repository = repository,
                    onAnimeClick = { openDetail(it.id) }
                )
            }
            composable(Route.Schedule.path) {
                ScheduleScreen(
                    repository = repository,
                    onAnimeClick = { openDetail(it.id) }
                )
            }
            composable(Route.MyList.path) {
                MyListScreen(
                    viewModel = libraryVm,
                    onAnimeClick = { openDetail(it.id) }
                )
            }
            composable(Route.Profile.path) {
                ProfileScreen()
            }
            composable(
                Route.Detail.path,
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                DetailScreen(
                    animeId = id,
                    repository = repository,
                    library = library,
                    onBack = { nav.popBackStack() },
                    onPlay = { ep -> openPlayer(id, ep) },
                    onRelated = { openDetail(it) }
                )
            }
            composable(
                Route.Player.path,
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("ep") { type = NavType.IntType }
                )
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                val ep = entry.arguments?.getInt("ep") ?: 1
                PlayerScreen(
                    animeId = id,
                    episode = ep,
                    title = repository.cached(id)?.title ?: "",
                    repository = repository,
                    library = library,
                    onBack = { nav.popBackStack() },
                    onEpisodeChange = { next ->
                        nav.navigate(Route.Player.create(id, next)) {
                            popUpTo(Route.Player.create(id, ep)) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
