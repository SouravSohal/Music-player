package com.musicplayer.app.effects

import android.media.audiofx.BassBoost
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing BassBoost audio effect.
 * Provides bass enhancement with adjustable strength.
 */
@Singleton
class BassBoostController @Inject constructor() {

    private var bassBoost: BassBoost? = null
    private var isEnabled: Boolean = false

    companion object {
        const val MIN_STRENGTH: Short = 0
        const val MAX_STRENGTH: Short = 1000
    }

    /**
     * Initialize the bass boost effect with an audio session ID.
     * 
     * @param audioSessionId The audio session ID from the media player
     * @return True if initialization was successful, false otherwise
     */
    fun initialize(audioSessionId: Int): Boolean {
        return try {
            release()
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = false
            }
            Timber.d("BassBoost initialized with session ID: $audioSessionId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize BassBoost")
            false
        }
    }

    /**
     * Enable or disable the bass boost effect.
     * 
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        try {
            bassBoost?.enabled = enabled
            isEnabled = enabled
            Timber.d("BassBoost enabled: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set BassBoost enabled state")
        }
    }

    /**
     * Get whether the bass boost is enabled.
     * 
     * @return True if enabled, false otherwise
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Set the strength of the bass boost effect.
     * 
     * @param strength The strength value (0-1000)
     */
    fun setStrength(strength: Short) {
        try {
            val clampedStrength = strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)
            bassBoost?.setStrength(clampedStrength)
            Timber.d("BassBoost strength set to: $clampedStrength")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set BassBoost strength")
        }
    }

    /**
     * Get the current bass boost strength.
     * 
     * @return Current strength value (0-1000)
     */
    fun getStrength(): Short {
        return try {
            bassBoost?.roundedStrength ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to get BassBoost strength")
            0
        }
    }

    /**
     * Check if the bass boost effect supports strength parameter.
     * 
     * @return True if strength is supported, false otherwise
     */
    fun supportsStrength(): Boolean {
        return try {
            bassBoost?.strengthSupported ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check BassBoost strength support")
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
     * Check if the bass boost is initialized.
     * 
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return bassBoost != null
    }

    /**
     * Release the bass boost resources.
     * Should be called when no longer needed.
     */
    fun release() {
        try {
            bassBoost?.release()
            bassBoost = null
            isEnabled = false
            Timber.d("BassBoost released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release BassBoost")
        }
    }
}
