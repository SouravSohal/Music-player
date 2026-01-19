package com.musicplayer.app.effects

import android.media.audiofx.PresetReverb
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing PresetReverb audio effect.
 * Provides various reverb presets for different acoustic environments.
 */
@Singleton
class PresetReverbController @Inject constructor() {

    private var presetReverb: PresetReverb? = null
    private var isEnabled: Boolean = false
    private var currentPreset: Short = PresetReverb.PRESET_NONE.toShort()

    /**
     * Initialize the preset reverb effect with an audio session ID.
     * 
     * @param audioSessionId The audio session ID from the media player
     * @return True if initialization was successful, false otherwise
     */
    fun initialize(audioSessionId: Int): Boolean {
        return try {
            release()
            presetReverb = PresetReverb(0, audioSessionId).apply {
                enabled = false
            }
            Timber.d("PresetReverb initialized with session ID: $audioSessionId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize PresetReverb")
            false
        }
    }

    /**
     * Enable or disable the reverb effect.
     * 
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        try {
            presetReverb?.enabled = enabled
            isEnabled = enabled
            Timber.d("PresetReverb enabled: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set PresetReverb enabled state")
        }
    }

    /**
     * Get whether the reverb is enabled.
     * 
     * @return True if enabled, false otherwise
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Set the reverb preset.
     * 
     * @param preset The reverb preset to apply
     */
    fun setPreset(preset: ReverbPreset) {
        setPreset(preset.value)
    }

    /**
     * Set the reverb preset by value.
     * 
     * @param preset The reverb preset value to apply
     */
    fun setPreset(preset: Short) {
        try {
            val androidPreset = when (preset.toInt()) {
                0 -> PresetReverb.PRESET_NONE
                1 -> PresetReverb.PRESET_SMALLROOM
                2 -> PresetReverb.PRESET_MEDIUMROOM
                3 -> PresetReverb.PRESET_LARGEROOM
                4 -> PresetReverb.PRESET_MEDIUMHALL
                5 -> PresetReverb.PRESET_LARGEHALL
                6 -> PresetReverb.PRESET_PLATE
                else -> PresetReverb.PRESET_NONE
            }.toShort()
            
            presetReverb?.preset = androidPreset
            currentPreset = preset
            Timber.d("PresetReverb preset set to: ${ReverbPreset.fromValue(preset).displayName}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set PresetReverb preset")
        }
    }

    /**
     * Get the current reverb preset.
     * 
     * @return Current preset value
     */
    fun getPreset(): Short {
        return try {
            currentPreset
        } catch (e: Exception) {
            Timber.e(e, "Failed to get PresetReverb preset")
            PresetReverb.PRESET_NONE.toShort()
        }
    }

    /**
     * Get the current reverb preset as enum.
     * 
     * @return Current preset as ReverbPreset enum
     */
    fun getPresetEnum(): ReverbPreset {
        return ReverbPreset.fromValue(getPreset())
    }

    /**
     * Get all available reverb presets.
     * 
     * @return List of all reverb presets
     */
    fun getAvailablePresets(): List<ReverbPreset> {
        return ReverbPreset.entries.toList()
    }

    /**
     * Check if a specific preset is available.
     * Some devices may not support all presets.
     * 
     * @param preset The preset to check
     * @return True if available, false otherwise
     */
    fun isPresetAvailable(preset: ReverbPreset): Boolean {
        return try {
            // All standard presets should be available
            // This method is here for future extensibility
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to check preset availability")
            false
        }
    }

    /**
     * Get preset information as a formatted string.
     * 
     * @param preset The preset to get info for
     * @return Formatted string with preset information
     */
    fun getPresetInfo(preset: ReverbPreset): String {
        return when (preset) {
            ReverbPreset.NONE -> "No reverb effect applied"
            ReverbPreset.SMALL_ROOM -> "Simulates a small room acoustic environment"
            ReverbPreset.MEDIUM_ROOM -> "Simulates a medium-sized room acoustic environment"
            ReverbPreset.LARGE_ROOM -> "Simulates a large room acoustic environment"
            ReverbPreset.MEDIUM_HALL -> "Simulates a medium-sized concert hall"
            ReverbPreset.LARGE_HALL -> "Simulates a large concert hall"
            ReverbPreset.PLATE -> "Simulates a plate reverb effect"
        }
    }

    /**
     * Reset to no reverb effect.
     */
    fun reset() {
        setPreset(ReverbPreset.NONE)
    }

    /**
     * Check if the reverb is initialized.
     * 
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return presetReverb != null
    }

    /**
     * Get the Android PresetReverb instance.
     * For advanced usage only.
     * 
     * @return The underlying PresetReverb instance, or null if not initialized
     */
    fun getReverbInstance(): PresetReverb? {
        return presetReverb
    }

    /**
     * Release the reverb resources.
     * Should be called when no longer needed.
     */
    fun release() {
        try {
            presetReverb?.release()
            presetReverb = null
            isEnabled = false
            currentPreset = PresetReverb.PRESET_NONE.toShort()
            Timber.d("PresetReverb released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release PresetReverb")
        }
    }
}
