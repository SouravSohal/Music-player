package com.musicplayer.app.effects

import android.media.audiofx.Virtualizer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing Virtualizer audio effect.
 * Provides surround/spatial sound enhancement with adjustable strength.
 */
@Singleton
class VirtualizerController @Inject constructor() {

    private var virtualizer: Virtualizer? = null
    private var isEnabled: Boolean = false

    companion object {
        const val MIN_STRENGTH: Short = 0
        const val MAX_STRENGTH: Short = 1000
    }

    /**
     * Initialize the virtualizer effect with an audio session ID.
     * 
     * @param audioSessionId The audio session ID from the media player
     * @return True if initialization was successful, false otherwise
     */
    fun initialize(audioSessionId: Int): Boolean {
        return try {
            release()
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = false
            }
            Timber.d("Virtualizer initialized with session ID: $audioSessionId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Virtualizer")
            false
        }
    }

    /**
     * Enable or disable the virtualizer effect.
     * 
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        try {
            virtualizer?.enabled = enabled
            isEnabled = enabled
            Timber.d("Virtualizer enabled: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set Virtualizer enabled state")
        }
    }

    /**
     * Get whether the virtualizer is enabled.
     * 
     * @return True if enabled, false otherwise
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Set the strength of the virtualizer effect.
     * 
     * @param strength The strength value (0-1000)
     */
    fun setStrength(strength: Short) {
        try {
            val clampedStrength = strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)
            virtualizer?.setStrength(clampedStrength)
            Timber.d("Virtualizer strength set to: $clampedStrength")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set Virtualizer strength")
        }
    }

    /**
     * Get the current virtualizer strength.
     * 
     * @return Current strength value (0-1000)
     */
    fun getStrength(): Short {
        return try {
            virtualizer?.roundedStrength ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Virtualizer strength")
            0
        }
    }

    /**
     * Check if the virtualizer effect supports strength parameter.
     * 
     * @return True if strength is supported, false otherwise
     */
    fun supportsStrength(): Boolean {
        return try {
            virtualizer?.strengthSupported ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check Virtualizer strength support")
            false
        }
    }

    /**
     * Get the strength range supported by this effect.
     * 
     * @return Pair of (min, max) strength values, or null if not available
     */
    fun getStrengthRange(): Pair<Short, Short>? {
        return if (supportsStrength()) {
            Pair(MIN_STRENGTH, MAX_STRENGTH)
        } else {
            null
        }
    }

    /**
     * Set the strength as a percentage (0-100).
     * 
     * @param percentage The strength percentage (0-100)
     */
    fun setStrengthPercent(percentage: Int) {
        val clampedPercent = percentage.coerceIn(0, 100)
        val strength = ((clampedPercent / 100.0) * MAX_STRENGTH).toInt().toShort()
        setStrength(strength)
    }

    /**
     * Get the current strength as a percentage (0-100).
     * 
     * @return Current strength percentage
     */
    fun getStrengthPercent(): Int {
        val strength = getStrength()
        return ((strength.toDouble() / MAX_STRENGTH) * 100).toInt()
    }

    /**
     * Check if speaker virtualization is available.
     * Speaker virtualization provides spatial audio effect for speaker playback.
     * 
     * @return True if available, false otherwise
     */
    fun canVirtualizeSpeakers(): Boolean {
        return try {
            virtualizer?.canVirtualize(Virtualizer.VIRTUALIZATION_MODE_BINAURAL) ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check speaker virtualization support")
            false
        }
    }

    /**
     * Get the virtualization mode.
     * 
     * @return Current virtualization mode
     */
    fun getVirtualizationMode(): Int {
        return try {
            virtualizer?.virtualizationMode ?: Virtualizer.VIRTUALIZATION_MODE_OFF
        } catch (e: Exception) {
            Timber.e(e, "Failed to get virtualization mode")
            Virtualizer.VIRTUALIZATION_MODE_OFF
        }
    }

    /**
     * Force the virtualizer to use a specific virtualization mode.
     * 
     * @param mode The virtualization mode to use
     * @return True if successful, false otherwise
     */
    fun forceVirtualizationMode(mode: Int): Boolean {
        return try {
            virtualizer?.forceVirtualizationMode(mode) ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to force virtualization mode")
            false
        }
    }

    /**
     * Check if the virtualizer is initialized.
     * 
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return virtualizer != null
    }

    /**
     * Release the virtualizer resources.
     * Should be called when no longer needed.
     */
    fun release() {
        try {
            virtualizer?.release()
            virtualizer = null
            isEnabled = false
            Timber.d("Virtualizer released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release Virtualizer")
        }
    }
}
