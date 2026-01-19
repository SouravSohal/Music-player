# Audio Effects Module

Comprehensive audio effects and enhancement system for the Music Player application.

## Overview

This module provides a complete audio effects engine with support for:
- **Equalizer** - Multi-band frequency adjustment with presets
- **Bass Boost** - Low-frequency enhancement
- **Virtualizer** - Spatial/surround sound effects
- **Reverb** - Acoustic environment simulation

## Architecture

### Core Components

#### 1. AudioEffectsManager
The main orchestrator that manages all audio effects.

**Key Features:**
- Unified interface for all effects
- Automatic settings persistence via SharedPreferences
- Real-time effect application
- Thread-safe operations with coroutines
- State management with StateFlow

**Usage:**
```kotlin
@Inject
lateinit var audioEffectsManager: AudioEffectsManager

// Initialize with audio session ID from ExoPlayer
val audioSessionId = player.audioSessionId
audioEffectsManager.initialize(audioSessionId)

// Enable equalizer with preset
audioEffectsManager.setEqualizerEnabled(true)
audioEffectsManager.applyEqualizerPreset(0) // Rock preset

// Enable bass boost
audioEffectsManager.setBassBoostEnabled(true)
audioEffectsManager.setBassBoostStrengthPercent(70)

// Observe state changes
audioEffectsManager.effectsState.collect { state ->
    // Update UI
}
```

#### 2. EqualizerController
Manages Android's Equalizer audio effect.

**Features:**
- Preset management (Rock, Pop, Jazz, Classical, etc.)
- Custom band level adjustments
- Frequency band information
- Band level range queries
- Reset to flat response

**Usage:**
```kotlin
// Get available presets
val presets = equalizerController.getPresets()

// Apply a preset
equalizerController.usePreset(0)

// Custom adjustment
val bands = equalizerController.getFrequencyBands()
equalizerController.setBandLevel(0, 500) // +5dB on band 0

// Get band information
val centerFreq = equalizerController.getCenterFrequency(0)
val range = equalizerController.getBandLevelRange() // [min, max] in millibels
```

#### 3. BassBoostController
Controls bass enhancement effect.

**Features:**
- Adjustable strength (0-1000 or 0-100%)
- Capability detection
- Simple enable/disable

**Usage:**
```kotlin
// Enable with 70% strength
bassBoostController.setEnabled(true)
bassBoostController.setStrengthPercent(70)

// Or use raw value
bassBoostController.setStrength(700)

// Check support
if (bassBoostController.supportsStrength()) {
    // Strength adjustment available
}
```

#### 4. VirtualizerController
Manages spatial/surround sound effects.

**Features:**
- Adjustable strength (0-1000 or 0-100%)
- Virtualization mode control
- Speaker virtualization support

**Usage:**
```kotlin
// Enable spatial effect
virtualizerController.setEnabled(true)
virtualizerController.setStrengthPercent(80)

// Check capabilities
if (virtualizerController.canVirtualizeSpeakers()) {
    // Speaker virtualization available
}
```

#### 5. PresetReverbController
Controls reverb/room simulation effects.

**Features:**
- Multiple reverb presets:
  - None
  - Small Room
  - Medium Room
  - Large Room
  - Medium Hall
  - Large Hall
  - Plate
- Preset information queries

**Usage:**
```kotlin
// Apply reverb preset
presetReverbController.setEnabled(true)
presetReverbController.setPreset(ReverbPreset.LARGE_HALL)

// Get all available presets
val presets = presetReverbController.getAvailablePresets()

// Get preset information
val info = presetReverbController.getPresetInfo(ReverbPreset.PLATE)
```

#### 6. AudioEnhancementSettings
Data class for settings persistence.

**Features:**
- Serializable settings for all effects
- SharedPreferences keys
- Default values

## Integration Guide

### 1. Add to Your PlaybackService

```kotlin
@AndroidEntryPoint
class MusicPlaybackService : Service() {
    
    @Inject
    lateinit var audioEffectsManager: AudioEffectsManager
    
    private lateinit var player: ExoPlayer
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize player
        player = exoPlayer
        
        // Initialize effects with player's audio session
        val audioSessionId = player.audioSessionId
        audioEffectsManager.initialize(audioSessionId)
    }
    
    override fun onDestroy() {
        audioEffectsManager.release()
        player.release()
        super.onDestroy()
    }
}
```

### 2. Create Effects UI

```kotlin
@Composable
fun AudioEffectsScreen(
    audioEffectsManager: AudioEffectsManager
) {
    val state by audioEffectsManager.effectsState.collectAsState()
    
    Column {
        // Equalizer Section
        EqualizerSection(
            enabled = state.equalizerEnabled,
            presets = state.equalizerPresets,
            currentPreset = state.currentPreset,
            bands = state.frequencyBands,
            onEnableChange = { audioEffectsManager.setEqualizerEnabled(it) },
            onPresetChange = { audioEffectsManager.applyEqualizerPreset(it.toShort()) },
            onBandChange = { band, level -> 
                audioEffectsManager.setEqualizerBandLevel(band, level) 
            }
        )
        
        // Bass Boost Section
        BassBoostSection(
            enabled = state.bassBoostEnabled,
            strength = state.bassBoostStrength,
            onEnableChange = { audioEffectsManager.setBassBoostEnabled(it) },
            onStrengthChange = { audioEffectsManager.setBassBoostStrength(it) }
        )
        
        // Virtualizer Section
        VirtualizerSection(
            enabled = state.virtualizerEnabled,
            strength = state.virtualizerStrength,
            onEnableChange = { audioEffectsManager.setVirtualizerEnabled(it) },
            onStrengthChange = { audioEffectsManager.setVirtualizerStrength(it) }
        )
        
        // Reverb Section
        ReverbSection(
            enabled = state.reverbEnabled,
            preset = state.reverbPreset,
            onEnableChange = { audioEffectsManager.setReverbEnabled(it) },
            onPresetChange = { audioEffectsManager.setReverbPreset(it) }
        )
    }
}
```

### 3. Handle Audio Session Changes

When the player is recreated or audio session changes:

```kotlin
// In your playback service or manager
fun onAudioSessionIdChanged(newAudioSessionId: Int) {
    audioEffectsManager.initialize(newAudioSessionId)
}

// ExoPlayer listener
player.addListener(object : Player.Listener {
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        audioEffectsManager.initialize(audioSessionId)
    }
})
```

## Settings Persistence

Settings are automatically persisted to SharedPreferences when changed:

```kotlin
// Manual save (usually not needed)
audioEffectsManager.saveSettings()

// Get current settings
val settings = audioEffectsManager.getSettings()

// Reset all effects
audioEffectsManager.resetAllEffects()
```

## State Management

The module uses Kotlin Flow for reactive state:

```kotlin
// Collect state in a composable
val state by audioEffectsManager.effectsState.collectAsState()

// Collect in a ViewModel
viewModelScope.launch {
    audioEffectsManager.effectsState.collect { state ->
        // Handle state changes
        if (state.isInitialized) {
            // Effects are ready
        }
        if (state.error != null) {
            // Handle error
        }
    }
}
```

## Error Handling

All controllers include comprehensive error handling:

```kotlin
// Check initialization state
if (!audioEffectsManager.isInitialized()) {
    // Effects not initialized
    // Try to initialize with valid audio session ID
}

// Monitor state for errors
audioEffectsManager.effectsState.collect { state ->
    state.error?.let { errorMessage ->
        // Show error to user
        showError(errorMessage)
    }
}
```

## Performance Considerations

1. **Low Latency**: All effects are applied in real-time with minimal latency
2. **Thread Safety**: All operations are thread-safe using coroutines
3. **Memory Efficient**: Effects are properly released when not needed
4. **Battery Friendly**: Effects use native Android APIs for optimal performance

## Best Practices

1. **Initialize Early**: Initialize effects when the player is ready
2. **Release Properly**: Always call `release()` in `onDestroy()`
3. **Handle Errors**: Check `isInitialized()` before using effects
4. **Save Settings**: Settings are auto-saved, but you can manually save with `saveSettings()`
5. **Update UI**: Use StateFlow to reactively update UI based on effect state

## Technical Details

### Audio Session ID
Effects are tied to an audio session ID from the media player:
```kotlin
val audioSessionId = exoPlayer.audioSessionId
```

### Frequency Bands
Equalizer typically has 5 bands with these approximate frequencies:
- Band 0: 60 Hz (Bass)
- Band 1: 230 Hz (Low Mid)
- Band 2: 910 Hz (Mid)
- Band 3: 3600 Hz (High Mid)
- Band 4: 14000 Hz (Treble)

Actual frequencies may vary by device.

### Level Range
Equalizer band levels are in millibels (mB):
- Range: typically -1500 to +1500 mB (-15dB to +15dB)
- 0 mB = no adjustment (flat)
- Positive values = boost
- Negative values = cut

### Strength Values
Bass Boost and Virtualizer use strength values:
- Range: 0-1000
- 0 = effect off
- 1000 = maximum effect
- Use percentage helper methods for easier UI integration

## Dependencies

Required dependencies (already in `build.gradle.kts`):
```kotlin
// Hilt for dependency injection
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-android-compiler:2.48")

// Coroutines for async operations
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Timber for logging
implementation("com.jakewharton.timber:timber:5.0.1")
```

## Testing

Example test cases:

```kotlin
@Test
fun testEqualizerInitialization() {
    val controller = EqualizerController()
    val result = controller.initialize(audioSessionId)
    assertTrue(result)
    assertTrue(controller.isInitialized())
}

@Test
fun testBassBoostStrength() {
    bassBoostController.initialize(audioSessionId)
    bassBoostController.setStrength(500)
    assertEquals(500, bassBoostController.getStrength())
}

@Test
fun testSettingsPersistence() {
    audioEffectsManager.setEqualizerEnabled(true)
    audioEffectsManager.setBassBoostStrength(700)
    audioEffectsManager.saveSettings()
    
    val settings = audioEffectsManager.getSettings()
    assertTrue(settings.equalizerEnabled)
    assertEquals(700, settings.bassBoostStrength.toInt())
}
```

## Troubleshooting

### Effects Not Working
1. Check if audio session ID is valid (> 0)
2. Verify effects are enabled
3. Check device capabilities (some devices have limited support)
4. Ensure proper permissions (usually not required for audio effects)

### Settings Not Persisting
1. Verify SharedPreferences is accessible
2. Check for proper context injection
3. Ensure `saveSettings()` is called if not auto-saving

### Crashes on Some Devices
1. Wrap initialization in try-catch (already done)
2. Check for null values before using effects
3. Verify device supports the audio effect APIs

## License

Part of the Music Player application.
