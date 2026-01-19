package com.musicplayer.app.effects

import android.media.audiofx.Equalizer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing Android Equalizer audio effect.
 * Provides preset management, custom band adjustments, and frequency band information.
 */
@Singleton
class EqualizerController @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var isEnabled: Boolean = false

    /**
     * Initialize the equalizer with an audio session ID.
     * 
     * @param audioSessionId The audio session ID from the media player
     * @return True if initialization was successful, false otherwise
     */
    fun initialize(audioSessionId: Int): Boolean {
        return try {
            release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = false
            }
            Timber.d("Equalizer initialized with session ID: $audioSessionId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Equalizer")
            false
        }
    }

    /**
     * Enable or disable the equalizer.
     * 
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            isEnabled = enabled
            Timber.d("Equalizer enabled: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set Equalizer enabled state")
        }
    }

    /**
     * Get whether the equalizer is enabled.
     * 
     * @return True if enabled, false otherwise
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Get the number of available presets.
     * 
     * @return Number of presets, or 0 if not initialized
     */
    fun getNumberOfPresets(): Short {
        return equalizer?.numberOfPresets ?: 0
    }

    /**
     * Get a list of all available equalizer presets.
     * 
     * @return List of equalizer presets
     */
    fun getPresets(): List<EqualizerPreset> {
        val eq = equalizer ?: return emptyList()
        val presets = mutableListOf<EqualizerPreset>()
        
        try {
            val numPresets = eq.numberOfPresets
            for (i in 0 until numPresets) {
                val name = eq.getPresetName(i.toShort())
                presets.add(EqualizerPreset(i, name))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get equalizer presets")
        }
        
        return presets
    }

    /**
     * Apply a preset to the equalizer.
     * 
     * @param presetIndex The index of the preset to apply
     */
    fun usePreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
            Timber.d("Applied equalizer preset: $presetIndex")
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply equalizer preset")
        }
    }

    /**
     * Get the currently applied preset.
     * 
     * @return Current preset index, or -1 if custom or not initialized
     */
    fun getCurrentPreset(): Short {
        return try {
            equalizer?.currentPreset ?: -1
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current preset")
            -1
        }
    }

    /**
     * Get the number of frequency bands.
     * 
     * @return Number of bands, or 0 if not initialized
     */
    fun getNumberOfBands(): Short {
        return equalizer?.numberOfBands ?: 0
    }

    /**
     * Get information about all frequency bands.
     * 
     * @return List of frequency band information
     */
    fun getFrequencyBands(): List<FrequencyBand> {
        val eq = equalizer ?: return emptyList()
        val bands = mutableListOf<FrequencyBand>()
        
        try {
            val numBands = eq.numberOfBands
            for (i in 0 until numBands) {
                val index = i.toShort()
                val centerFreq = eq.getCenterFreq(index)
                val level = eq.getBandLevel(index)
                bands.add(FrequencyBand(index, centerFreq, level))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get frequency bands")
        }
        
        return bands
    }

    /**
     * Get the center frequency of a specific band.
     * 
     * @param band The band index
     * @return Center frequency in millihertz
     */
    fun getCenterFrequency(band: Short): Int {
        return try {
            equalizer?.getCenterFreq(band) ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to get center frequency for band $band")
            0
        }
    }

    /**
     * Get the frequency range for a specific band.
     * 
     * @param band The band index
     * @return Array of [min, max] frequencies in millihertz
     */
    fun getBandFreqRange(band: Short): IntArray? {
        return try {
            equalizer?.getBandFreqRange(band)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get frequency range for band $band")
            null
        }
    }

    /**
     * Get the band level range (min and max values).
     * 
     * @return Array of [min, max] levels in millibels
     */
    fun getBandLevelRange(): ShortArray? {
        return try {
            equalizer?.bandLevelRange
        } catch (e: Exception) {
            Timber.e(e, "Failed to get band level range")
            null
        }
    }

    /**
     * Get the level of a specific band.
     * 
     * @param band The band index
     * @return Band level in millibels
     */
    fun getBandLevel(band: Short): Short {
        return try {
            equalizer?.getBandLevel(band) ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to get band level for band $band")
            0
        }
    }

    /**
     * Set the level of a specific band.
     * 
     * @param band The band index
     * @param level The level in millibels
     */
    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            Timber.d("Set band $band level to $level")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set band level")
        }
    }

    /**
     * Set all band levels at once.
     * 
     * @param levels List of levels for all bands in millibels
     */
    fun setBandLevels(levels: List<Short>) {
        val eq = equalizer ?: return
        try {
            levels.forEachIndexed { index, level ->
                eq.setBandLevel(index.toShort(), level)
            }
            Timber.d("Set all band levels")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set band levels")
        }
    }

    /**
     * Get all current band levels.
     * 
     * @return List of current band levels in millibels
     */
    fun getAllBandLevels(): List<Short> {
        val eq = equalizer ?: return emptyList()
        val levels = mutableListOf<Short>()
        
        try {
            val numBands = eq.numberOfBands
            for (i in 0 until numBands) {
                levels.add(eq.getBandLevel(i.toShort()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all band levels")
        }
        
        return levels
    }

    /**
     * Reset all bands to flat (0 dB).
     */
    fun resetBands() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            for (i in 0 until numBands) {
                eq.setBandLevel(i.toShort(), 0)
            }
            Timber.d("Reset all bands to flat")
        } catch (e: Exception) {
            Timber.e(e, "Failed to reset bands")
        }
    }

    /**
     * Check if the equalizer is initialized.
     * 
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return equalizer != null
    }

    /**
     * Release the equalizer resources.
     * Should be called when no longer needed.
     */
    fun release() {
        try {
            equalizer?.release()
            equalizer = null
            isEnabled = false
            Timber.d("Equalizer released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release Equalizer")
        }
    }
}
