package com.musicplayer.app.presentation.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Library screen showing songs, albums, and artists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Songs", "Albums", "Artists")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Library") }
        )
        
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (uiState) {
            is LibraryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LibraryUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (uiState as LibraryUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is LibraryUiState.Success -> {
                when (selectedTab) {
                    0 -> SongsList(songs = songs)
                    1 -> AlbumsList(albums = albums)
                    2 -> ArtistsList(artists = artists)
                }
            }
        }
    }
}

@Composable
fun SongsList(songs: List<com.musicplayer.app.domain.model.SongModel>) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No songs found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text("${song.artistName} • ${song.albumName}") },
                    trailingContent = { Text(song.getFormattedDuration()) }
                )
            }
        }
    }
}

@Composable
fun AlbumsList(albums: List<com.musicplayer.app.domain.model.AlbumModel>) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No albums found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(albums) { album ->
                ListItem(
                    headlineContent = { Text(album.name) },
                    supportingContent = { Text("${album.artistName} • ${album.songCount} songs") }
                )
            }
        }
    }
}

@Composable
fun ArtistsList(artists: List<com.musicplayer.app.domain.model.ArtistModel>) {
    if (artists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No artists found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(artists) { artist ->
                ListItem(
                    headlineContent = { Text(artist.name) },
                    supportingContent = { Text("${artist.albumCount} albums • ${artist.songCount} songs") }
                )
            }
        }
    }
}
