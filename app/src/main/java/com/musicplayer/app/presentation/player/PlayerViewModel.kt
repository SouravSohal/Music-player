package com.musicplayer.app.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.musicplayer.app.domain.model.SongModel
import com.musicplayer.app.playback.PlaybackManager
import com.musicplayer.app.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Player screen.
 * Manages playback state and controls.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    private val _currentQueue = MutableStateFlow<List<SongModel>>(emptyList())
    val currentQueue: StateFlow<List<SongModel>> = _currentQueue.asStateFlow()

    /**
     * Play a list of songs starting from a specific index.
     */
    fun playSongs(songs: List<SongModel>, startIndex: Int = 0) {
        viewModelScope.launch {
            _currentQueue.value = songs
            
            val mediaItems = songs.map { song ->
                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(song.uri)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artistName)
                            .setAlbumTitle(song.albumName)
                            .setArtworkUri(song.artworkUri?.let { android.net.Uri.parse(it) })
                            .build()
                    )
                    .build()
            }
            
            playbackManager.setPlaylist(mediaItems, startIndex)
            Timber.d("Playing ${songs.size} songs starting at index $startIndex")
        }
    }

    /**
     * Play a single song.
     */
    fun playSong(song: SongModel) {
        playSongs(listOf(song), 0)
    }

    /**
     * Toggle play/pause.
     */
    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    /**
     * Skip to next track.
     */
    fun skipToNext() {
        playbackManager.skipToNext()
    }

    /**
     * Skip to previous track.
     */
    fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    /**
     * Seek to position in current track.
     */
    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    /**
     * Set repeat mode.
     */
    fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
        playbackManager.setRepeatMode(repeatMode)
    }

    /**
     * Toggle shuffle mode.
     */
    fun toggleShuffle() {
        val currentState = playbackState.value.shuffleMode
        playbackManager.setShuffleMode(!currentState)
    }

    /**
     * Add song to play queue.
     */
    fun addToQueue(song: SongModel) {
        viewModelScope.launch {
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artistName)
                        .setAlbumTitle(song.albumName)
                        .build()
                )
                .build()
            
            playbackManager.addToQueue(mediaItem)
            Timber.d("Added ${song.title} to queue")
        }
    }
}
