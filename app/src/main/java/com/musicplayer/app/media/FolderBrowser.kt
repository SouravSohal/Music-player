package com.musicplayer.app.media

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides folder-based browsing functionality for audio files.
 * 
 * This class enables:
 * - Browsing audio files organized by folder structure
 * - Navigating folder hierarchy
 * - Filtering audio files from other content
 * - Getting folder statistics (file count, total duration, total size)
 * 
 * @property context Application context
 * @property mediaScanner MediaScanner instance for querying audio files
 */
@Singleton
class FolderBrowser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaScanner: MediaScanner
) {

    /**
     * Represents a folder containing audio files.
     * 
     * @property path Absolute path to the folder
     * @property name Folder name (last component of path)
     * @property audioFileCount Number of audio files in this folder
     * @property totalDuration Total duration of all audio files in milliseconds
     * @property totalSize Total size of all audio files in bytes
     * @property parentPath Path to the parent folder, or null if root
     * @property subFolders List of immediate subfolders containing audio files
     */
    data class AudioFolder(
        val path: String,
        val name: String,
        val audioFileCount: Int,
        val totalDuration: Long,
        val totalSize: Long,
        val parentPath: String?,
        val subFolders: List<String> = emptyList()
    )

    /**
     * Represents the result of browsing a folder.
     * 
     * @property folder Information about the current folder
     * @property audioFiles List of audio files in the folder
     * @property subFolders List of subfolders
     */
    data class FolderBrowseResult(
        val folder: AudioFolder,
        val audioFiles: List<MediaScanner.AudioFile>,
        val subFolders: List<AudioFolder>
    )

    /**
     * Gets all folders containing audio files organized by hierarchy.
     * 
     * This method scans for all audio files and organizes them into a folder structure.
     * Only folders containing audio files (or subfolders with audio files) are included.
     * 
     * @param minDuration Minimum duration in milliseconds to filter audio files
     * @return Flow emitting the list of root-level folders
     */
    fun getAllAudioFolders(minDuration: Long = 30_000): Flow<Result<List<AudioFolder>>> = flow {
        try {
            Timber.d("Getting all audio folders...")
            
            val folderMap = mutableMapOf<String, MutableList<MediaScanner.AudioFile>>()
            
            // Collect all audio files from scanner
            mediaScanner.scanAudioFiles(minDuration).collect { result ->
                result.onSuccess { data ->
                    if (data is MediaScanner.ScanResult) {
                        // Group files by folder
                        data.audioFiles.forEach { audioFile ->
                            val files = folderMap.getOrPut(audioFile.folderPath) { mutableListOf() }
                            files.add(audioFile)
                        }
                    }
                }
            }

            // Convert to AudioFolder objects
            val folders = folderMap.map { (path, files) ->
                createAudioFolder(path, files)
            }.sortedBy { it.name }

            Timber.d("Found ${folders.size} folders with audio files")
            emit(Result.success(folders))
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting audio folders")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Browses a specific folder and returns its contents.
     * 
     * This method retrieves all audio files in the specified folder along with
     * information about subfolders. Uses optimized queries where possible.
     * 
     * @param folderPath Absolute path to the folder to browse
     * @param includeSubfolders Whether to include files from subfolders
     * @param minDuration Minimum duration in milliseconds to filter audio files
     * @return Flow emitting the folder browse result
     */
    fun browseFolder(
        folderPath: String,
        includeSubfolders: Boolean = false,
        minDuration: Long = 30_000
    ): Flow<Result<FolderBrowseResult>> = flow {
        try {
            Timber.d("Browsing folder: $folderPath (includeSubfolders=$includeSubfolders)")
            
            if (!includeSubfolders) {
                // Simple case: just get files in this folder
                val files = mediaScanner.getAudioFilesInFolder(folderPath, minDuration)
                val folder = createAudioFolder(folderPath, files)
                
                // Get subfolder information by scanning
                val subFolderMap = mutableMapOf<String, MutableList<MediaScanner.AudioFile>>()
                mediaScanner.scanAudioFiles(minDuration).collect { result ->
                    result.onSuccess { data ->
                        if (data is MediaScanner.ScanResult) {
                            data.audioFiles.forEach { audioFile ->
                                if (isDirectSubfolder(folderPath, audioFile.folderPath)) {
                                    val subFolder = getDirectSubfolderPath(folderPath, audioFile.folderPath)
                                    val subFiles = subFolderMap.getOrPut(subFolder) { mutableListOf() }
                                    subFiles.add(audioFile)
                                }
                            }
                        }
                    }
                }
                
                val subFolders = subFolderMap.map { (path, subFiles) ->
                    createAudioFolder(path, subFiles)
                }.sortedBy { it.name }
                
                val result = FolderBrowseResult(
                    folder = folder,
                    audioFiles = files.sortedBy { it.title },
                    subFolders = subFolders
                )
                
                Timber.d("Folder browse complete: ${files.size} files, ${subFolders.size} subfolders")
                emit(Result.success(result))
            } else {
                // Complex case: need all files including subfolders
                val allFiles = mutableListOf<MediaScanner.AudioFile>()
                val subFolderMap = mutableMapOf<String, MutableList<MediaScanner.AudioFile>>()
                
                mediaScanner.scanAudioFiles(minDuration).collect { result ->
                    result.onSuccess { data ->
                        if (data is MediaScanner.ScanResult) {
                            data.audioFiles.forEach { audioFile ->
                                when {
                                    // Exact folder match
                                    audioFile.folderPath == folderPath -> {
                                        allFiles.add(audioFile)
                                    }
                                    // Subfolder match (including subfolders)
                                    audioFile.folderPath.startsWith("$folderPath/") -> {
                                        allFiles.add(audioFile)
                                    }
                                }
                                
                                // Track direct subfolders
                                if (isDirectSubfolder(folderPath, audioFile.folderPath)) {
                                    val subFolder = getDirectSubfolderPath(folderPath, audioFile.folderPath)
                                    val files = subFolderMap.getOrPut(subFolder) { mutableListOf() }
                                    files.add(audioFile)
                                }
                            }
                        }
                    }
                }

                val folder = createAudioFolder(folderPath, allFiles)
                val subFolders = subFolderMap.map { (path, files) ->
                    createAudioFolder(path, files)
                }.sortedBy { it.name }

                val result = FolderBrowseResult(
                    folder = folder,
                    audioFiles = allFiles.sortedBy { it.title },
                    subFolders = subFolders
                )

                Timber.d("Folder browse complete: ${allFiles.size} files, ${subFolders.size} subfolders")
                emit(Result.success(result))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error browsing folder: $folderPath")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Gets audio files in a specific folder (without subfolders).
     * 
     * This method uses optimized MediaStore queries to fetch only files
     * in the specified folder without scanning the entire library.
     * 
     * @param folderPath Absolute path to the folder
     * @param minDuration Minimum duration in milliseconds
     * @return List of audio files in the folder
     */
    suspend fun getAudioFilesInFolder(
        folderPath: String,
        minDuration: Long = 30_000
    ): List<MediaScanner.AudioFile> = withContext(Dispatchers.IO) {
        try {
            mediaScanner.getAudioFilesInFolder(folderPath, minDuration)
        } catch (e: Exception) {
            Timber.e(e, "Error getting audio files in folder: $folderPath")
            emptyList()
        }
    }

    /**
     * Gets all parent folders in the path hierarchy.
     * 
     * For example, for "/storage/emulated/0/Music/Rock/Classic"
     * returns ["/storage/emulated/0/Music", "/storage/emulated/0/Music/Rock"]
     * 
     * @param folderPath Absolute path to the folder
     * @return List of parent folder paths from root to immediate parent
     */
    fun getParentFolders(folderPath: String): List<String> {
        val parents = mutableListOf<String>()
        var currentPath = folderPath
        
        while (true) {
            val parentPath = File(currentPath).parent ?: break
            if (parentPath == currentPath) break // Reached root
            parents.add(0, parentPath) // Add to beginning
            currentPath = parentPath
        }
        
        return parents
    }

    /**
     * Gets the immediate parent folder path.
     * 
     * @param folderPath Absolute path to the folder
     * @return Parent folder path, or null if no parent
     */
    fun getParentFolder(folderPath: String): String? {
        return File(folderPath).parent
    }

    /**
     * Checks if a folder contains audio files (directly or in subfolders).
     * 
     * This method uses an optimized query that returns as soon as the first
     * audio file is found, without scanning the entire library.
     * 
     * @param folderPath Absolute path to the folder
     * @param minDuration Minimum duration in milliseconds
     * @return true if the folder contains audio files
     */
    suspend fun containsAudioFiles(
        folderPath: String,
        minDuration: Long = 30_000
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            mediaScanner.containsAudioFiles(folderPath, minDuration)
        } catch (e: Exception) {
            Timber.e(e, "Error checking if folder contains audio files: $folderPath")
            false
        }
    }

    /**
     * Gets folder statistics (file count, duration, size).
     * 
     * Uses optimized queries when not including subfolders. For subfolder
     * statistics, falls back to full scan as it requires comprehensive data.
     * 
     * @param folderPath Absolute path to the folder
     * @param includeSubfolders Whether to include subfolders in statistics
     * @param minDuration Minimum duration in milliseconds
     * @return AudioFolder object with statistics
     */
    suspend fun getFolderStatistics(
        folderPath: String,
        includeSubfolders: Boolean = false,
        minDuration: Long = 30_000
    ): AudioFolder? = withContext(Dispatchers.IO) {
        try {
            if (!includeSubfolders) {
                // Optimized path: direct folder query
                val files = mediaScanner.getAudioFilesInFolder(folderPath, minDuration)
                if (files.isEmpty()) return@withContext null
                createAudioFolder(folderPath, files)
            } else {
                // Need full scan for subfolder statistics
                val files = mutableListOf<MediaScanner.AudioFile>()
                
                mediaScanner.scanAudioFiles(minDuration).collect { result ->
                    result.onSuccess { data ->
                        if (data is MediaScanner.ScanResult) {
                            files.addAll(
                                data.audioFiles.filter { audioFile ->
                                    audioFile.folderPath.startsWith(folderPath)
                                }
                            )
                        }
                    }
                }
                
                if (files.isEmpty()) return@withContext null
                createAudioFolder(folderPath, files)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting folder statistics: $folderPath")
            null
        }
    }

    /**
     * Searches for folders by name.
     * 
     * @param query Search query (case-insensitive)
     * @param minDuration Minimum duration in milliseconds
     * @return List of matching folders
     */
    fun searchFolders(query: String, minDuration: Long = 30_000): Flow<Result<List<AudioFolder>>> = flow {
        try {
            val allFolders = mutableListOf<AudioFolder>()
            
            getAllAudioFolders(minDuration).collect { result ->
                result.onSuccess { folders ->
                    allFolders.addAll(
                        folders.filter { folder ->
                            folder.name.contains(query, ignoreCase = true) ||
                            folder.path.contains(query, ignoreCase = true)
                        }
                    )
                }
            }
            
            emit(Result.success(allFolders))
        } catch (e: Exception) {
            Timber.e(e, "Error searching folders with query: $query")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Creates an AudioFolder object from a path and list of files.
     * 
     * @param path Folder path
     * @param files List of audio files in the folder
     * @return AudioFolder object with calculated statistics
     */
    private fun createAudioFolder(path: String, files: List<MediaScanner.AudioFile>): AudioFolder {
        val name = path.substringAfterLast('/')
        val parentPath = File(path).parent
        val totalDuration = files.sumOf { it.duration }
        val totalSize = files.sumOf { it.size }
        
        return AudioFolder(
            path = path,
            name = name,
            audioFileCount = files.size,
            totalDuration = totalDuration,
            totalSize = totalSize,
            parentPath = parentPath
        )
    }

    /**
     * Checks if childPath is a direct subfolder of parentPath.
     * 
     * @param parentPath Parent folder path
     * @param childPath Potential child folder path
     * @return true if childPath is a direct subfolder
     */
    private fun isDirectSubfolder(parentPath: String, childPath: String): Boolean {
        if (!childPath.startsWith("$parentPath/")) return false
        
        val relativePath = childPath.substring(parentPath.length + 1)
        return !relativePath.contains('/')
    }

    /**
     * Gets the direct subfolder path from a parent path and a descendant path.
     * 
     * @param parentPath Parent folder path
     * @param descendantPath Descendant folder path
     * @return Direct subfolder path
     */
    private fun getDirectSubfolderPath(parentPath: String, descendantPath: String): String {
        val relativePath = descendantPath.substring(parentPath.length + 1)
        val subfolderName = relativePath.substringBefore('/')
        return "$parentPath/$subfolderName"
    }

    /**
     * Formats folder size for display.
     * 
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "125.5 MB")
     */
    fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Formats total duration for display.
     * 
     * @param duration Duration in milliseconds
     * @return Formatted string (e.g., "2h 45m")
     */
    fun formatTotalDuration(duration: Long): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
