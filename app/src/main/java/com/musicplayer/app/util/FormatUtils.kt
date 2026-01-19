package com.musicplayer.app.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {
    
    /**
     * Format duration in milliseconds to MM:SS or HH:MM:SS format
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Format duration in milliseconds to human-readable format (e.g., "3h 45m", "2m 30s")
     */
    fun formatDurationLong(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 || isEmpty()) append("${seconds}s")
        }.trim()
    }
    
    /**
     * Format file size in bytes to human-readable format (B, KB, MB, GB)
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        
        val unit = 1024
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val size = bytes / unit.toDouble().pow(exp)
        
        return String.format(Locale.getDefault(), "%.2f %s", size, units[exp])
    }
    
    /**
     * Format bitrate in bps to kbps
     */
    fun formatBitrate(bitrate: Int): String {
        return if (bitrate >= 1000) {
            "${bitrate / 1000} kbps"
        } else {
            "$bitrate bps"
        }
    }
    
    /**
     * Format sample rate in Hz to kHz
     */
    fun formatSampleRate(sampleRate: Int): String {
        return if (sampleRate >= 1000) {
            String.format(Locale.getDefault(), "%.1f kHz", sampleRate / 1000.0)
        } else {
            "$sampleRate Hz"
        }
    }
    
    /**
     * Format track number with leading zeros
     */
    fun formatTrackNumber(trackNumber: Int, totalTracks: Int = 0): String {
        return if (totalTracks > 0) {
            String.format(Locale.getDefault(), "%02d/%02d", trackNumber, totalTracks)
        } else {
            String.format(Locale.getDefault(), "%02d", trackNumber)
        }
    }
    
    /**
     * Format date in milliseconds to human-readable format
     */
    fun formatDate(timeMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 365 -> "${days / 365} year${if (days / 365 > 1) "s" else ""} ago"
            days > 30 -> "${days / 30} month${if (days / 30 > 1) "s" else ""} ago"
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            seconds > 0 -> "$seconds second${if (seconds > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
    
    /**
     * Format song count
     */
    fun formatSongCount(count: Int): String {
        return "$count song${if (count != 1) "s" else ""}"
    }
    
    /**
     * Format percentage with one decimal place
     */
    fun formatPercentage(value: Float): String {
        return String.format(Locale.getDefault(), "%.1f%%", value * 100)
    }
}
