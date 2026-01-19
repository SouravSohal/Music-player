package com.musicplayer.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicplayer.app.R
import com.musicplayer.app.presentation.library.LibraryScreen
import com.musicplayer.app.presentation.player.PlayerScreen
import com.musicplayer.app.presentation.playlist.PlaylistScreen
import com.musicplayer.app.presentation.settings.SettingsScreen
import com.musicplayer.app.presentation.theme.MusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Music Player app.
 * Sets up the Compose UI and navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MusicPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayerApp()
                }
            }
        }
    }
}

/**
 * Main composable for the app, sets up navigation and bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen()
            }
            composable(Screen.Player.route) {
                PlayerScreen()
            }
            composable(Screen.Playlists.route) {
                PlaylistScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

/**
 * Bottom navigation bar composable.
 */
@Composable
fun BottomNavigationBar(
    onNavigate: (String) -> Unit,
    currentRoute: String?
) {
    val items = listOf(
        Screen.Library,
        Screen.Player,
        Screen.Playlists,
        Screen.Settings
    )
    
    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) }
            )
        }
    }
}

/**
 * Sealed class representing navigation destinations.
 */
sealed class Screen(val route: String, val title: String, val icon: Int) {
    object Library : Screen("library", "Library", R.drawable.ic_library)
    object Player : Screen("player", "Player", R.drawable.ic_play)
    object Playlists : Screen("playlists", "Playlists", R.drawable.ic_playlist)
    object Settings : Screen("settings", "Settings", R.drawable.ic_settings)
}
