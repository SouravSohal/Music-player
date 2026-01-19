package com.musicplayer.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import com.musicplayer.app.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages media notifications for the music player.
 * Displays playback controls and song information in the notification shade.
 */
@Singleton
class NotificationManager @Inject constructor(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val CHANNEL_NAME = "Music Playback"
        const val NOTIFICATION_ID = 1
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * Create the notification channel for playback notifications.
     * Required for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music and playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            notificationManager.createNotificationChannel(channel)
            Timber.d("Notification channel created")
        }
    }

    /**
     * Build a notification for the media session.
     * This notification will be used by the foreground service.
     * 
     * @param mediaSession The active media session
     * @return Notification to display
     */
    fun buildNotification(mediaSession: MediaSession): Notification {
        val mediaMetadata = mediaSession.player.currentMediaItem?.mediaMetadata
        
        // Create an intent to launch the app when notification is tapped
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note) // You'll need to create this icon
            .setContentTitle(mediaMetadata?.title ?: "Music Player")
            .setContentText(mediaMetadata?.artist ?: "Unknown Artist")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        return builder.build()
    }

    /**
     * Show a notification.
     */
    fun showNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancel the playback notification.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Timber.d("Notification cancelled")
    }
}
