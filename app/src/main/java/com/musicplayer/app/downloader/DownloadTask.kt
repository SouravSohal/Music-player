package com.musicplayer.app.downloader

import java.util.UUID

/**
 * Represents a media download task with comprehensive tracking capabilities.
 * 
 * This data class encapsulates all information about a download operation including:
 * - Source URL and destination path
 * - Download progress and speed metrics
 * - Quality settings and media type
 * - Status and error information
 * 
 * @property id Unique identifier for this download task
 * @property sourceUrl Original URL of the media to download
 * @property destinationPath Local file path where media will be saved
 * @property mediaType Type of media being downloaded (VIDEO or AUDIO_ONLY)
 * @property quality Requested quality/bitrate for the download
 * @property status Current status of the download
 * @property progress Current progress percentage (0-100)
 * @property bytesDownloaded Number of bytes downloaded so far
 * @property totalBytes Total size of the file in bytes (0 if unknown)
 * @property downloadSpeedBytesPerSec Current download speed in bytes per second
 * @property startTimeMs Timestamp when download started (null if not started)
 * @property endTimeMs Timestamp when download completed (null if not completed)
 * @property estimatedTimeRemainingMs Estimated time remaining in milliseconds
 * @property errorMessage Error message if download failed
 * @property canResume Whether this download supports resume capability
 * @property resumeToken Token for resuming interrupted downloads
 * @property retryCount Number of retry attempts made
 */
data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val sourceUrl: String,
    val destinationPath: String,
    val mediaType: MediaType = MediaType.AUDIO_ONLY,
    val quality: DownloadQuality = DownloadQuality.MEDIUM,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedBytesPerSec: Long = 0L,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val estimatedTimeRemainingMs: Long? = null,
    val errorMessage: String? = null,
    val canResume: Boolean = false,
    val resumeToken: String? = null,
    val retryCount: Int = 0
) {
    /**
     * Calculates the actual elapsed time for this download.
     * 
     * @return Elapsed time in milliseconds, or null if not started
     */
    fun getElapsedTimeMs(): Long? {
        return startTimeMs?.let { start ->
            (endTimeMs ?: System.currentTimeMillis()) - start
        }
    }

    /**
     * Checks if the download is in a terminal state (completed, failed, or cancelled).
     * 
     * @return true if the download is finished, false otherwise
     */
    fun isFinished(): Boolean {
        return status in listOf(
            DownloadStatus.COMPLETED,
            DownloadStatus.FAILED,
            DownloadStatus.CANCELLED
        )
    }

    /**
     * Checks if the download is currently active (downloading or paused).
     * 
     * @return true if the download is active, false otherwise
     */
    fun isActive(): Boolean {
        return status in listOf(
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED
        )
    }

    /**
     * Checks if this download can be resumed.
     * 
     * @return true if resume is possible, false otherwise
     */
    fun canBeResumed(): Boolean {
        return canResume && status == DownloadStatus.PAUSED && resumeToken != null
    }

    /**
     * Creates a copy with updated progress information.
     * 
     * @param newProgress New progress value (0-100)
     * @param downloaded Number of bytes downloaded
     * @param total Total bytes (0 if unknown)
     * @param speed Current download speed in bytes/sec
     * @param estimatedRemaining Estimated time remaining in milliseconds
     * @return Updated DownloadTask
     */
    fun withProgress(
        newProgress: Float,
        downloaded: Long,
        total: Long = totalBytes,
        speed: Long = downloadSpeedBytesPerSec,
        estimatedRemaining: Long? = null
    ): DownloadTask {
        return copy(
            progress = newProgress.coerceIn(0f, 100f),
            bytesDownloaded = downloaded,
            totalBytes = total,
            downloadSpeedBytesPerSec = speed,
            estimatedTimeRemainingMs = estimatedRemaining
        )
    }

    /**
     * Creates a copy with updated status.
     * 
     * @param newStatus New download status
     * @param error Optional error message for failed downloads
     * @param token Optional resume token
     * @return Updated DownloadTask
     */
    fun withStatus(
        newStatus: DownloadStatus, 
        error: String? = null,
        token: String? = resumeToken
    ): DownloadTask {
        return copy(
            status = newStatus,
            errorMessage = error,
            resumeToken = token,
            startTimeMs = if (newStatus == DownloadStatus.DOWNLOADING && startTimeMs == null) 
                System.currentTimeMillis() else startTimeMs,
            endTimeMs = if (newStatus in listOf(
                DownloadStatus.COMPLETED, 
                DownloadStatus.FAILED, 
                DownloadStatus.CANCELLED
            )) System.currentTimeMillis() else null
        )
    }

    /**
     * Creates a copy with incremented retry count.
     * 
     * @return Updated DownloadTask with incremented retry count
     */
    fun withRetry(): DownloadTask {
        return copy(
            retryCount = retryCount + 1,
            status = DownloadStatus.PENDING,
            errorMessage = null
        )
    }

    /**
     * Formats download speed for display.
     * 
     * @return Human-readable speed string (e.g., "1.5 MB/s")
     */
    fun formatSpeed(): String {
        return when {
            downloadSpeedBytesPerSec >= 1_000_000 -> 
                "%.1f MB/s".format(downloadSpeedBytesPerSec / 1_000_000.0)
            downloadSpeedBytesPerSec >= 1_000 -> 
                "%.1f KB/s".format(downloadSpeedBytesPerSec / 1_000.0)
            else -> "$downloadSpeedBytesPerSec B/s"
        }
    }

    /**
     * Formats total bytes for display.
     * 
     * @return Human-readable size string (e.g., "15.3 MB")
     */
    fun formatTotalSize(): String {
        return formatBytes(totalBytes)
    }

    /**
     * Formats downloaded bytes for display.
     * 
     * @return Human-readable size string (e.g., "5.2 MB")
     */
    fun formatDownloadedSize(): String {
        return formatBytes(bytesDownloaded)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}

/**
 * Status of a download task.
 */
enum class DownloadStatus {
    /** Task is waiting to be processed */
    PENDING,
    
    /** Task is queued and waiting for a download slot */
    QUEUED,
    
    /** Task is currently being downloaded */
    DOWNLOADING,
    
    /** Task has been paused by user or system */
    PAUSED,
    
    /** Task completed successfully */
    COMPLETED,
    
    /** Task failed due to an error */
    FAILED,
    
    /** Task was cancelled by user */
    CANCELLED
}

/**
 * Type of media to download.
 */
enum class MediaType(val displayName: String) {
    /** Download full video with audio */
    VIDEO("Video"),
    
    /** Extract and download audio only */
    AUDIO_ONLY("Audio Only");

    companion object {
        fun fromString(value: String): MediaType {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: AUDIO_ONLY
        }
    }
}

/**
 * Quality settings for downloads.
 * 
 * @property displayName Human-readable quality name
 * @property videoBitrate Target video bitrate in kbps (0 for audio-only)
 * @property audioBitrate Target audio bitrate in kbps
 * @property resolution Target resolution (e.g., "720p", "1080p")
 */
enum class DownloadQuality(
    val displayName: String,
    val videoBitrate: Int,
    val audioBitrate: Int,
    val resolution: String
) {
    LOW(
        displayName = "Low (360p)",
        videoBitrate = 500,
        audioBitrate = 96,
        resolution = "360p"
    ),
    MEDIUM(
        displayName = "Medium (480p)",
        videoBitrate = 1000,
        audioBitrate = 128,
        resolution = "480p"
    ),
    HIGH(
        displayName = "High (720p)",
        videoBitrate = 2500,
        audioBitrate = 192,
        resolution = "720p"
    ),
    VERY_HIGH(
        displayName = "Very High (1080p)",
        videoBitrate = 5000,
        audioBitrate = 256,
        resolution = "1080p"
    ),
    BEST(
        displayName = "Best Available",
        videoBitrate = 0, // Use source quality
        audioBitrate = 320,
        resolution = "Source"
    );

    companion object {
        /**
         * Gets quality from string value.
         * 
         * @param value Quality name
         * @return DownloadQuality or MEDIUM as default
         */
        fun fromString(value: String): DownloadQuality {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
