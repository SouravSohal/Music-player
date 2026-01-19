package com.musicplayer.app.util

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.MessageDigest

object FileUtils {
    
    /**
     * Get the app's external music directory
     */
    fun getMusicDirectory(context: Context): File? {
        return context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.also {
            if (!it.exists()) it.mkdirs()
        }
    }
    
    /**
     * Get the app's downloads directory
     */
    fun getDownloadsDirectory(context: Context): File? {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.also {
            if (!it.exists()) it.mkdirs()
        }
    }
    
    /**
     * Get the app's cache directory
     */
    fun getCacheDirectory(context: Context, subDirectory: String? = null): File {
        val cacheDir = if (subDirectory != null) {
            File(context.cacheDir, subDirectory)
        } else {
            context.cacheDir
        }
        
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        return cacheDir
    }
    
    /**
     * Get the album art cache directory
     */
    fun getAlbumArtDirectory(context: Context): File {
        return getCacheDirectory(context, Constants.ALBUM_ART_DIRECTORY)
    }
    
    /**
     * Check if file is a supported audio file
     */
    fun isSupportedAudioFile(file: File): Boolean {
        return isSupportedAudioFile(file.name)
    }
    
    fun isSupportedAudioFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return Constants.SUPPORTED_AUDIO_FORMATS.any { 
            it.lowercase().endsWith(extension) 
        }
    }
    
    /**
     * Get file extension from file name
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }
    
    /**
     * Get MIME type from file extension
     */
    fun getMimeType(fileName: String): String {
        val extension = getFileExtension(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) 
            ?: "audio/*"
    }
    
    /**
     * Generate a unique file name based on hash
     */
    fun generateUniqueFileName(input: String, extension: String): String {
        val hash = input.hashCode().toString(16)
        return "$hash.$extension"
    }
    
    /**
     * Generate MD5 hash of a string
     */
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get the size of a directory recursively
     */
    fun getDirectorySize(directory: File): Long {
        var size = 0L
        
        if (directory.exists()) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        }
        
        return size
    }
    
    /**
     * Clear cache directory
     */
    fun clearCache(context: Context): Boolean {
        return try {
            deleteDirectory(context.cacheDir)
            Timber.d("Cache cleared successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
            false
        }
    }
    
    /**
     * Clear cache if it exceeds the maximum size
     */
    fun clearCacheIfNeeded(context: Context, maxSizeMb: Long = Constants.MAX_CACHE_SIZE_MB): Boolean {
        val cacheSize = getDirectorySize(context.cacheDir)
        val maxSizeBytes = maxSizeMb * 1024 * 1024
        
        return if (cacheSize > maxSizeBytes) {
            Timber.d("Cache size ($cacheSize bytes) exceeds limit ($maxSizeBytes bytes), clearing...")
            clearCache(context)
        } else {
            false
        }
    }
    
    /**
     * Clear album art cache if it exceeds the maximum size
     */
    fun clearAlbumArtCacheIfNeeded(
        context: Context, 
        maxSizeMb: Long = Constants.MAX_ALBUM_ART_CACHE_SIZE_MB
    ): Boolean {
        val albumArtDir = getAlbumArtDirectory(context)
        val cacheSize = getDirectorySize(albumArtDir)
        val maxSizeBytes = maxSizeMb * 1024 * 1024
        
        return if (cacheSize > maxSizeBytes) {
            Timber.d("Album art cache size ($cacheSize bytes) exceeds limit ($maxSizeBytes bytes), clearing...")
            try {
                deleteDirectory(albumArtDir)
                albumArtDir.mkdirs()
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear album art cache")
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Delete a directory and all its contents
     */
    fun deleteDirectory(directory: File): Boolean {
        return try {
            if (directory.exists()) {
                directory.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete directory: ${directory.absolutePath}")
            false
        }
    }
    
    /**
     * Delete a file safely
     */
    fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists() && file.isFile) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file: ${file.absolutePath}")
            false
        }
    }
    
    /**
     * Create a new file with unique name if file already exists
     */
    @Throws(IOException::class)
    fun createUniqueFile(directory: File, fileName: String): File {
        val extension = getFileExtension(fileName)
        val nameWithoutExtension = fileName.substringBeforeLast('.')
        
        var file = File(directory, fileName)
        var counter = 1
        
        while (file.exists()) {
            val newFileName = if (extension.isNotEmpty()) {
                "${nameWithoutExtension}_$counter.$extension"
            } else {
                "${nameWithoutExtension}_$counter"
            }
            file = File(directory, newFileName)
            counter++
        }
        
        if (!file.createNewFile()) {
            throw IOException("Failed to create file: ${file.absolutePath}")
        }
        
        return file
    }
    
    /**
     * Check if external storage is available for read and write
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Check if external storage is available to at least read
     */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }
    
    /**
     * Sanitize file name by removing invalid characters
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
    }
}
