# Media Scanning Module

This directory contains the comprehensive media scanning module for the Music Player application.

## Overview

The media scanning module provides a complete solution for scanning, indexing, and monitoring audio files on Android devices. It handles both legacy storage and Android 10+ scoped storage, uses coroutines for async operations, and provides real-time updates via Flow.

## Components

### 1. MediaScanner.kt

Main scanner class that scans local storage using the MediaStore API.

**Features:**
- Scans all audio files from device storage
- Extracts basic metadata (title, artist, album, duration, etc.)
- Supports folder-based organization
- Handles Android 10+ scoped storage
- Provides scan progress via Flow
- Uses coroutines for async scanning
- Filters by minimum duration to exclude notifications/ringtones

**Usage:**
```kotlin
@Inject
lateinit var mediaScanner: MediaScanner

// Basic scan with progress
mediaScanner.scanAudioFiles(minDuration = 30_000).collect { result ->
    result.onSuccess { data ->
        when (data) {
            is MediaScanner.ScanProgress -> {
                // Update UI with progress
                updateProgress(data.percentage)
            }
            is MediaScanner.ScanResult -> {
                // Scan complete, process results
                processAudioFiles(data.audioFiles)
            }
        }
    }
}

// Detailed scan with enriched metadata
mediaScanner.scanAudioFilesWithDetailedMetadata().collect { result ->
    // This extracts bitrate, sample rate, etc.
}

// Get album artwork
val artworkUri = mediaScanner.getAlbumArtwork(albumId)

// Get audio folders
val folders = mediaScanner.getAudioFolders()
```

**Key Methods:**
- `scanAudioFiles(minDuration)` - Basic scan with MediaStore data
- `scanAudioFilesWithDetailedMetadata(minDuration)` - Detailed scan with MediaMetadataRetriever
- `getAlbumArtwork(albumId)` - Get album artwork URI
- `getAudioFolders()` - Get list of folders containing audio files

### 2. MetadataExtractor.kt

Extracts detailed metadata from audio files using MediaMetadataRetriever.

**Features:**
- Extracts comprehensive metadata (title, artist, album, year, genre, duration, bitrate, sample rate)
- Reads and caches album artwork
- Supports MP3, FLAC, AAC, OGG, WAV, M4A, OPUS formats
- Handles corrupted or unsupported files gracefully
- Caches album art to disk

**Usage:**
```kotlin
@Inject
lateinit var metadataExtractor: MetadataExtractor

// Extract metadata
val metadata = metadataExtractor.extractMetadata(audioUri)
println("Title: ${metadata.title}")
println("Bitrate: ${metadata.bitrate} bps")

// Extract and cache album art
val artPath = metadataExtractor.extractAndCacheAlbumArt(audioUri, albumId)

// Get album art as bitmap
val bitmap = metadataExtractor.extractAlbumArtBitmap(audioUri)

// Format values for display
val bitrateStr = metadataExtractor.formatBitrate(320000) // "320 kbps"
val durationStr = metadataExtractor.formatDuration(225000) // "3:45"

// Clear cache
metadataExtractor.clearCache(clearDisk = true)
```

**Key Methods:**
- `extractMetadata(uri)` - Extract all metadata
- `extractAndCacheAlbumArt(uri, albumId)` - Extract and cache album art
- `extractAlbumArtBitmap(uri)` - Get album art as Bitmap
- `extractAlbumArtBytes(uri)` - Get album art as byte array
- `isFormatSupported(mimeType)` - Check format support
- `formatBitrate(bitrate)`, `formatDuration(duration)` - Format values

### 3. FolderBrowser.kt

Provides folder-based browsing functionality for audio files.

**Features:**
- Lists audio files by folder structure
- Supports folder hierarchy navigation
- Filters audio files from other content
- Calculates folder statistics (file count, duration, size)
- Search folders by name

**Usage:**
```kotlin
@Inject
lateinit var folderBrowser: FolderBrowser

// Get all folders with audio files
folderBrowser.getAllAudioFolders().collect { result ->
    result.onSuccess { folders ->
        folders.forEach { folder ->
            println("${folder.name}: ${folder.audioFileCount} files")
        }
    }
}

// Browse a specific folder
folderBrowser.browseFolder(
    folderPath = "/storage/emulated/0/Music",
    includeSubfolders = false
).collect { result ->
    result.onSuccess { browseResult ->
        println("Files: ${browseResult.audioFiles.size}")
        println("Subfolders: ${browseResult.subFolders.size}")
    }
}

// Get folder statistics
val stats = folderBrowser.getFolderStatistics(
    folderPath = "/storage/emulated/0/Music",
    includeSubfolders = true
)

// Search folders
folderBrowser.searchFolders("Rock").collect { result ->
    result.onSuccess { folders ->
        // Display matching folders
    }
}

// Format values
val sizeStr = folderBrowser.formatSize(1024 * 1024 * 100) // "100.0 MB"
val durationStr = folderBrowser.formatTotalDuration(3600000) // "1h 0m"
```

**Key Methods:**
- `getAllAudioFolders(minDuration)` - Get all folders with audio
- `browseFolder(path, includeSubfolders, minDuration)` - Browse specific folder
- `getAudioFilesInFolder(path, minDuration)` - Get files in folder
- `getFolderStatistics(path, includeSubfolders, minDuration)` - Get folder stats
- `searchFolders(query, minDuration)` - Search folders by name

### 4. MediaStoreObserver.kt

Observes changes to the MediaStore and provides notifications when media files change.

**Features:**
- Monitors MediaStore for changes using ContentObserver
- Detects when new media files are added/removed/modified
- Triggers automatic re-scanning when changes occur
- Provides Flow of media change events
- Supports granular change detection (audio, video, images)

**Usage:**
```kotlin
@Inject
lateinit var mediaStoreObserver: MediaStoreObserver

// Observe audio changes with detailed events
mediaStoreObserver.observeAudioChanges().collect { event ->
    when (event) {
        is MediaChangeEvent.MediaAdded -> {
            // New audio file added
            triggerRescan()
        }
        is MediaChangeEvent.MediaDeleted -> {
            // Audio file deleted
            updateDatabase()
        }
        is MediaChangeEvent.MediaModified -> {
            // Audio file modified
            refreshMetadata()
        }
        is MediaChangeEvent.UnknownChange -> {
            // General change, trigger full rescan
            fullRescan()
        }
    }
}

// Simple observation (just get notified of changes)
mediaStoreObserver.observeAudioChangesSimple().collect {
    // Any change occurred, trigger rescan
    rescanLibrary()
}

// Check for new media since last scan
val hasNewMedia = mediaStoreObserver.hasNewMediaSince(lastScanTimestamp)

// Manually trigger scan for a specific file
mediaStoreObserver.scanFile("/path/to/audio.mp3")
```

**Key Methods:**
- `observeAudioChanges(notifyForDescendants)` - Observe audio changes
- `observeAllMediaChanges(notifyForDescendants)` - Observe all media changes
- `observeAudioChangesSimple()` - Simple change notification
- `hasNewMediaSince(timestamp, mediaType)` - Check for new media
- `getLastMediaChangeTimestamp(mediaType)` - Get last change time
- `scanFile(path)` - Trigger scan for specific file

## Integration Example

Here's a complete example of integrating the media scanning module:

```kotlin
@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val mediaScanner: MediaScanner,
    private val metadataExtractor: MetadataExtractor,
    private val folderBrowser: FolderBrowser,
    private val mediaStoreObserver: MediaStoreObserver,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) : ViewModel() {

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    init {
        // Start observing media changes
        observeMediaChanges()
    }

    fun scanLibrary() {
        viewModelScope.launch {
            mediaScanner.scanAudioFilesWithDetailedMetadata(minDuration = 30_000)
                .collect { result ->
                    result.onSuccess { data ->
                        when (data) {
                            is MediaScanner.ScanProgress -> {
                                _scanProgress.value = data
                            }
                            is MediaScanner.ScanResult -> {
                                // Save to database
                                saveToDatabase(data.audioFiles)
                                _scanProgress.value = null
                            }
                        }
                    }
                }
        }
    }

    private fun observeMediaChanges() {
        viewModelScope.launch {
            mediaStoreObserver.observeAudioChangesSimple().collect {
                // Auto-rescan when media changes
                delay(2000) // Debounce
                scanLibrary()
            }
        }
    }

    private suspend fun saveToDatabase(audioFiles: List<MediaScanner.AudioFile>) {
        // Group by artist and album
        val artists = audioFiles.groupBy { it.artist }.map { (name, files) ->
            Artist(name = name, songCount = files.size)
        }
        
        val albums = audioFiles.groupBy { it.album }.map { (name, files) ->
            val firstFile = files.first()
            Album(
                name = name,
                artistId = 0, // Will be updated with proper ID
                year = firstFile.year,
                artworkUri = firstFile.albumId.toString(),
                songCount = files.size
            )
        }

        // Insert into database
        artistDao.insertAll(artists)
        albumDao.insertAll(albums)
        // ... insert songs
    }
}
```

## Permissions

The module requires the following permissions (already defined in AndroidManifest.xml):

```xml
<!-- For Android 12 and below -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- For Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

## Dependency Injection

All classes are annotated with `@Singleton` and use Hilt for dependency injection. Make sure your Application class is annotated with `@HiltAndroidApp`:

```kotlin
@HiltAndroidApp
class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
```

## Performance Considerations

1. **Scan Duration**: Basic scan is fast (~1-2 seconds for 1000 files). Detailed scan with metadata extraction takes longer (~10-20 seconds for 1000 files).

2. **Memory Usage**: The scanner processes files in a streaming fashion and doesn't load all data into memory at once.

3. **Album Art Caching**: Album art is cached to disk to avoid repeated extraction. Clear cache periodically if needed.

4. **Minimum Duration Filter**: Default 30 seconds filters out ringtones and notification sounds.

5. **Background Scanning**: Use a foreground service or WorkManager for long-running scans.

## Error Handling

All methods handle errors gracefully and use Timber for logging. Errors are emitted as `Result.failure()` in Flow-based APIs.

```kotlin
mediaScanner.scanAudioFiles().collect { result ->
    result.onSuccess { data ->
        // Handle success
    }.onFailure { exception ->
        // Handle error
        Timber.e(exception, "Scan failed")
        showError(exception.message)
    }
}
```

## Testing

The module is designed to be testable. Mock the dependencies in your tests:

```kotlin
@Test
fun `test media scanning`() = runTest {
    val mockContext = mockk<Context>()
    val mockMetadataExtractor = mockk<MetadataExtractor>()
    
    val scanner = MediaScanner(mockContext, mockMetadataExtractor)
    
    // Test scanning logic
}
```

## Supported Audio Formats

- MP3 (MPEG Layer 3)
- FLAC (Free Lossless Audio Codec)
- AAC (Advanced Audio Coding)
- OGG Vorbis
- WAV (Waveform Audio File Format)
- M4A (MPEG-4 Audio)
- OPUS
- WMA (Windows Media Audio)
- AMR (Adaptive Multi-Rate)

## License

Part of the Music Player application.
