# Video to Audio Converter Module

A comprehensive video-to-audio converter module for the Music Player Android application.

## Overview

This module provides hardware-accelerated video-to-audio conversion with support for multiple formats, quality levels, and real-time progress tracking.

## Features

- **Multiple Output Formats**: MP3, AAC, M4A, FLAC, OGG Vorbis
- **Quality Selection**: 128, 192, 256, 320 kbps bitrates
- **Hardware Acceleration**: Uses Android MediaCodec API for efficient processing
- **Progress Tracking**: Real-time progress updates via Kotlin Flow
- **Queue Management**: Concurrent conversion with configurable limits
- **Pause/Resume**: Control conversions with pause/resume capabilities
- **Storage Management**: Organized output directory structure
- **Error Handling**: Comprehensive error handling with retry logic
- **Hilt Integration**: Full dependency injection support

## Architecture

### Components

1. **ConversionTask.kt** - Data model for conversion tasks
   - Tracks conversion state and progress
   - Provides status information
   - Calculates estimates

2. **AudioEncoder.kt** - Audio encoding engine
   - Encodes PCM audio to various formats
   - Uses MediaCodec for hardware acceleration
   - Supports multiple bitrates and sample rates

3. **VideoToAudioConverter.kt** - Main conversion engine
   - Extracts audio from video files
   - Decodes to PCM and re-encodes to target format
   - Provides progress updates via Flow
   - Manages temporary files

4. **ConversionManager.kt** - Queue and lifecycle manager
   - Manages conversion queue
   - Supports concurrent conversions (default: 2 simultaneous)
   - Provides pause/resume/cancel operations
   - Emits conversion events

## Usage

### Basic Conversion

```kotlin
@Inject
lateinit var conversionManager: ConversionManager

fun convertVideo() {
    val videoUri = Uri.parse("content://...")
    val task = conversionManager.addConversion(
        sourceUri = videoUri,
        sourcePath = "/path/to/video.mp4",
        outputFormat = AudioFormat.MP3,
        bitrate = AudioBitrate.HIGH
    )
    
    // Observe conversion progress
    lifecycleScope.launch {
        conversionManager.conversionEvents.collect { event ->
            when (event) {
                is ConversionManager.ConversionEvent.ProgressUpdated -> {
                    updateUI(event.task.progress)
                }
                is ConversionManager.ConversionEvent.Completed -> {
                    showSuccess(event.task.outputPath)
                }
                is ConversionManager.ConversionEvent.Failed -> {
                    showError(event.error)
                }
                else -> { /* Handle other events */ }
            }
        }
    }
}
```

### Monitor All Tasks

```kotlin
lifecycleScope.launch {
    conversionManager.tasks.collect { tasks ->
        // Update UI with all tasks
        adapter.submitList(tasks)
    }
}
```

### Control Conversions

```kotlin
// Pause a conversion
conversionManager.pauseConversion(taskId)

// Resume a paused conversion
conversionManager.resumeConversion(taskId)

// Cancel a conversion
conversionManager.cancelConversion(taskId)

// Cancel all active conversions
conversionManager.cancelAllConversions()
```

### Get Statistics

```kotlin
val stats = conversionManager.getStatistics()
println("Success rate: ${stats.successRate}%")
println("Total output size: ${stats.totalOutputSize} bytes")
```

## Supported Formats

### Input Formats (Video)
- MP4 (H.264, H.265)
- MKV
- AVI
- MOV
- WebM
- FLV
- Any video format supported by Android MediaExtractor

### Output Formats (Audio)

| Format | Extension | MIME Type | Description |
|--------|-----------|-----------|-------------|
| MP3 | .mp3 | audio/mpeg | Universal compatibility |
| AAC | .aac | audio/aac | High quality, efficient |
| M4A | .m4a | audio/mp4 | Apple ecosystem optimized |
| FLAC | .flac | audio/flac | Lossless quality |
| OGG | .ogg | audio/ogg | Open source, high quality |

### Bitrate Options

| Setting | Bitrate | Quality | Use Case |
|---------|---------|---------|----------|
| LOW | 128 kbps | Standard | Podcasts, voice |
| MEDIUM | 192 kbps | Good | General music |
| HIGH | 256 kbps | High | High-quality music |
| VERY_HIGH | 320 kbps | Very High | Audiophile quality |

## File Management

### Output Directory
Converted files are stored in:
```
/Android/data/com.musicplayer.app/files/ConvertedAudio/
```

### File Naming Convention
```
{original_name}_{bitrate}kbps_{timestamp}.{extension}
```

Example: `video_name_320kbps_1642531200000.mp3`

### Temporary Files
Temporary files are stored in:
```
/Android/data/com.musicplayer.app/cache/ConversionTemp/
```

Clean up temporary files:
```kotlin
conversionManager.cleanupTempFiles()
```

## Progress Tracking

The conversion process emits progress updates through multiple stages:

1. **INITIALIZING** (0-5%): Preparing for conversion
2. **EXTRACTING** (5-10%): Extracting audio from video
3. **DECODING** (10-45%): Decoding audio to PCM
4. **ENCODING** (45-90%): Encoding to target format
5. **FINALIZING** (90-100%): Writing final output
6. **COMPLETED**: Conversion finished successfully
7. **ERROR**: Conversion failed

## Error Handling

The module includes automatic retry logic:
- Maximum 2 retry attempts per conversion
- Automatic cleanup of partial files on failure
- Detailed error messages in ConversionTask

Common errors:
- **No audio track**: Video file contains no audio
- **Unsupported format**: Device doesn't support encoding format
- **Storage full**: Insufficient storage space
- **Corrupted file**: Input file is corrupted

## Performance Considerations

### Concurrent Conversions
- Default: 2 simultaneous conversions
- Prevents resource exhaustion
- Automatically queues additional tasks

### Memory Management
- Uses streaming approach (no full file loading)
- Automatic cleanup of temporary files
- Buffer size optimized for mobile devices (1MB)

### Battery Impact
- Hardware-accelerated encoding (low CPU usage)
- Efficient I/O operations
- Cancellation support to save battery

## Testing

Validate a video file before conversion:
```kotlin
val isValid = converter.validateVideoFile(videoUri)
if (!isValid) {
    showError("Video file contains no audio track")
}
```

Check format support:
```kotlin
val supported = audioEncoder.getSupportedFormats()
println("Supported formats: $supported")
```

## Integration with Hilt

The module is fully integrated with Hilt for dependency injection:

```kotlin
@HiltAndroidApp
class MusicPlayerApplication : Application()

@AndroidEntryPoint
class ConversionActivity : AppCompatActivity() {
    @Inject
    lateinit var conversionManager: ConversionManager
}
```

Dependencies are provided by `ConverterModule` in the `di` package.

## Best Practices

1. **Always validate input files** before conversion
2. **Monitor storage space** before starting conversions
3. **Clean up temporary files** periodically
4. **Use appropriate bitrates** for different content types
5. **Handle configuration changes** properly with ViewModel
6. **Cancel conversions** when leaving the screen
7. **Observe conversion events** in lifecycle-aware manner

## Permissions Required

Add these permissions to AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

For Android 10+ (API 29+), use scoped storage:
```xml
<application
    android:requestLegacyExternalStorage="true">
</application>
```

## Example: Complete Implementation

```kotlin
@AndroidEntryPoint
class ConversionFragment : Fragment() {
    
    @Inject
    lateinit var conversionManager: ConversionManager
    
    private val viewModel: ConversionViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        // Observe all tasks
        viewLifecycleOwner.lifecycleScope.launch {
            conversionManager.tasks.collect { tasks ->
                adapter.submitList(tasks)
            }
        }
        
        // Observe events
        viewLifecycleOwner.lifecycleScope.launch {
            conversionManager.conversionEvents.collect { event ->
                handleEvent(event)
            }
        }
    }
    
    private fun startConversion(videoUri: Uri) {
        val task = conversionManager.addConversion(
            sourceUri = videoUri,
            sourcePath = getPathFromUri(videoUri),
            outputFormat = AudioFormat.MP3,
            bitrate = AudioBitrate.HIGH
        )
    }
    
    private fun handleEvent(event: ConversionManager.ConversionEvent) {
        when (event) {
            is ConversionManager.ConversionEvent.Completed -> {
                Toast.makeText(
                    requireContext(),
                    "Conversion completed: ${event.task.outputPath}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is ConversionManager.ConversionEvent.Failed -> {
                Toast.makeText(
                    requireContext(),
                    "Conversion failed: ${event.error}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> { /* Handle other events */ }
        }
    }
}
```

## Limitations

1. **MP3 Encoding**: Android's MediaCodec doesn't natively support MP3 encoding. The module uses AAC encoding and container wrapping as a workaround.

2. **Format Support**: Available formats depend on device hardware and Android version. Use `audioEncoder.getSupportedFormats()` to check.

3. **Concurrent Limit**: Maximum 2 simultaneous conversions to prevent resource exhaustion.

4. **File Size**: Very large video files (>2GB) may cause issues on some devices.

## Troubleshooting

### Conversion fails immediately
- Check if video file contains audio track
- Verify storage permissions
- Check available storage space

### Poor audio quality
- Increase bitrate setting
- Use lossless format (FLAC) if quality is critical
- Check source video audio quality

### Slow conversion
- Reduce concurrent conversion limit
- Close other apps to free resources
- Check if device supports hardware acceleration

## License

Part of the Music Player application.
