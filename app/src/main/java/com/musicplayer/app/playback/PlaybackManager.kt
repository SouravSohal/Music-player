package com.musicplayer.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages music playback using ExoPlayer.
 * Provides a high-level interface for controlling playback state.
 */
@Singleton
class PlaybackManager @Inject constructor() {

    private var player: ExoPlayer? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlaybackState()
        }
    }

    /**
     * Initialize the playback manager with an ExoPlayer instance.
     * Should be called from the service.
     */
    fun initialize(exoPlayer: ExoPlayer) {
        player = exoPlayer
        player?.addListener(playerListener)
        Timber.d("PlaybackManager initialized")
    }

    /**
     * Release resources and cleanup.
     */
    fun release() {
        player?.removeListener(playerListener)
        player = null
        Timber.d("PlaybackManager released")
    }

    /**
     * Play or resume playback.
     */
    fun play() {
        player?.play()
        Timber.d("Playback started")
    }

    /**
     * Pause playback.
     */
    fun pause() {
        player?.pause()
        Timber.d("Playback paused")
    }

    /**
     * Toggle play/pause state.
     */
    fun togglePlayPause() {
        if (player?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Skip to the next track.
     */
    fun skipToNext() {
        if (player?.hasNextMediaItem() == true) {
            player?.seekToNext()
            Timber.d("Skipped to next track")
        }
    }

    /**
     * Skip to the previous track.
     */
    fun skipToPrevious() {
        if (player?.hasPreviousMediaItem() == true) {
            player?.seekToPrevious()
            Timber.d("Skipped to previous track")
        }
    }

    /**
     * Seek to a specific position in the current track.
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        Timber.d("Seeked to position: $positionMs ms")
    }

    /**
     * Set the playlist and start playback.
     * @param mediaItems List of media items to play
     * @param startIndex Index of the first item to play
     */
    fun setPlaylist(mediaItems: List<MediaItem>, startIndex: Int = 0) {
        player?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
        Timber.d("Playlist set with ${mediaItems.size} items, starting at index $startIndex")
    }

    /**
     * Add a single item to the current playlist.
     */
    fun addToQueue(mediaItem: MediaItem) {
        player?.addMediaItem(mediaItem)
        Timber.d("Added item to queue")
    }

    /**
     * Set repeat mode.
     * @param repeatMode One of Player.REPEAT_MODE_*
     */
    fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
        player?.repeatMode = repeatMode
        Timber.d("Repeat mode set to: $repeatMode")
    }

    /**
     * Set shuffle mode.
     */
    fun setShuffleMode(enabled: Boolean) {
        player?.shuffleModeEnabled = enabled
        Timber.d("Shuffle mode: $enabled")
    }

    /**
     * Get current playback position.
     */
    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0
    }

    /**
     * Get total duration of current item.
     */
    fun getDuration(): Long {
        return player?.duration ?: 0
    }

    /**
     * Update the playback state flow with current player state.
     */
    private fun updatePlaybackState() {
        val currentPlayer = player ?: return
        
        _playbackState.value = PlaybackState(
            isPlaying = currentPlayer.isPlaying,
            currentPosition = currentPlayer.currentPosition,
            duration = currentPlayer.duration,
            currentMediaItem = currentPlayer.currentMediaItem,
            playbackState = currentPlayer.playbackState,
            repeatMode = currentPlayer.repeatMode,
            shuffleMode = currentPlayer.shuffleModeEnabled
        )
    }
}

/**
 * Data class representing the current playback state.
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val currentMediaItem: MediaItem? = null,
    val playbackState: Int = Player.STATE_IDLE,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleMode: Boolean = false
)
