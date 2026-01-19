package com.musicplayer.app.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main media scanner class that scans local storage for audio files using MediaStore API.
 * 
 * This class handles:
 * - Scanning local storage for audio files
 * - Extracting metadata from audio files
 * - Reading album artwork
 * - Supporting folder-based browsing
 * - Handling Android 10+ scoped storage
 * - Providing scan progress via Flow
 * 
 * @property context Application context
 * @property metadataExtractor Helper for extracting detailed metadata
 */
@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataExtractor: MetadataExtractor
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Represents the result of scanning for audio files.
     * 
     * @property audioFiles List of scanned audio files with metadata
     * @property totalCount Total number of audio files found
     * @property scanDurationMs Time taken to complete the scan in milliseconds
     */
    data class ScanResult(
        val audioFiles: List<AudioFile>,
        val totalCount: Int,
        val scanDurationMs: Long
    )

    /**
     * Represents progress during a media scan operation.
     * 
     * @property currentFile Current file being scanned
     * @property processedCount Number of files processed so far
     * @property totalCount Total number of files to process
     * @property percentage Scan completion percentage (0-100)
     */
    data class ScanProgress(
        val currentFile: String,
        val processedCount: Int,
        val totalCount: Int,
        val percentage: Int
    )

    /**
     * Represents an audio file with its metadata.
     * 
     * @property id Media store ID
     * @property uri Content URI to the audio file
     * @property title Song title
     * @property artist Artist name
     * @property album Album name
     * @property albumId Album ID for fetching artwork
     * @property duration Duration in milliseconds
     * @property size File size in bytes
     * @property mimeType MIME type of the audio file
     * @property bitrate Bitrate in bits per second
     * @property sampleRate Sample rate in Hz
     * @property dateAdded Timestamp when file was added
     * @property dateModified Timestamp when file was last modified
     * @property trackNumber Track number in album
     * @property year Release year
     * @property genre Music genre
     * @property path File path
     * @property folderPath Parent folder path
     * @property folderName Parent folder name
     */
    data class AudioFile(
        val id: Long,
        val uri: Uri,
        val title: String,
        val artist: String,
        val album: String,
        val albumId: Long,
        val duration: Long,
        val size: Long,
        val mimeType: String,
        val bitrate: Int?,
        val sampleRate: Int?,
        val dateAdded: Long,
        val dateModified: Long,
        val trackNumber: Int?,
        val year: Int?,
        val genre: String?,
        val path: String,
        val folderPath: String,
        val folderName: String
    )

    /**
     * Scans all audio files from device storage with progress updates.
     * 
     * This method uses MediaStore API to query all audio files and emits progress
     * updates as files are being processed. Supports both legacy and scoped storage.
     * 
     * @param minDuration Minimum duration in milliseconds to filter short audio files (default: 30000ms = 30s)
     * @return Flow emitting scan progress and final result
     */
    fun scanAudioFiles(minDuration: Long = 30_000): Flow<Result<Any>> = flow {
        val startTime = System.currentTimeMillis()
        
        try {
            Timber.d("Starting media scan...")
            
            // First, count total files
            val totalCount = countAudioFiles(minDuration)
            Timber.d("Found $totalCount audio files to scan")
            
            if (totalCount == 0) {
                emit(Result.success(ScanResult(emptyList(), 0, 0)))
                return@flow
            }

            val audioFiles = mutableListOf<AudioFile>()
            var processedCount = 0

            // Query audio files
            queryAudioFiles(minDuration) { cursor ->
                try {
                    val audioFile = extractAudioFileFromCursor(cursor)
                    audioFiles.add(audioFile)
                    processedCount++

                    // Emit progress update every 10 files or at the end
                    if (processedCount % 10 == 0 || processedCount == totalCount) {
                        val progress = ScanProgress(
                            currentFile = audioFile.title,
                            processedCount = processedCount,
                            totalCount = totalCount,
                            percentage = (processedCount * 100) / totalCount
                        )
                        emit(Result.success(progress))
                        Timber.d("Scan progress: $processedCount/$totalCount (${progress.percentage}%)")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing audio file")
                }
            }

            val scanDuration = System.currentTimeMillis() - startTime
            val result = ScanResult(
                audioFiles = audioFiles,
                totalCount = audioFiles.size,
                scanDurationMs = scanDuration
            )
            
            Timber.d("Media scan completed: ${result.totalCount} files in ${result.scanDurationMs}ms")
            emit(Result.success(result))
            
        } catch (e: Exception) {
            Timber.e(e, "Error during media scan")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Scans audio files and enriches them with detailed metadata.
     * 
     * This is a more thorough scan that uses MediaMetadataRetriever to extract
     * additional metadata like bitrate and sample rate.
     * 
     * @param minDuration Minimum duration in milliseconds
     * @return Flow emitting scan progress and final result with enriched metadata
     */
    fun scanAudioFilesWithDetailedMetadata(minDuration: Long = 30_000): Flow<Result<Any>> = flow {
        val startTime = System.currentTimeMillis()
        
        try {
            Timber.d("Starting detailed media scan...")
            
            val totalCount = countAudioFiles(minDuration)
            if (totalCount == 0) {
                emit(Result.success(ScanResult(emptyList(), 0, 0)))
                return@flow
            }

            val audioFiles = mutableListOf<AudioFile>()
            var processedCount = 0

            queryAudioFiles(minDuration) { cursor ->
                try {
                    var audioFile = extractAudioFileFromCursor(cursor)
                    
                    // Enrich with detailed metadata
                    val detailedMetadata = metadataExtractor.extractMetadata(audioFile.uri)
                    audioFile = audioFile.copy(
                        title = detailedMetadata.title ?: audioFile.title,
                        artist = detailedMetadata.artist ?: audioFile.artist,
                        album = detailedMetadata.album ?: audioFile.album,
                        year = detailedMetadata.year ?: audioFile.year,
                        genre = detailedMetadata.genre ?: audioFile.genre,
                        bitrate = detailedMetadata.bitrate ?: audioFile.bitrate,
                        duration = detailedMetadata.duration ?: audioFile.duration
                    )
                    
                    audioFiles.add(audioFile)
                    processedCount++

                    if (processedCount % 5 == 0 || processedCount == totalCount) {
                        val progress = ScanProgress(
                            currentFile = audioFile.title,
                            processedCount = processedCount,
                            totalCount = totalCount,
                            percentage = (processedCount * 100) / totalCount
                        )
                        emit(Result.success(progress))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing audio file with detailed metadata")
                }
            }

            val scanDuration = System.currentTimeMillis() - startTime
            emit(Result.success(ScanResult(audioFiles, audioFiles.size, scanDuration)))
            
        } catch (e: Exception) {
            Timber.e(e, "Error during detailed media scan")
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Gets album artwork URI for a given album ID.
     * 
     * @param albumId Album ID from MediaStore
     * @return URI to the album artwork, or null if not available
     */
    suspend fun getAlbumArtwork(albumId: Long): Uri? = withContext(Dispatchers.IO) {
        try {
            val artworkUri = Uri.parse("content://media/external/audio/albumart")
            ContentUris.withAppendedId(artworkUri, albumId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting album artwork for albumId: $albumId")
            null
        }
    }

    /**
     * Gets a list of all unique folders containing audio files.
     * 
     * @return List of folder paths containing audio files
     */
    suspend fun getAudioFolders(): List<String> = withContext(Dispatchers.IO) {
        val folders = mutableSetOf<String>()
        
        try {
            queryAudioFiles(0) { cursor ->
                val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val path = cursor.getString(dataIndex)
                val folderPath = path.substringBeforeLast('/')
                folders.add(folderPath)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting audio folders")
        }
        
        folders.sorted()
    }

    /**
     * Counts the total number of audio files matching the criteria.
     * 
     * @param minDuration Minimum duration in milliseconds
     * @return Total count of audio files
     */
    private fun countAudioFiles(minDuration: Long): Int {
        val uri = getMediaStoreUri()
        val selection = buildSelection(minDuration)
        val selectionArgs = buildSelectionArgs(minDuration)

        contentResolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            return cursor.count
        }

        return 0
    }

    /**
     * Queries audio files from MediaStore and processes each row with the provided callback.
     * 
     * @param minDuration Minimum duration in milliseconds
     * @param onEachFile Callback invoked for each audio file row
     */
    private fun queryAudioFiles(minDuration: Long, onEachFile: (Cursor) -> Unit) {
        val uri = getMediaStoreUri()
        val projection = getProjection()
        val selection = buildSelection(minDuration)
        val selectionArgs = buildSelectionArgs(minDuration)
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                onEachFile(cursor)
            }
        }
    }

    /**
     * Extracts audio file information from a cursor row.
     * 
     * @param cursor Cursor positioned at an audio file row
     * @return AudioFile object with extracted metadata
     */
    private fun extractAudioFileFromCursor(cursor: Cursor): AudioFile {
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        val id = cursor.getLong(idIndex)
        val title = cursor.getString(titleIndex) ?: "Unknown Title"
        val artist = cursor.getString(artistIndex) ?: "Unknown Artist"
        val album = cursor.getString(albumIndex) ?: "Unknown Album"
        val albumId = cursor.getLong(albumIdIndex)
        val duration = cursor.getLong(durationIndex)
        val size = cursor.getLong(sizeIndex)
        val mimeType = cursor.getString(mimeTypeIndex) ?: "audio/*"
        val dateAdded = cursor.getLong(dateAddedIndex) * 1000 // Convert to milliseconds
        val dateModified = cursor.getLong(dateModifiedIndex) * 1000
        val path = cursor.getString(dataIndex)

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            id
        )

        val folderPath = path.substringBeforeLast('/')
        val folderName = folderPath.substringAfterLast('/')

        // Try to get optional fields
        var trackNumber: Int? = null
        var year: Int? = null
        var genre: String? = null

        try {
            val trackIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            if (trackIndex != -1) {
                trackNumber = cursor.getInt(trackIndex)
            }
        } catch (e: Exception) {
            Timber.w("Could not get track number: ${e.message}")
        }

        try {
            val yearIndex = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            if (yearIndex != -1) {
                year = cursor.getInt(yearIndex)
            }
        } catch (e: Exception) {
            Timber.w("Could not get year: ${e.message}")
        }

        return AudioFile(
            id = id,
            uri = uri,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            duration = duration,
            size = size,
            mimeType = mimeType,
            bitrate = null, // Will be enriched later if needed
            sampleRate = null,
            dateAdded = dateAdded,
            dateModified = dateModified,
            trackNumber = trackNumber,
            year = year,
            genre = genre,
            path = path,
            folderPath = folderPath,
            folderName = folderName
        )
    }

    /**
     * Gets the appropriate MediaStore URI based on Android version.
     * 
     * @return MediaStore URI for audio files
     */
    private fun getMediaStoreUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    /**
     * Gets the projection (columns) to query from MediaStore.
     * 
     * @return Array of column names to retrieve
     */
    private fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )
    }

    /**
     * Builds the selection clause for MediaStore query.
     * 
     * @param minDuration Minimum duration in milliseconds
     * @return Selection clause string
     */
    private fun buildSelection(minDuration: Long): String {
        val conditions = mutableListOf<String>()
        
        // Filter by music type
        conditions.add("${MediaStore.Audio.Media.IS_MUSIC} = ?")
        
        // Filter by minimum duration
        if (minDuration > 0) {
            conditions.add("${MediaStore.Audio.Media.DURATION} >= ?")
        }

        return conditions.joinToString(" AND ")
    }

    /**
     * Builds the selection arguments array for MediaStore query.
     * 
     * @param minDuration Minimum duration in milliseconds
     * @return Array of selection argument values
     */
    private fun buildSelectionArgs(minDuration: Long): Array<String> {
        val args = mutableListOf<String>()
        
        args.add("1") // IS_MUSIC = 1
        
        if (minDuration > 0) {
            args.add(minDuration.toString())
        }

        return args.toTypedArray()
    }

    /**
     * Queries audio files in a specific folder path from MediaStore.
     * 
     * This method is optimized to query only files in the specified folder
     * without scanning the entire library. Uses SQL LIKE with escaped path.
     * 
     * @param folderPath Absolute path to the folder
     * @param minDuration Minimum duration in milliseconds
     * @return List of audio files in the folder
     */
    suspend fun getAudioFilesInFolder(
        folderPath: String,
        minDuration: Long = 30_000
    ): List<AudioFile> = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<AudioFile>()
        
        try {
            val uri = getMediaStoreUri()
            val projection = getProjection()
            val selection = buildSelectionWithFolder(minDuration)
            val selectionArgs = buildSelectionArgsWithFolder(folderPath, minDuration)
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        val audioFile = extractAudioFileFromCursor(cursor)
                        // Additional check to ensure exact folder match (no subfolders)
                        if (audioFile.folderPath == folderPath) {
                            audioFiles.add(audioFile)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error processing audio file")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error querying folder: $folderPath")
        }
        
        audioFiles
    }

    /**
     * Checks if a folder contains any audio files.
     * 
     * Optimized to return as soon as the first audio file is found.
     * 
     * @param folderPath Absolute path to the folder
     * @param minDuration Minimum duration in milliseconds
     * @return true if the folder contains at least one audio file
     */
    suspend fun containsAudioFiles(
        folderPath: String,
        minDuration: Long = 30_000
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = getMediaStoreUri()
            val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA)
            val selection = buildSelectionWithFolder(minDuration)
            val selectionArgs = buildSelectionArgsWithFolder(folderPath, minDuration)

            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                // If cursor has any results, the folder contains audio files
                return@withContext cursor.count > 0
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking folder: $folderPath")
        }
        
        false
    }

    /**
     * Builds the selection clause with folder path filter.
     * 
     * @param minDuration Minimum duration in milliseconds
     * @return Selection clause string
     */
    private fun buildSelectionWithFolder(minDuration: Long): String {
        val conditions = mutableListOf<String>()
        
        conditions.add("${MediaStore.Audio.Media.IS_MUSIC} = ?")
        conditions.add("${MediaStore.Audio.Media.DATA} LIKE ?")
        
        if (minDuration > 0) {
            conditions.add("${MediaStore.Audio.Media.DURATION} >= ?")
        }

        return conditions.joinToString(" AND ")
    }

    /**
     * Escapes special characters in a string for use in SQL LIKE queries.
     * 
     * Escapes the following characters:
     * - \ (backslash) -> \\
     * - % (percent) -> \%
     * - _ (underscore) -> \_
     * 
     * Note: ContentResolver.query() with selection args provides SQL injection
     * protection via parameterized queries. This method only escapes LIKE
     * special characters which have special meaning in LIKE patterns.
     * 
     * @param value String to escape
     * @return Escaped string safe for SQL LIKE patterns
     */
    private fun escapeSqlLike(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    /**
     * Builds the selection arguments array with folder path.
     * 
     * Uses SQL LIKE pattern to match folder path. The pattern matches files
     * starting with the folder path. Note: This will match subfolders too,
     * so additional filtering in code is necessary for exact folder match.
     * 
     * SQL Injection Safety: ContentResolver.query() uses parameterized queries
     * where selection args are automatically escaped. The escaping here is for
     * SQL LIKE special characters (%, _) which have special meaning in LIKE queries.
     * 
     * @param folderPath Folder path to filter
     * @param minDuration Minimum duration in milliseconds
     * @return Array of selection argument values
     */
    private fun buildSelectionArgsWithFolder(folderPath: String, minDuration: Long): Array<String> {
        val args = mutableListOf<String>()
        
        args.add("1") // IS_MUSIC = 1
        args.add("${escapeSqlLike(folderPath)}/%") // DATA LIKE folderPath/%
        
        if (minDuration > 0) {
            args.add(minDuration.toString())
        }

        return args.toTypedArray()
    }
}
