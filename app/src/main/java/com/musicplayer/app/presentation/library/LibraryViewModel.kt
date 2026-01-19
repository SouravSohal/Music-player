package com.musicplayer.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.domain.model.AlbumModel
import com.musicplayer.app.domain.model.ArtistModel
import com.musicplayer.app.domain.model.SongModel
import com.musicplayer.app.domain.usecase.GetAlbumsUseCase
import com.musicplayer.app.domain.usecase.GetArtistsUseCase
import com.musicplayer.app.domain.usecase.GetSongsUseCase
import com.musicplayer.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Library screen.
 * Manages the state of songs, albums, and artists in the user's library.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getSongsUseCase: GetSongsUseCase,
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val getArtistsUseCase: GetArtistsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _songs = MutableStateFlow<List<SongModel>>(emptyList())
    val songs: StateFlow<List<SongModel>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumModel>>(emptyList())
    val albums: StateFlow<List<AlbumModel>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<ArtistModel>>(emptyList())
    val artists: StateFlow<List<ArtistModel>> = _artists.asStateFlow()

    init {
        loadLibrary()
    }

    /**
     * Load all library content.
     */
    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading

            // Load songs
            getSongsUseCase().collect { result ->
                result.onSuccess { songList ->
                    _songs.value = songList
                    Timber.d("Loaded ${songList.size} songs")
                }.onFailure { error ->
                    Timber.e(error, "Error loading songs")
                    _uiState.value = LibraryUiState.Error(error.message ?: "Unknown error")
                }
            }
        }

        viewModelScope.launch {
            // Load albums
            getAlbumsUseCase().collect { result ->
                result.onSuccess { albumList ->
                    _albums.value = albumList
                    Timber.d("Loaded ${albumList.size} albums")
                }.onFailure { error ->
                    Timber.e(error, "Error loading albums")
                }
            }
        }

        viewModelScope.launch {
            // Load artists
            getArtistsUseCase().collect { result ->
                result.onSuccess { artistList ->
                    _artists.value = artistList
                    _uiState.value = LibraryUiState.Success
                    Timber.d("Loaded ${artistList.size} artists")
                }.onFailure { error ->
                    Timber.e(error, "Error loading artists")
                }
            }
        }
    }

    /**
     * Toggle favorite status of a song.
     */
    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(songId).onFailure { error ->
                Timber.e(error, "Error toggling favorite for song: $songId")
            }
        }
    }

    /**
     * Refresh the library content.
     */
    fun refresh() {
        loadLibrary()
    }
}

/**
 * UI state for the Library screen.
 */
sealed class LibraryUiState {
    object Loading : LibraryUiState()
    object Success : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
