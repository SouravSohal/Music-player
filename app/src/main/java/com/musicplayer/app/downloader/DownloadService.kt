package com.musicplayer.app.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service for handling media downloads in the background.
 * 
 * This service ensures that downloads continue even when the app is not
 * in the foreground. Features include:
 * - Runs as a foreground service with notification
 * - Handles system interruptions (network changes, low battery)
 * - Persists across app restarts (when properly configured)
 * - Shows download progress in notification
 * - Supports multiple simultaneous downloads
 * 
 * The service automatically stops when all downloads are complete or cancelled.
 * 
 * LEGAL NOTICE:
 * This service respects user permissions and system policies.
 * Downloads are subject to the same legal restrictions as the main app.
 * Users must ensure they have rights to download content.
 * 
 * @property downloadManager Manager for download operations
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadManager: DownloadManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationManager: NotificationManager? = null
    private val activeTaskNotifications = mutableMapOf<String, Int>()
    private var nextNotificationId = NOTIFICATION_ID_START

    companion object {
        private const val CHANNEL_ID = "media_downloads"
        private const val CHANNEL_NAME = "Media Downloads"
        private const val NOTIFICATION_ID_START = 1000
        private const val SUMMARY_NOTIFICATION_ID = 999

        // Intent actions
        const val ACTION_START_DOWNLOAD = "com.musicplayer.app.action.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.musicplayer.app.action.PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.musicplayer.app.action.RESUME_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.musicplayer.app.action.CANCEL_DOWNLOAD"
        const val ACTION_CANCEL_ALL = "com.musicplayer.app.action.CANCEL_ALL"

        // Intent extras
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_URL = "url"
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_MEDIA_TYPE = "media_type"
        const val EXTRA_QUALITY = "quality"

        /**
         * Starts the download service with a new download task.
         * 
         * @param context Application context
         * @param task Download task to start
         */
        fun startDownload(context: Context, task: DownloadTask) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_URL, task.sourceUrl)
                putExtra(EXTRA_DESTINATION, task.destinationPath)
                putExtra(EXTRA_MEDIA_TYPE, task.mediaType.name)
                putExtra(EXTRA_QUALITY, task.quality.name)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Pauses a download.
         * 
         * @param context Application context
         * @param taskId Task ID to pause
         */
        fun pauseDownload(context: Context, taskId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }

        /**
         * Resumes a paused download.
         * 
         * @param context Application context
         * @param taskId Task ID to resume
         */
        fun resumeDownload(context: Context, taskId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME_DOWNLOAD
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }

        /**
         * Cancels a download.
         * 
         * @param context Application context
         * @param taskId Task ID to cancel
         */
        fun cancelDownload(context: Context, taskId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }

        /**
         * Cancels all downloads.
         * 
         * @param context Application context
         */
        fun cancelAllDownloads(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_ALL
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("DownloadService created")
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Start as foreground service
        startForeground(SUMMARY_NOTIFICATION_ID, createSummaryNotification())
        
        // Observe download state changes
        observeDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        
        // Return START_STICKY to restart service if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Handles incoming intents with download actions.
     */
    private fun handleIntent(intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)

        scope.launch {
            when (intent.action) {
                ACTION_START_DOWNLOAD -> {
                    val task = createTaskFromIntent(intent)
                    if (task != null) {
                        downloadManager.addDownload(task)
                        Timber.d("Started download: ${task.id}")
                    }
                }
                ACTION_PAUSE_DOWNLOAD -> {
                    if (taskId != null) {
                        downloadManager.pauseDownload(taskId)
                        Timber.d("Paused download: $taskId")
                    }
                }
                ACTION_RESUME_DOWNLOAD -> {
                    if (taskId != null) {
                        downloadManager.resumeDownload(taskId)
                        Timber.d("Resumed download: $taskId")
                    }
                }
                ACTION_CANCEL_DOWNLOAD -> {
                    if (taskId != null) {
                        downloadManager.cancelDownload(taskId)
                        activeTaskNotifications.remove(taskId)?.let { notifId ->
                            notificationManager?.cancel(notifId)
                        }
                        Timber.d("Cancelled download: $taskId")
                    }
                }
                ACTION_CANCEL_ALL -> {
                    downloadManager.cancelAllDownloads()
                    activeTaskNotifications.clear()
                    notificationManager?.cancelAll()
                    Timber.d("Cancelled all downloads")
                }
            }

            // Check if we should stop the service
            checkAndStopService()
        }
    }

    /**
     * Creates a download task from intent extras.
     */
    private fun createTaskFromIntent(intent: Intent): DownloadTask? {
        val url = intent.getStringExtra(EXTRA_URL) ?: return null
        val destination = intent.getStringExtra(EXTRA_DESTINATION) ?: return null
        val mediaType = intent.getStringExtra(EXTRA_MEDIA_TYPE)?.let { 
            MediaType.fromString(it) 
        } ?: MediaType.AUDIO_ONLY
        val quality = intent.getStringExtra(EXTRA_QUALITY)?.let {
            DownloadQuality.fromString(it)
        } ?: DownloadQuality.MEDIUM

        return DownloadTask(
            id = intent.getStringExtra(EXTRA_TASK_ID) ?: return null,
            sourceUrl = url,
            destinationPath = destination,
            mediaType = mediaType,
            quality = quality
        )
    }

    /**
     * Observes download state changes and updates notifications.
     */
    private fun observeDownloads() {
        scope.launch {
            downloadManager.downloadStates
                .catch { e ->
                    Timber.e(e, "Error observing downloads")
                }
                .collect { task ->
                    updateNotification(task)
                    
                    // Update summary notification
                    updateSummaryNotification()
                    
                    // Check if service should stop
                    if (task.isFinished()) {
                        checkAndStopService()
                    }
                }
        }
    }

    /**
     * Updates notification for a specific download task.
     */
    private fun updateNotification(task: DownloadTask) {
        val notificationId = activeTaskNotifications.getOrPut(task.id) {
            nextNotificationId++
        }

        val notification = when (task.status) {
            DownloadStatus.DOWNLOADING -> createDownloadingNotification(task)
            DownloadStatus.COMPLETED -> createCompletedNotification(task)
            DownloadStatus.FAILED -> createFailedNotification(task)
            DownloadStatus.PAUSED -> createPausedNotification(task)
            DownloadStatus.QUEUED -> createQueuedNotification(task)
            else -> return
        }

        notificationManager?.notify(notificationId, notification)

        // Remove notification for completed/failed/cancelled after a delay
        if (task.isFinished()) {
            scope.launch {
                delay(3000)
                notificationManager?.cancel(notificationId)
                activeTaskNotifications.remove(task.id)
            }
        }
    }

    /**
     * Creates notification channel for downloads.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of media downloads"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Creates summary notification for the foreground service.
     */
    private fun createSummaryNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Downloads")
            .setContentText("Managing downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Updates the summary notification with current download stats.
     */
    private fun updateSummaryNotification() {
        val activeCount = downloadManager.getActiveCount()
        val queueSize = downloadManager.getQueueSize()

        val text = when {
            activeCount > 0 && queueSize > 0 -> 
                "$activeCount downloading, $queueSize queued"
            activeCount > 0 -> 
                "$activeCount downloading"
            queueSize > 0 -> 
                "$queueSize queued"
            else -> 
                "No active downloads"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Downloads")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(activeCount > 0 || queueSize > 0)
            .build()

        notificationManager?.notify(SUMMARY_NOTIFICATION_ID, notification)
    }

    private fun createDownloadingNotification(task: DownloadTask): Notification {
        val pauseIntent = createPendingIntent(ACTION_PAUSE_DOWNLOAD, task.id)
        val cancelIntent = createPendingIntent(ACTION_CANCEL_DOWNLOAD, task.id)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading ${task.mediaType.displayName}")
            .setContentText("${task.formatDownloadedSize()} / ${task.formatTotalSize()} • ${task.formatSpeed()}")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, task.progress.toInt(), task.totalBytes == 0L)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .setOngoing(true)
            .build()
    }

    private fun createPausedNotification(task: DownloadTask): Notification {
        val resumeIntent = createPendingIntent(ACTION_RESUME_DOWNLOAD, task.id)
        val cancelIntent = createPendingIntent(ACTION_CANCEL_DOWNLOAD, task.id)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Paused")
            .setContentText("${task.formatDownloadedSize()} / ${task.formatTotalSize()}")
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setProgress(100, task.progress.toInt(), false)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .build()
    }

    private fun createQueuedNotification(task: DownloadTask): Notification {
        val cancelIntent = createPendingIntent(ACTION_CANCEL_DOWNLOAD, task.id)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Queued")
            .setContentText("Waiting to start")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .build()
    }

    private fun createCompletedNotification(task: DownloadTask): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(task.destinationPath.substringAfterLast('/'))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
    }

    private fun createFailedNotification(task: DownloadTask): Notification {
        val retryIntent = createPendingIntent(ACTION_RESUME_DOWNLOAD, task.id)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText(task.errorMessage ?: "Unknown error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .addAction(android.R.drawable.ic_menu_rotate, "Retry", retryIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun createPendingIntent(action: String, taskId: String): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getService(this, taskId.hashCode(), intent, flags)
    }

    /**
     * Checks if the service should stop (no active downloads).
     */
    private fun checkAndStopService() {
        if (downloadManager.getActiveCount() == 0 && downloadManager.getQueueSize() == 0) {
            Timber.d("No active downloads, stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Timber.d("DownloadService destroyed")
    }
}
