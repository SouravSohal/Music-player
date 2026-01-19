package com.musicplayer.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts detailed metadata from audio files using MediaMetadataRetriever.
 * 
 * This class handles:
 * - Extracting comprehensive metadata from various audio formats
 * - Reading and caching album artwork
 * - Supporting MP3, FLAC, AAC, OGG, WAV, and other formats
 * - Error handling for corrupted or unsupported files
 * 
 * Supported formats:
 * - MP3 (MPEG Layer 3)
 * - FLAC (Free Lossless Audio Codec)
 * - AAC (Advanced Audio Coding)
 * - OGG Vorbis
 * - WAV (Waveform Audio File Format)
 * - M4A (MPEG-4 Audio)
 * - OPUS
 * 
 * @property context Application context for file access
 */
@Singleton
class MetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val albumArtCache = mutableMapOf<Long, String>()
    private val cacheDir: File = File(context.cacheDir, "album_art")

    init {
        // Create cache directory if it doesn't exist
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Represents detailed metadata extracted from an audio file.
     * 
     * @property title Song title
     * @property artist Artist name
     * @property album Album name
     * @property albumArtist Album artist (may differ from track artist)
     * @property year Release year
     * @property genre Music genre
     * @property duration Duration in milliseconds
     * @property bitrate Bitrate in bits per second
     * @property sampleRate Sample rate in Hz
     * @property trackNumber Track number in album
     * @property discNumber Disc number for multi-disc albums
     * @property composer Composer name
     * @property writer Writer/lyricist name
     * @property mimeType MIME type of the audio file
     * @property hasAlbumArt Whether the file contains embedded album artwork
     */
    data class AudioMetadata(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val albumArtist: String? = null,
        val year: Int? = null,
        val genre: String? = null,
        val duration: Long? = null,
        val bitrate: Int? = null,
        val sampleRate: Int? = null,
        val trackNumber: Int? = null,
        val discNumber: Int? = null,
        val composer: String? = null,
        val writer: String? = null,
        val mimeType: String? = null,
        val hasAlbumArt: Boolean = false
    )

    /**
     * Extracts metadata from an audio file.
     * 
     * @param uri Content URI or file URI of the audio file
     * @return AudioMetadata object containing extracted information
     */
    suspend fun extractMetadata(uri: Uri): AudioMetadata = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val yearString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val bitrateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val trackString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val discString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            val writer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            // Parse numeric values safely
            val year = yearString?.toIntOrNull()
            val duration = durationString?.toLongOrNull()
            val bitrate = bitrateString?.toIntOrNull()
            
            // Parse track number (format: "track/total" or just "track")
            val trackNumber = trackString?.split("/")?.firstOrNull()?.toIntOrNull()
            val discNumber = discString?.split("/")?.firstOrNull()?.toIntOrNull()

            // Check if album art exists
            val hasAlbumArt = retriever.embeddedPicture != null

            // Get sample rate (if available)
            val sampleRate = try {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
            } catch (e: Exception) {
                null
            }

            AudioMetadata(
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                year = year,
                genre = genre,
                duration = duration,
                bitrate = bitrate,
                sampleRate = sampleRate,
                trackNumber = trackNumber,
                discNumber = discNumber,
                composer = composer,
                writer = writer,
                mimeType = mimeType,
                hasAlbumArt = hasAlbumArt
            )
        } catch (e: Exception) {
            Timber.e(e, "Error extracting metadata from URI: $uri")
            AudioMetadata() // Return empty metadata on error
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Timber.w(e, "Error releasing MediaMetadataRetriever")
            }
        }
    }

    /**
     * Extracts and caches album artwork from an audio file.
     * 
     * This method extracts embedded album art from the audio file and caches it
     * to disk for future use. Subsequent calls for the same album ID will return
     * the cached file path.
     * 
     * @param uri Content URI of the audio file
     * @param albumId Album ID for cache key
     * @return File path to the cached album artwork, or null if not available
     */
    suspend fun extractAndCacheAlbumArt(uri: Uri, albumId: Long): String? = withContext(Dispatchers.IO) {
        // Check if already cached
        albumArtCache[albumId]?.let { cachedPath ->
            if (File(cachedPath).exists()) {
                return@withContext cachedPath
            } else {
                albumArtCache.remove(albumId)
            }
        }

        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                val artFile = File(cacheDir, "album_${albumId}.jpg")
                FileOutputStream(artFile).use { output ->
                    output.write(artBytes)
                }
                
                val artPath = artFile.absolutePath
                albumArtCache[albumId] = artPath
                
                Timber.d("Album art cached for album ID $albumId at $artPath")
                return@withContext artPath
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting album art from URI: $uri")
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Timber.w(e, "Error releasing MediaMetadataRetriever")
            }
        }

        null
    }

    /**
     * Extracts album artwork as a Bitmap.
     * 
     * @param uri Content URI of the audio file
     * @return Bitmap of the album artwork, or null if not available
     */
    suspend fun extractAlbumArtBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                return@withContext BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error extracting album art bitmap from URI: $uri")
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Timber.w(e, "Error releasing MediaMetadataRetriever")
            }
        }

        null
    }

    /**
     * Extracts album artwork as a byte array.
     * 
     * @param uri Content URI of the audio file
     * @return Byte array of the album artwork, or null if not available
     */
    suspend fun extractAlbumArtBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            return@withContext retriever.embeddedPicture
        } catch (e: Exception) {
            Timber.e(e, "Error extracting album art bytes from URI: $uri")
            null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Timber.w(e, "Error releasing MediaMetadataRetriever")
            }
        }
    }

    /**
     * Gets the cached album art path for a given album ID.
     * 
     * @param albumId Album ID
     * @return File path to cached album art, or null if not cached
     */
    fun getCachedAlbumArt(albumId: Long): String? {
        return albumArtCache[albumId]?.takeIf { File(it).exists() }
    }

    /**
     * Clears the album art cache from memory and disk.
     * 
     * @param clearDisk Whether to also delete cached files from disk
     */
    suspend fun clearCache(clearDisk: Boolean = false) = withContext(Dispatchers.IO) {
        albumArtCache.clear()
        
        if (clearDisk) {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                Timber.d("Album art cache cleared from disk")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing album art cache from disk")
            }
        }
    }

    /**
     * Gets the size of the album art cache on disk.
     * 
     * @return Total size in bytes
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            Timber.e(e, "Error calculating cache size")
            0L
        }
    }

    /**
     * Checks if an audio file format is supported.
     * 
     * @param mimeType MIME type of the audio file
     * @return true if the format is supported, false otherwise
     */
    fun isFormatSupported(mimeType: String): Boolean {
        return when (mimeType.lowercase()) {
            "audio/mpeg", "audio/mp3" -> true // MP3
            "audio/flac" -> true // FLAC
            "audio/mp4", "audio/mp4a-latm", "audio/aac" -> true // AAC/M4A
            "audio/ogg", "audio/vorbis" -> true // OGG Vorbis
            "audio/wav", "audio/x-wav" -> true // WAV
            "audio/opus" -> true // OPUS
            "audio/x-ms-wma" -> true // WMA
            "audio/amr" -> true // AMR
            else -> mimeType.startsWith("audio/")
        }
    }

    /**
     * Gets a human-readable audio format name from MIME type.
     * 
     * @param mimeType MIME type of the audio file
     * @return Human-readable format name
     */
    fun getFormatName(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "MP3"
            "audio/flac" -> "FLAC"
            "audio/mp4", "audio/mp4a-latm", "audio/aac" -> "AAC"
            "audio/ogg", "audio/vorbis" -> "OGG Vorbis"
            "audio/wav", "audio/x-wav" -> "WAV"
            "audio/opus" -> "OPUS"
            "audio/x-ms-wma" -> "WMA"
            "audio/amr" -> "AMR"
            else -> mimeType.substringAfter("/").uppercase()
        }
    }

    /**
     * Formats bitrate for display.
     * 
     * @param bitrate Bitrate in bits per second
     * @return Formatted string (e.g., "320 kbps")
     */
    fun formatBitrate(bitrate: Int?): String {
        return bitrate?.let { "${it / 1000} kbps" } ?: "Unknown"
    }

    /**
     * Formats sample rate for display.
     * 
     * @param sampleRate Sample rate in Hz
     * @return Formatted string (e.g., "44.1 kHz")
     */
    fun formatSampleRate(sampleRate: Int?): String {
        return sampleRate?.let { 
            val khz = it / 1000.0
            String.format("%.1f kHz", khz)
        } ?: "Unknown"
    }

    /**
     * Formats duration for display.
     * 
     * @param duration Duration in milliseconds
     * @return Formatted string (e.g., "3:45")
     */
    fun formatDuration(duration: Long?): String {
        if (duration == null) return "0:00"
        
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
