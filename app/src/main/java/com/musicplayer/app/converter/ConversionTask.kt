package com.musicplayer.app.converter

import android.net.Uri
import java.util.UUID

/**
 * Represents a video to audio conversion task.
 * 
 * This data class tracks the complete state of a conversion task including:
 * - Source video file information
 * - Target audio format and quality settings
 * - Conversion progress and status
 * - Timestamps and duration estimates
 * 
 * @property id Unique identifier for this conversion task
 * @property sourceUri URI of the source video file
 * @property sourcePath File path of the source video (for display)
 * @property outputPath Target file path for the converted audio
 * @property outputFormat Desired audio output format
 * @property bitrate Target bitrate in kbps
 * @property status Current status of the conversion
 * @property progress Current progress percentage (0-100)
 * @property startTimeMs Timestamp when conversion started (null if not started)
 * @property endTimeMs Timestamp when conversion completed (null if not completed)
 * @property estimatedTimeRemainingMs Estimated time remaining in milliseconds
 * @property errorMessage Error message if conversion failed
 * @property inputFileSizeBytes Size of input video file in bytes
 * @property outputFileSizeBytes Current size of output audio file in bytes
 */
data class ConversionTask(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri,
    val sourcePath: String,
    val outputPath: String,
    val outputFormat: AudioFormat,
    val bitrate: AudioBitrate,
    val status: ConversionStatus = ConversionStatus.PENDING,
    val progress: Float = 0f,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val estimatedTimeRemainingMs: Long? = null,
    val errorMessage: String? = null,
    val inputFileSizeBytes: Long = 0L,
    val outputFileSizeBytes: Long = 0L
) {
    /**
     * Calculates the actual elapsed time for this conversion.
     * 
     * @return Elapsed time in milliseconds, or null if not started
     */
    fun getElapsedTimeMs(): Long? {
        return startTimeMs?.let { start ->
            (endTimeMs ?: System.currentTimeMillis()) - start
        }
    }

    /**
     * Checks if the conversion is in a terminal state (completed, failed, or cancelled).
     * 
     * @return true if the conversion is finished, false otherwise
     */
    fun isFinished(): Boolean {
        return status in listOf(
            ConversionStatus.COMPLETED,
            ConversionStatus.FAILED,
            ConversionStatus.CANCELLED
        )
    }

    /**
     * Checks if the conversion is currently active (running or paused).
     * 
     * @return true if the conversion is active, false otherwise
     */
    fun isActive(): Boolean {
        return status in listOf(
            ConversionStatus.RUNNING,
            ConversionStatus.PAUSED
        )
    }

    /**
     * Creates a copy with updated progress information.
     * 
     * @param newProgress New progress value (0-100)
     * @param estimatedRemaining Estimated time remaining in milliseconds
     * @param currentOutputSize Current output file size in bytes
     * @return Updated ConversionTask
     */
    fun withProgress(
        newProgress: Float,
        estimatedRemaining: Long? = null,
        currentOutputSize: Long = outputFileSizeBytes
    ): ConversionTask {
        return copy(
            progress = newProgress.coerceIn(0f, 100f),
            estimatedTimeRemainingMs = estimatedRemaining,
            outputFileSizeBytes = currentOutputSize
        )
    }

    /**
     * Creates a copy with updated status.
     * 
     * @param newStatus New conversion status
     * @param error Optional error message for failed conversions
     * @return Updated ConversionTask
     */
    fun withStatus(newStatus: ConversionStatus, error: String? = null): ConversionTask {
        return copy(
            status = newStatus,
            errorMessage = error,
            startTimeMs = if (newStatus == ConversionStatus.RUNNING && startTimeMs == null) 
                System.currentTimeMillis() else startTimeMs,
            endTimeMs = if (newStatus in listOf(ConversionStatus.COMPLETED, ConversionStatus.FAILED, ConversionStatus.CANCELLED))
                System.currentTimeMillis() else null
        )
    }

    /**
     * Estimates the final output file size based on bitrate and duration.
     * 
     * @param durationMs Duration of the audio in milliseconds
     * @return Estimated output file size in bytes
     */
    fun estimateOutputSize(durationMs: Long): Long {
        val bitrateInBitsPerSecond = bitrate.value * 1000L
        val durationInSeconds = durationMs / 1000L
        return (bitrateInBitsPerSecond * durationInSeconds) / 8
    }
}

/**
 * Status of a conversion task.
 */
enum class ConversionStatus {
    /** Task is waiting to be processed */
    PENDING,
    
    /** Task is currently being processed */
    RUNNING,
    
    /** Task has been paused */
    PAUSED,
    
    /** Task completed successfully */
    COMPLETED,
    
    /** Task failed due to an error */
    FAILED,
    
    /** Task was cancelled by user */
    CANCELLED
}

/**
 * Supported audio output formats.
 * 
 * @property extension File extension for this format
 * @property mimeType MIME type for this format
 * @property displayName Human-readable format name
 * @property description Format description
 */
enum class AudioFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val description: String
) {
    MP3(
        extension = "mp3",
        mimeType = "audio/mpeg",
        displayName = "MP3",
        description = "MPEG Layer 3 Audio - Universal compatibility"
    ),
    AAC(
        extension = "aac",
        mimeType = "audio/aac",
        displayName = "AAC",
        description = "Advanced Audio Coding - High quality, efficient"
    ),
    M4A(
        extension = "m4a",
        mimeType = "audio/mp4",
        displayName = "M4A",
        description = "MPEG-4 Audio - Apple ecosystem optimized"
    ),
    FLAC(
        extension = "flac",
        mimeType = "audio/flac",
        displayName = "FLAC",
        description = "Free Lossless Audio Codec - Lossless quality"
    ),
    OGG(
        extension = "ogg",
        mimeType = "audio/ogg",
        displayName = "OGG Vorbis",
        description = "OGG Vorbis - Open source, high quality"
    );

    companion object {
        /**
         * Gets format from file extension.
         * 
         * @param extension File extension (with or without dot)
         * @return AudioFormat or null if not found
         */
        fun fromExtension(extension: String): AudioFormat? {
            val ext = extension.removePrefix(".")
            return values().find { it.extension.equals(ext, ignoreCase = true) }
        }
    }
}

/**
 * Supported audio bitrates in kbps.
 * 
 * @property value Bitrate value in kbps
 * @property displayName Human-readable bitrate name
 * @property quality Quality description
 */
enum class AudioBitrate(
    val value: Int,
    val displayName: String,
    val quality: String
) {
    LOW(
        value = 128,
        displayName = "128 kbps",
        quality = "Standard Quality"
    ),
    MEDIUM(
        value = 192,
        displayName = "192 kbps",
        quality = "Good Quality"
    ),
    HIGH(
        value = 256,
        displayName = "256 kbps",
        quality = "High Quality"
    ),
    VERY_HIGH(
        value = 320,
        displayName = "320 kbps",
        quality = "Very High Quality"
    );

    companion object {
        /**
         * Gets bitrate from integer value.
         * 
         * @param value Bitrate value in kbps
         * @return AudioBitrate or HIGH as default
         */
        fun fromValue(value: Int): AudioBitrate {
            return values().find { it.value == value } ?: HIGH
        }
    }
}
