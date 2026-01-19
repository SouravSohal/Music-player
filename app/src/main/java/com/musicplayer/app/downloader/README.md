# Media Downloader Module

## Overview

The Media Downloader module provides comprehensive functionality for downloading video and audio content from online sources. It is designed with production-quality standards, including robust error handling, progress tracking, resume capability, and most importantly, **legal and ethical compliance**.

## ⚖️ Legal Disclaimer and Important Notice

### **CRITICAL: Legal Responsibility**

**BY USING THIS DOWNLOADER MODULE, USERS ACKNOWLEDGE AND ACCEPT FULL RESPONSIBILITY FOR ENSURING THEY HAVE THE LEGAL RIGHT TO DOWNLOAD CONTENT.**

This module is designed for legitimate use cases only:

✅ **PERMITTED USES:**
- Downloading content you own or created
- Downloading content with explicit download permissions from the creator/platform
- Downloading public domain content
- Downloading openly licensed content (Creative Commons, etc.)
- Downloading from platforms that explicitly permit downloading in their Terms of Service
- Educational and research purposes where legally permitted
- Personal backups of content you have legal access to

❌ **PROHIBITED USES:**
- Downloading copyrighted content without permission
- Downloading from platforms that prohibit downloading in their Terms of Service
- Downloading DRM-protected content
- Commercial redistribution of downloaded content without rights
- Circumventing access controls or paywalls
- Any use that violates copyright laws or platform terms of service

### Legal Compliance Features

The downloader includes several features to promote legal compliance:

1. **Whitelist System**: Only pre-approved domains known to permit downloading are supported
2. **User Consent**: Applications must implement user consent mechanisms before downloads
3. **Copyright Notice**: Clear warnings displayed to users about legal responsibilities
4. **Robots.txt Respect**: The module respects standard web crawling policies
5. **Terms of Service**: Documentation encourages review of platform ToS before adding domains

### Developer Responsibilities

Developers integrating this module must:

1. **Implement User Consent**: Show legal disclaimer and obtain explicit consent
2. **Verify Domain Legality**: Only add domains to whitelist that explicitly permit downloading
3. **Display Copyright Notices**: Inform users of their legal obligations
4. **Monitor Compliance**: Regularly review and update whitelist based on ToS changes
5. **Handle DMCA Requests**: Implement procedures for responding to copyright complaints

### Supported Domains (Whitelist)

The module includes a whitelist of domains that either:
- Explicitly permit downloading in their terms of service
- Provide public domain or openly licensed content
- Offer user-controlled download permissions

**Current whitelisted domains include:**
- `archive.org` - Internet Archive (Public Domain)
- `commons.wikimedia.org` - Wikimedia Commons (Open Licenses)
- `freesound.org` - Creative Commons audio
- `soundcloud.com` - User-controlled downloads
- `vimeo.com` - User-controlled privacy settings
- `ted.com` - Educational content
- `khanacademy.org` - Educational resources

**To add new domains:**
1. Verify the platform's Terms of Service explicitly permit downloading
2. Document the legal basis for inclusion
3. Add to `UrlValidator.WHITELISTED_DOMAINS`
4. Update this documentation

## Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                      DownloadService                        │
│  (Foreground Service - Background Downloads & Notifications)│
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      DownloadManager                        │
│  (Queue Management, Concurrency Control, Pause/Resume)      │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                     MediaDownloader                         │
│  (Core Download Logic, Progress Tracking, Resume Support)   │
└────────────────────────────┬────────────────────────────────┘
                             │
        ┌────────────────────┴────────────────────┐
        │                                         │
┌───────▼──────────┐                   ┌─────────▼──────────┐
│  UrlValidator    │                   │   DownloadTask     │
│  (URL Validation │                   │   (Data Model)     │
│   & Whitelisting)│                   │                    │
└──────────────────┘                   └────────────────────┘
```

### 1. **DownloadTask.kt**

Data class representing a download task with comprehensive state tracking.

**Key Features:**
- Unique task identification
- Progress tracking (bytes, percentage, speed)
- Status management (pending, downloading, paused, completed, failed, cancelled)
- Resume capability with tokens
- Time estimation
- Quality settings (Low 360p, Medium 480p, High 720p, Very High 1080p, Best)
- Media type (Video or Audio-Only)

**Usage:**
```kotlin
val task = DownloadTask(
    sourceUrl = "https://example.com/video.mp4",
    destinationPath = "/storage/emulated/0/Music/video.m4a",
    mediaType = MediaType.AUDIO_ONLY,
    quality = DownloadQuality.HIGH
)
```

### 2. **UrlValidator.kt**

Validates URLs and enforces legal compliance through whitelisting.

**Key Features:**
- URL format validation
- Domain whitelist checking
- Reachability testing
- Media ID extraction for supported platforms
- Platform detection (YouTube, Vimeo, SoundCloud, etc.)

**Usage:**
```kotlin
val validator = UrlValidator(okHttpClient)
val result = validator.validateUrl("https://archive.org/details/example")

when (result) {
    is ValidationResult.Valid -> {
        // Proceed with download
        println("Valid: ${result.url}")
    }
    is ValidationResult.NotSupported -> {
        // Show error to user
        println("Not supported: ${result.reason}")
    }
    is ValidationResult.Invalid -> {
        println("Invalid URL: ${result.reason}")
    }
    is ValidationResult.Unreachable -> {
        println("Cannot reach URL: ${result.reason}")
    }
}
```

### 3. **MediaDownloader.kt**

Core downloader implementation with resume support and progress tracking.

**Key Features:**
- HTTP Range requests for resume capability
- Real-time progress via Kotlin Flow
- Download speed calculation
- Comprehensive error handling
- Concurrent download tracking
- Automatic retry with exponential backoff

**Usage:**
```kotlin
val downloader = MediaDownloader(okHttpClient, urlValidator)

downloader.downloadMedia(task).collect { updatedTask ->
    when (updatedTask.status) {
        DownloadStatus.DOWNLOADING -> {
            updateUI(
                progress = updatedTask.progress,
                speed = updatedTask.formatSpeed()
            )
        }
        DownloadStatus.COMPLETED -> {
            showSuccess()
        }
        DownloadStatus.FAILED -> {
            showError(updatedTask.errorMessage)
        }
        else -> {}
    }
}
```

### 4. **DownloadManager.kt**

Manages download queue with concurrency control and retry logic.

**Key Features:**
- Queue system for multiple downloads
- Configurable concurrent download limit (default: 2)
- Automatic retry for failed downloads (max 3 attempts)
- Pause/resume individual or all downloads
- Cancel individual or all downloads
- Shared Flow for observing all download state changes
- Automatic queue processing

**Usage:**
```kotlin
val manager = DownloadManager(mediaDownloader)

// Add download
manager.addDownload(task)

// Observe all downloads
manager.downloadStates.collect { task ->
    updateDownloadList(task)
}

// Control downloads
manager.pauseDownload(taskId)
manager.resumeDownload(taskId)
manager.cancelDownload(taskId)

// Query state
val activeDownloads = manager.getActiveDownloads()
val completedDownloads = manager.getCompletedDownloads()
```

### 5. **DownloadService.kt**

Foreground service for background downloads with notifications.

**Key Features:**
- Runs as foreground service (persists when app is backgrounded)
- Progress notifications for each download
- Summary notification showing overall status
- Action buttons (Pause, Resume, Cancel)
- Automatic service lifecycle management
- Handles system interruptions

**Usage:**
```kotlin
// Start download
DownloadService.startDownload(context, task)

// Control from notification or app
DownloadService.pauseDownload(context, taskId)
DownloadService.resumeDownload(context, taskId)
DownloadService.cancelDownload(context, taskId)
DownloadService.cancelAllDownloads(context)
```

## Integration Guide

### 1. Add Required Permissions

Add to `AndroidManifest.xml`:

```xml
<!-- Internet access for downloads -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Storage access -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Wake lock to prevent interruptions -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### 2. Register Service

Add to `AndroidManifest.xml`:

```xml
<service
    android:name=".downloader.DownloadService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

### 3. Request Runtime Permissions

```kotlin
// Request storage and notification permissions
val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(Manifest.permission.POST_NOTIFICATIONS)
} else {
    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}

requestPermissions(permissions, REQUEST_CODE)
```

### 4. Initialize with Hilt

The module uses Hilt for dependency injection. Ensure your Application class is annotated:

```kotlin
@HiltAndroidApp
class MusicPlayerApplication : Application() {
    // ...
}
```

### 5. Inject and Use

```kotlin
@AndroidEntryPoint
class DownloadActivity : AppCompatActivity() {
    
    @Inject
    lateinit var downloadManager: DownloadManager
    
    @Inject
    lateinit var urlValidator: UrlValidator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show legal disclaimer and get consent
        showLegalDisclaimer()
        
        // Start download
        lifecycleScope.launch {
            val task = createDownloadTask()
            downloadManager.addDownload(task)
            DownloadService.startDownload(this@DownloadActivity, task)
        }
        
        // Observe downloads
        lifecycleScope.launch {
            downloadManager.downloadStates.collect { task ->
                updateUI(task)
            }
        }
    }
    
    private fun showLegalDisclaimer() {
        AlertDialog.Builder(this)
            .setTitle("Legal Notice")
            .setMessage("""
                You are responsible for ensuring you have the legal right to download content.
                
                Only download:
                - Content you own or created
                - Content with explicit permission
                - Public domain or openly licensed content
                
                Downloading copyrighted content without permission is illegal.
                Do you agree to these terms?
            """.trimIndent())
            .setPositiveButton("I Agree") { _, _ -> 
                // Proceed with download
            }
            .setNegativeButton("Cancel") { _, _ -> 
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
```

## API Reference

### DownloadTask

```kotlin
data class DownloadTask(
    val id: String,
    val sourceUrl: String,
    val destinationPath: String,
    val mediaType: MediaType,
    val quality: DownloadQuality,
    val status: DownloadStatus,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val downloadSpeedBytesPerSec: Long,
    // ... other properties
)
```

### DownloadManager Methods

```kotlin
suspend fun addDownload(task: DownloadTask): Boolean
suspend fun pauseDownload(taskId: String): Boolean
suspend fun resumeDownload(taskId: String): Boolean
suspend fun cancelDownload(taskId: String): Boolean
suspend fun retryDownload(taskId: String): Boolean
suspend fun cancelAllDownloads()
suspend fun pauseAllDownloads()
suspend fun resumeAllDownloads()

fun getDownload(taskId: String): DownloadTask?
fun getAllDownloads(): List<DownloadTask>
fun getActiveDownloads(): List<DownloadTask>
fun getCompletedDownloads(): List<DownloadTask>
fun getFailedDownloads(): List<DownloadTask>
```

## Error Handling

The module provides comprehensive error handling:

```kotlin
downloadManager.downloadStates.collect { task ->
    when (task.status) {
        DownloadStatus.FAILED -> {
            when {
                task.errorMessage?.contains("Invalid URL") == true -> {
                    // Handle invalid URL
                }
                task.errorMessage?.contains("not supported") == true -> {
                    // Handle unsupported domain
                }
                task.errorMessage?.contains("HTTP") == true -> {
                    // Handle network error
                }
                else -> {
                    // Handle other errors
                }
            }
            
            // Automatic retry is handled by DownloadManager
            // Manual retry:
            if (task.retryCount < 3) {
                downloadManager.retryDownload(task.id)
            }
        }
    }
}
```

## Best Practices

### 1. Always Show Legal Disclaimer

```kotlin
fun showDownloadConsent(onAccept: () -> Unit) {
    // Show comprehensive legal notice
    // Get explicit user consent
    // Document consent in app logs/analytics
}
```

### 2. Validate URLs Before Download

```kotlin
lifecycleScope.launch {
    val result = urlValidator.validateUrl(url)
    if (result is ValidationResult.Valid) {
        startDownload()
    } else {
        showError(result.toString())
    }
}
```

### 3. Handle Storage Properly

```kotlin
// Use app-specific storage (doesn't require permission on Android 10+)
val downloadsDir = File(context.getExternalFilesDir(null), "downloads")
downloadsDir.mkdirs()

// Or use MediaStore for shared storage
```

### 4. Monitor Network Conditions

```kotlin
val connectivityManager = getSystemService(ConnectivityManager::class.java)
connectivityManager.registerNetworkCallback(
    NetworkRequest.Builder().build(),
    object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            // Pause downloads on network loss
            downloadManager.pauseAllDownloads()
        }
    }
)
```

### 5. Clean Up Resources

```kotlin
override fun onDestroy() {
    super.onDestroy()
    downloadManager.cleanup()
}
```

## Testing

### Unit Tests

```kotlin
@Test
fun `test URL validation with whitelisted domain`() = runTest {
    val validator = UrlValidator(mockOkHttpClient)
    val result = validator.validateUrl("https://archive.org/details/test")
    assertTrue(result is ValidationResult.Valid)
}

@Test
fun `test URL validation with non-whitelisted domain`() = runTest {
    val validator = UrlValidator(mockOkHttpClient)
    val result = validator.validateUrl("https://illegal-site.com/video")
    assertTrue(result is ValidationResult.NotSupported)
}
```

### Integration Tests

```kotlin
@Test
fun `test complete download flow`() = runTest {
    val task = DownloadTask(
        sourceUrl = "https://archive.org/download/test.mp4",
        destinationPath = testFile.absolutePath,
        mediaType = MediaType.VIDEO,
        quality = DownloadQuality.MEDIUM
    )
    
    var completed = false
    downloader.downloadMedia(task).collect { 
        if (it.status == DownloadStatus.COMPLETED) {
            completed = true
        }
    }
    
    assertTrue(completed)
    assertTrue(testFile.exists())
}
```

## Performance Considerations

- **Concurrent Downloads**: Limited to 2 by default to prevent network saturation
- **Buffer Size**: 8KB buffer for efficient I/O
- **Progress Updates**: Throttled to 500ms intervals to reduce overhead
- **Speed Calculation**: Uses 2-second rolling window for accurate speed metrics
- **Memory**: Streams data directly to disk, minimal memory footprint

## Security Considerations

1. **HTTPS Only**: Enforce HTTPS URLs when possible
2. **File Validation**: Validate file types after download
3. **Path Traversal**: Sanitize destination paths
4. **Size Limits**: Implement maximum file size limits
5. **Timeout Protection**: Configure appropriate timeouts

## Troubleshooting

### Downloads Not Starting

- Check internet connectivity
- Verify storage permissions
- Ensure destination directory exists
- Check domain is whitelisted

### Resume Not Working

- Server must support HTTP Range requests
- Check ETag or Last-Modified headers
- Verify file wasn't modified on server

### Service Stops Unexpectedly

- Check battery optimization settings
- Ensure foreground service is properly started
- Verify notification channel is created

## Future Enhancements

- [ ] Download scheduling (off-peak hours)
- [ ] Bandwidth limiting
- [ ] Parallel segment downloading
- [ ] Download history persistence with Room
- [ ] Network type preferences (WiFi only)
- [ ] Integration with platform-specific APIs (YouTube API, etc.)
- [ ] Metadata extraction and tagging
- [ ] Playlist/batch download support

## License Compliance

This module is designed for use with legally accessible content only. Developers and users must:

1. Respect copyright laws in their jurisdiction
2. Honor platform Terms of Service
3. Obtain proper licenses for content
4. Implement DMCA compliance procedures
5. Monitor and enforce legal usage

## Support and Contributions

When contributing:
- Do NOT add domains that prohibit downloading
- Document legal basis for any whitelist changes
- Include tests for new features
- Follow existing code style and conventions
- Update documentation

## Conclusion

The Media Downloader module provides production-ready downloading capabilities with a strong emphasis on legal compliance, user experience, and reliability. Proper integration requires understanding and respecting the legal responsibilities involved in content downloading.

**Remember: With great power comes great responsibility. Use this module ethically and legally.**
