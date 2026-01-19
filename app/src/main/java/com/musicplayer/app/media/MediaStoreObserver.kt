package com.musicplayer.app.media

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes changes to the MediaStore and provides notifications when media files are added,
 * removed, or modified.
 * 
 * This class handles:
 * - Monitoring MediaStore for changes using ContentObserver
 * - Detecting when new media files are added
 * - Triggering automatic re-scanning when changes occur
 * - Providing Flow of media change events
 * - Supporting granular change detection (audio, video, images)
 * 
 * Usage:
 * ```
 * mediaStoreObserver.observeMediaChanges()
 *     .collect { event ->
 *         when (event) {
 *             is MediaChangeEvent.MediaAdded -> // Handle new media
 *             is MediaChangeEvent.MediaDeleted -> // Handle deleted media
 *             is MediaChangeEvent.MediaModified -> // Handle modified media
 *         }
 *     }
 * ```
 * 
 * @property context Application context
 * @property contentResolver Content resolver for accessing MediaStore
 */
@Singleton
class MediaStoreObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Represents different types of media change events.
     */
    sealed class MediaChangeEvent {
        /**
         * Fired when new media is added to the MediaStore.
         * 
         * @property uri URI of the media content that changed
         * @property mediaType Type of media (audio, video, image)
         */
        data class MediaAdded(
            val uri: Uri,
            val mediaType: MediaType
        ) : MediaChangeEvent()

        /**
         * Fired when media is deleted from the MediaStore.
         * 
         * @property uri URI of the media content that changed
         * @property mediaType Type of media (audio, video, image)
         */
        data class MediaDeleted(
            val uri: Uri,
            val mediaType: MediaType
        ) : MediaChangeEvent()

        /**
         * Fired when media metadata is modified in the MediaStore.
         * 
         * @property uri URI of the media content that changed
         * @property mediaType Type of media (audio, video, image)
         */
        data class MediaModified(
            val uri: Uri,
            val mediaType: MediaType
        ) : MediaChangeEvent()

        /**
         * Fired when a general change occurs that can't be classified.
         * Used as a fallback to trigger a full rescan.
         * 
         * @property uri URI of the media content that changed
         */
        data class UnknownChange(val uri: Uri) : MediaChangeEvent()
    }

    /**
     * Types of media content that can be observed.
     */
    enum class MediaType {
        AUDIO,
        VIDEO,
        IMAGE,
        ALL
    }

    /**
     * Observes changes to audio files in the MediaStore.
     * 
     * This method creates a Flow that emits events whenever audio files are added,
     * removed, or modified in the MediaStore. The observer is automatically
     * registered and unregistered based on Flow collection.
     * 
     * @param notifyForDescendants If true, also observes changes to descendants of the URI
     * @return Flow of MediaChangeEvent
     */
    fun observeAudioChanges(notifyForDescendants: Boolean = true): Flow<MediaChangeEvent> {
        return observeChanges(
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.AUDIO,
            notifyForDescendants = notifyForDescendants
        )
    }

    /**
     * Observes changes to all media files (audio, video, images) in the MediaStore.
     * 
     * @param notifyForDescendants If true, also observes changes to descendants of the URI
     * @return Flow of MediaChangeEvent
     */
    fun observeAllMediaChanges(notifyForDescendants: Boolean = true): Flow<MediaChangeEvent> {
        return observeChanges(
            uri = MediaStore.Files.getContentUri("external"),
            mediaType = MediaType.ALL,
            notifyForDescendants = notifyForDescendants
        )
    }

    /**
     * Observes changes to a specific MediaStore URI.
     * 
     * This is the core observation method that creates a ContentObserver
     * and wraps it in a Flow for reactive consumption.
     * 
     * @param uri MediaStore URI to observe
     * @param mediaType Type of media being observed
     * @param notifyForDescendants Whether to observe changes to descendants
     * @return Flow of MediaChangeEvent
     */
    private fun observeChanges(
        uri: Uri,
        mediaType: MediaType,
        notifyForDescendants: Boolean
    ): Flow<MediaChangeEvent> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())
        var previousCount = getMediaCount(uri)
        
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                try {
                    Timber.d("MediaStore change detected: uri=$uri, selfChange=$selfChange")
                    
                    val currentCount = getMediaCount(this@MediaStoreObserver.uri)
                    val event = when {
                        uri == null -> MediaChangeEvent.UnknownChange(this@MediaStoreObserver.uri)
                        currentCount > previousCount -> {
                            previousCount = currentCount
                            MediaChangeEvent.MediaAdded(uri, mediaType)
                        }
                        currentCount < previousCount -> {
                            previousCount = currentCount
                            MediaChangeEvent.MediaDeleted(uri, mediaType)
                        }
                        else -> MediaChangeEvent.MediaModified(uri, mediaType)
                    }
                    
                    trySend(event).isSuccess.also { success ->
                        if (!success) {
                            Timber.w("Failed to send media change event")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing media change")
                }
            }

            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }
        }

        try {
            contentResolver.registerContentObserver(uri, notifyForDescendants, observer)
            Timber.d("MediaStore observer registered for URI: $uri")
        } catch (e: Exception) {
            Timber.e(e, "Error registering MediaStore observer")
            close(e)
        }

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
                Timber.d("MediaStore observer unregistered for URI: $uri")
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering MediaStore observer")
            }
        }
    }.distinctUntilChanged()

    /**
     * Creates a Flow that emits a signal whenever audio media changes.
     * 
     * This is a simplified version that just emits Unit whenever a change occurs,
     * useful for triggering rescans without needing detailed event information.
     * 
     * @return Flow that emits Unit on each change
     */
    fun observeAudioChangesSimple(): Flow<Unit> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())
        
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                Timber.d("Audio media change detected")
                trySend(Unit)
            }
        }

        try {
            contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            Timber.d("Simple audio observer registered")
        } catch (e: Exception) {
            Timber.e(e, "Error registering simple audio observer")
            close(e)
        }

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
                Timber.d("Simple audio observer unregistered")
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering simple audio observer")
            }
        }
    }

    /**
     * Gets the current count of media items at the specified URI.
     * 
     * This is used to detect additions and deletions by comparing counts
     * before and after a change event.
     * 
     * @param uri MediaStore URI to query
     * @return Count of media items
     */
    private fun getMediaCount(uri: Uri): Int {
        return try {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.count
            } ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Error getting media count for URI: $uri")
            0
        }
    }

    /**
     * Checks if media scanning is currently in progress.
     * 
     * The system's media scanner may be running in the background,
     * and it's useful to know this to avoid concurrent scans.
     * 
     * @return true if media scan is in progress
     */
    fun isMediaScannerScanning(): Boolean {
        return try {
            contentResolver.query(
                MediaStore.getMediaScannerUri(),
                null,
                null,
                null,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error checking media scanner status")
            false
        }
    }

    /**
     * Manually triggers a media scan for a specific file or directory.
     * 
     * This notifies the MediaStore to scan a specific path, useful after
     * downloading or copying new media files.
     * 
     * Note: This requires the file path, not a content URI.
     * 
     * @param path Absolute file path to scan
     */
    fun scanFile(path: String) {
        try {
            val uri = Uri.parse("file://$path")
            context.sendBroadcast(
                android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
            )
            Timber.d("Triggered media scan for: $path")
        } catch (e: Exception) {
            Timber.e(e, "Error triggering media scan for: $path")
        }
    }

    /**
     * Triggers a full media rescan.
     * 
     * This broadcasts an intent to rescan all media on external storage.
     * Note: This is a system-wide operation and may take time.
     * 
     * WARNING: This method is deprecated on Android 11+ (API 30+) and may not work.
     * Consider using MediaScannerConnection.scanFile() for specific files instead.
     */
    @Deprecated("Use scanFile() for specific files instead. Full rescans are deprecated on Android 11+")
    fun triggerFullMediaRescan() {
        try {
            context.sendBroadcast(
                android.content.Intent(android.content.Intent.ACTION_MEDIA_MOUNTED).apply {
                    data = Uri.parse("file://${android.os.Environment.getExternalStorageDirectory()}")
                }
            )
            Timber.d("Triggered full media rescan")
        } catch (e: Exception) {
            Timber.e(e, "Error triggering full media rescan")
        }
    }

    /**
     * Gets the timestamp of the last media change.
     * 
     * This queries the most recently modified media file to determine
     * when the last change occurred.
     * 
     * @param mediaType Type of media to check (default: AUDIO)
     * @return Timestamp in milliseconds, or 0 if no media found
     */
    fun getLastMediaChangeTimestamp(mediaType: MediaType = MediaType.AUDIO): Long {
        val uri = when (mediaType) {
            MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.ALL -> MediaStore.Files.getContentUri("external")
        }

        return try {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    if (modifiedIndex != -1) {
                        cursor.getLong(modifiedIndex) * 1000 // Convert to milliseconds
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            Timber.e(e, "Error getting last media change timestamp")
            0L
        }
    }

    /**
     * Checks if any media has been added since the given timestamp.
     * 
     * Useful for determining if a rescan is needed.
     * 
     * @param sinceTimestamp Timestamp in milliseconds
     * @param mediaType Type of media to check (default: AUDIO)
     * @return true if new media has been added
     */
    fun hasNewMediaSince(sinceTimestamp: Long, mediaType: MediaType = MediaType.AUDIO): Boolean {
        val lastChange = getLastMediaChangeTimestamp(mediaType)
        return lastChange > sinceTimestamp
    }
}
