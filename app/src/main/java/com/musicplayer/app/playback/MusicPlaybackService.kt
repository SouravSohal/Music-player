package com.musicplayer.app.playback

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * MediaSessionService for handling music playback.
 * This service manages the media session and playback lifecycle.
 * 
 * Runs as a foreground service when playing music to ensure playback
 * continues even when the app is in the background.
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the playback manager with the player
        playbackManager.initialize(player)
        
        Timber.d("MusicPlaybackService created")
    }

    /**
     * Return the media session for this service.
     * Called by the system to get the session for media controls.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        // Stop the service if the player is not playing
        if (!player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Release resources
        playbackManager.release()
        mediaSession.release()
        player.release()
        
        Timber.d("MusicPlaybackService destroyed")
    }
}
