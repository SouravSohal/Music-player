package com.musicplayer.app.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.domain.model.PlaylistModel
import com.musicplayer.app.domain.model.SongModel
import com.musicplayer.app.domain.usecase.AddToPlaylistUseCase
import com.musicplayer.app.domain.usecase.CreatePlaylistUseCase
import com.musicplayer.app.domain.usecase.DeletePlaylistUseCase
import com.musicplayer.app.domain.usecase.GetPlaylistSongsUseCase
import com.musicplayer.app.domain.usecase.GetPlaylistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing playlists.
 */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val getPlaylistSongsUseCase: GetPlaylistSongsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val addToPlaylistUseCase: AddToPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<PlaylistModel>>(emptyList())
    val playlists: StateFlow<List<PlaylistModel>> = _playlists.asStateFlow()

    private val _currentPlaylistSongs = MutableStateFlow<List<SongModel>>(emptyList())
    val currentPlaylistSongs: StateFlow<List<SongModel>> = _currentPlaylistSongs.asStateFlow()

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Idle)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    /**
     * Load all playlists.
     */
    fun loadPlaylists() {
        viewModelScope.launch {
            getPlaylistsUseCase().collect { result ->
                result.onSuccess { playlistList ->
                    _playlists.value = playlistList
                    Timber.d("Loaded ${playlistList.size} playlists")
                }.onFailure { error ->
                    Timber.e(error, "Error loading playlists")
                    _uiState.value = PlaylistUiState.Error(error.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Load songs for a specific playlist.
     */
    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            getPlaylistSongsUseCase(playlistId).collect { result ->
                result.onSuccess { songs ->
                    _currentPlaylistSongs.value = songs
                    Timber.d("Loaded ${songs.size} songs for playlist $playlistId")
                }.onFailure { error ->
                    Timber.e(error, "Error loading playlist songs")
                    _uiState.value = PlaylistUiState.Error(error.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Create a new playlist.
     */
    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            
            createPlaylistUseCase(name, description)
                .onSuccess { playlistId ->
                    _uiState.value = PlaylistUiState.PlaylistCreated(playlistId)
                    Timber.d("Created playlist: $name with id: $playlistId")
                }
                .onFailure { error ->
                    _uiState.value = PlaylistUiState.Error(error.message ?: "Failed to create playlist")
                    Timber.e(error, "Error creating playlist")
                }
        }
    }

    /**
     * Add a song to a playlist.
     */
    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            addToPlaylistUseCase(playlistId, songId)
                .onSuccess {
                    _uiState.value = PlaylistUiState.SongAdded
                    Timber.d("Added song $songId to playlist $playlistId")
                }
                .onFailure { error ->
                    _uiState.value = PlaylistUiState.Error(error.message ?: "Failed to add song")
                    Timber.e(error, "Error adding song to playlist")
                }
        }
    }

    /**
     * Delete a playlist.
     */
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            deletePlaylistUseCase(playlistId)
                .onSuccess {
                    _uiState.value = PlaylistUiState.PlaylistDeleted
                    Timber.d("Deleted playlist $playlistId")
                }
                .onFailure { error ->
                    _uiState.value = PlaylistUiState.Error(error.message ?: "Failed to delete playlist")
                    Timber.e(error, "Error deleting playlist")
                }
        }
    }

    /**
     * Reset UI state.
     */
    fun resetUiState() {
        _uiState.value = PlaylistUiState.Idle
    }
}

/**
 * UI state for playlist operations.
 */
sealed class PlaylistUiState {
    object Idle : PlaylistUiState()
    object Loading : PlaylistUiState()
    data class PlaylistCreated(val playlistId: Long) : PlaylistUiState()
    object SongAdded : PlaylistUiState()
    object PlaylistDeleted : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}
