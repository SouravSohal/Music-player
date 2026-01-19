package com.musicplayer.app.effects

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main manager for all audio effects and enhancements.
 * Provides a unified interface for managing equalizer, bass boost, virtualizer, and reverb effects.
 * Handles persistence of effect settings and applies effects to the active audio session.
 */
@Singleton
class AudioEffectsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val equalizerController: EqualizerController,
    private val bassBoostController: BassBoostController,
    private val virtualizerController: VirtualizerController,
    private val presetReverbController: PresetReverbController
) {
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateManager = AudioEffectsStateManager()
    private var currentAudioSessionId: Int? = null
    private var isInitialized = false

    val effectsState: StateFlow<AudioEffectsState> = stateManager.state

    companion object {
        private const val PREFS_NAME = "audio_effects_prefs"
    }

    /**
     * Initialize audio effects with the given audio session ID.
     * This should be called when a media player is ready.
     *
     * @param audioSessionId The audio session ID from the media player
     * @return True if all effects were initialized successfully
     */
    fun initialize(audioSessionId: Int): Boolean {
        Timber.d("Initializing AudioEffectsManager with session ID: $audioSessionId")

        return try {
            // Release previous instances if any
            release()

            currentAudioSessionId = audioSessionId

            // Initialize all controllers
            val eqInit = equalizerController.initialize(audioSessionId)
            val bassInit = bassBoostController.initialize(audioSessionId)
            val virtInit = virtualizerController.initialize(audioSessionId)
            val reverbInit = presetReverbController.initialize(audioSessionId)

            isInitialized = eqInit && bassInit && virtInit && reverbInit

            if (isInitialized) {
                // Load saved settings
                loadSettings()
                
                // Update state with current configuration
                updateState()
                
                Timber.d("AudioEffectsManager initialized successfully")
            } else {
                Timber.e("Failed to initialize some audio effects")
                stateManager.updateState { it.copy(error = "Failed to initialize audio effects") }
            }

            isInitialized
        } catch (e: Exception) {
            Timber.e(e, "Error initializing AudioEffectsManager")
            stateManager.updateState { it.copy(error = e.message) }
            false
        }
    }

    /**
     * Check if the effects manager is initialized.
     *
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Get the current audio session ID.
     *
     * @return Current audio session ID, or null if not initialized
     */
    fun getAudioSessionId(): Int? = currentAudioSessionId

    // ============ Equalizer Methods ============

    /**
     * Enable or disable the equalizer.
     *
     * @param enabled True to enable, false to disable
     */
    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
        saveEqualizerEnabled(enabled)
        updateState()
    }

    /**
     * Get available equalizer presets.
     *
     * @return List of equalizer presets
     */
    fun getEqualizerPresets(): List<EqualizerPreset> {
        return equalizerController.getPresets()
    }

    /**
     * Apply an equalizer preset.
     *
     * @param presetIndex The index of the preset to apply
     */
    fun applyEqualizerPreset(presetIndex: Short) {
        equalizerController.usePreset(presetIndex)
        saveEqualizerPreset(presetIndex.toInt())
        updateState()
    }

    /**
     * Get frequency bands information.
     *
     * @return List of frequency bands
     */
    fun getFrequencyBands(): List<FrequencyBand> {
        return equalizerController.getFrequencyBands()
    }

    /**
     * Set the level of a specific frequency band.
     *
     * @param band The band index
     * @param level The level in millibels
     */
    fun setEqualizerBandLevel(band: Short, level: Short) {
        equalizerController.setBandLevel(band, level)
        saveEqualizerBandLevels()
        updateState()
    }

    /**
     * Set all equalizer band levels.
     *
     * @param levels List of levels for all bands
     */
    fun setEqualizerBandLevels(levels: List<Short>) {
        equalizerController.setBandLevels(levels)
        saveEqualizerBandLevels()
        updateState()
    }

    /**
     * Reset equalizer to flat (all bands at 0 dB).
     */
    fun resetEqualizer() {
        equalizerController.resetBands()
        saveEqualizerBandLevels()
        saveEqualizerPreset(-1)
        updateState()
    }

    /**
     * Get the equalizer band level range.
     *
     * @return Array of [min, max] levels in millibels
     */
    fun getEqualizerBandLevelRange(): ShortArray? {
        return equalizerController.getBandLevelRange()
    }

    // ============ Bass Boost Methods ============

    /**
     * Enable or disable bass boost.
     *
     * @param enabled True to enable, false to disable
     */
    fun setBassBoostEnabled(enabled: Boolean) {
        bassBoostController.setEnabled(enabled)
        saveBassBoostEnabled(enabled)
        updateState()
    }

    /**
     * Set bass boost strength.
     *
     * @param strength The strength value (0-1000)
     */
    fun setBassBoostStrength(strength: Short) {
        bassBoostController.setStrength(strength)
        saveBassBoostStrength(strength)
        updateState()
    }

    /**
     * Set bass boost strength as percentage.
     *
     * @param percentage The strength percentage (0-100)
     */
    fun setBassBoostStrengthPercent(percentage: Int) {
        bassBoostController.setStrengthPercent(percentage)
        saveBassBoostStrength(bassBoostController.getStrength())
        updateState()
    }

    // ============ Virtualizer Methods ============

    /**
     * Enable or disable virtualizer.
     *
     * @param enabled True to enable, false to disable
     */
    fun setVirtualizerEnabled(enabled: Boolean) {
        virtualizerController.setEnabled(enabled)
        saveVirtualizerEnabled(enabled)
        updateState()
    }

    /**
     * Set virtualizer strength.
     *
     * @param strength The strength value (0-1000)
     */
    fun setVirtualizerStrength(strength: Short) {
        virtualizerController.setStrength(strength)
        saveVirtualizerStrength(strength)
        updateState()
    }

    /**
     * Set virtualizer strength as percentage.
     *
     * @param percentage The strength percentage (0-100)
     */
    fun setVirtualizerStrengthPercent(percentage: Int) {
        virtualizerController.setStrengthPercent(percentage)
        saveVirtualizerStrength(virtualizerController.getStrength())
        updateState()
    }

    // ============ Reverb Methods ============

    /**
     * Enable or disable reverb.
     *
     * @param enabled True to enable, false to disable
     */
    fun setReverbEnabled(enabled: Boolean) {
        presetReverbController.setEnabled(enabled)
        saveReverbEnabled(enabled)
        updateState()
    }

    /**
     * Set reverb preset.
     *
     * @param preset The reverb preset to apply
     */
    fun setReverbPreset(preset: ReverbPreset) {
        presetReverbController.setPreset(preset)
        saveReverbPreset(preset.value)
        updateState()
    }

    /**
     * Get available reverb presets.
     *
     * @return List of reverb presets
     */
    fun getReverbPresets(): List<ReverbPreset> {
        return presetReverbController.getAvailablePresets()
    }

    // ============ Settings Persistence ============

    /**
     * Save all current effect settings to SharedPreferences.
     */
    fun saveSettings() {
        scope.launch {
            try {
                saveEqualizerEnabled(equalizerController.isEnabled())
                saveEqualizerPreset(equalizerController.getCurrentPreset().toInt())
                saveEqualizerBandLevels()
                saveBassBoostEnabled(bassBoostController.isEnabled())
                saveBassBoostStrength(bassBoostController.getStrength())
                saveVirtualizerEnabled(virtualizerController.isEnabled())
                saveVirtualizerStrength(virtualizerController.getStrength())
                saveReverbEnabled(presetReverbController.isEnabled())
                saveReverbPreset(presetReverbController.getPreset())
                Timber.d("Audio effects settings saved")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save audio effects settings")
            }
        }
    }

    /**
     * Load effect settings from SharedPreferences and apply them.
     */
    private fun loadSettings() {
        try {
            val settings = getSettings()

            // Apply equalizer settings
            if (settings.equalizerEnabled) {
                equalizerController.setEnabled(true)
                if (settings.equalizerPreset >= 0) {
                    equalizerController.usePreset(settings.equalizerPreset.toShort())
                } else if (settings.equalizerBandLevels.isNotEmpty()) {
                    equalizerController.setBandLevels(settings.equalizerBandLevels.map { it.toShort() })
                }
            }

            // Apply bass boost settings
            if (settings.bassBoostEnabled) {
                bassBoostController.setEnabled(true)
                bassBoostController.setStrength(settings.bassBoostStrength)
            }

            // Apply virtualizer settings
            if (settings.virtualizerEnabled) {
                virtualizerController.setEnabled(true)
                virtualizerController.setStrength(settings.virtualizerStrength)
            }

            // Apply reverb settings
            if (settings.reverbEnabled) {
                presetReverbController.setEnabled(true)
                presetReverbController.setPreset(settings.reverbPreset)
            }

            Timber.d("Audio effects settings loaded and applied")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load audio effects settings")
        }
    }

    /**
     * Get current effect settings.
     *
     * @return AudioEnhancementSettings object with current settings
     */
    fun getSettings(): AudioEnhancementSettings {
        return AudioEnhancementSettings(
            equalizerEnabled = preferences.getBoolean(AudioEnhancementSettings.PREF_KEY_EQUALIZER_ENABLED, false),
            equalizerPreset = preferences.getInt(AudioEnhancementSettings.PREF_KEY_EQUALIZER_PRESET, -1),
            equalizerBandLevels = loadEqualizerBandLevels(),
            bassBoostEnabled = preferences.getBoolean(AudioEnhancementSettings.PREF_KEY_BASS_BOOST_ENABLED, false),
            bassBoostStrength = preferences.getInt(AudioEnhancementSettings.PREF_KEY_BASS_BOOST_STRENGTH, 0).toShort(),
            virtualizerEnabled = preferences.getBoolean(AudioEnhancementSettings.PREF_KEY_VIRTUALIZER_ENABLED, false),
            virtualizerStrength = preferences.getInt(AudioEnhancementSettings.PREF_KEY_VIRTUALIZER_STRENGTH, 0).toShort(),
            reverbEnabled = preferences.getBoolean(AudioEnhancementSettings.PREF_KEY_REVERB_ENABLED, false),
            reverbPreset = preferences.getInt(AudioEnhancementSettings.PREF_KEY_REVERB_PRESET, 0).toShort()
        )
    }

    /**
     * Reset all effects to default settings.
     */
    fun resetAllEffects() {
        setEqualizerEnabled(false)
        resetEqualizer()
        setBassBoostEnabled(false)
        setBassBoostStrength(0)
        setVirtualizerEnabled(false)
        setVirtualizerStrength(0)
        setReverbEnabled(false)
        setReverbPreset(ReverbPreset.NONE)
        saveSettings()
        updateState()
    }

    // ============ Private Helper Methods ============

    private fun saveEqualizerEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(AudioEnhancementSettings.PREF_KEY_EQUALIZER_ENABLED, enabled) }
    }

    private fun saveEqualizerPreset(preset: Int) {
        preferences.edit { putInt(AudioEnhancementSettings.PREF_KEY_EQUALIZER_PRESET, preset) }
    }

    private fun saveEqualizerBandLevels() {
        val levels = equalizerController.getAllBandLevels()
        val levelsString = levels.joinToString(",")
        preferences.edit { putString(AudioEnhancementSettings.PREF_KEY_EQUALIZER_BANDS, levelsString) }
    }

    private fun loadEqualizerBandLevels(): List<Int> {
        val levelsString = preferences.getString(AudioEnhancementSettings.PREF_KEY_EQUALIZER_BANDS, "") ?: ""
        return if (levelsString.isNotEmpty()) {
            levelsString.split(",").mapNotNull { it.toIntOrNull() }
        } else {
            emptyList()
        }
    }

    private fun saveBassBoostEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(AudioEnhancementSettings.PREF_KEY_BASS_BOOST_ENABLED, enabled) }
    }

    private fun saveBassBoostStrength(strength: Short) {
        preferences.edit { putInt(AudioEnhancementSettings.PREF_KEY_BASS_BOOST_STRENGTH, strength.toInt()) }
    }

    private fun saveVirtualizerEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(AudioEnhancementSettings.PREF_KEY_VIRTUALIZER_ENABLED, enabled) }
    }

    private fun saveVirtualizerStrength(strength: Short) {
        preferences.edit { putInt(AudioEnhancementSettings.PREF_KEY_VIRTUALIZER_STRENGTH, strength.toInt()) }
    }

    private fun saveReverbEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(AudioEnhancementSettings.PREF_KEY_REVERB_ENABLED, enabled) }
    }

    private fun saveReverbPreset(preset: Short) {
        preferences.edit { putInt(AudioEnhancementSettings.PREF_KEY_REVERB_PRESET, preset.toInt()) }
    }

    private fun updateState() {
        stateManager.updateState {
            AudioEffectsState(
                equalizerEnabled = equalizerController.isEnabled(),
                equalizerPresets = equalizerController.getPresets(),
                currentPreset = equalizerController.getCurrentPreset().toInt(),
                frequencyBands = equalizerController.getFrequencyBands(),
                bassBoostEnabled = bassBoostController.isEnabled(),
                bassBoostStrength = bassBoostController.getStrength(),
                virtualizerEnabled = virtualizerController.isEnabled(),
                virtualizerStrength = virtualizerController.getStrength(),
                reverbEnabled = presetReverbController.isEnabled(),
                reverbPreset = presetReverbController.getPresetEnum(),
                isInitialized = isInitialized,
                error = null
            )
        }
    }

    /**
     * Release all audio effect resources.
     * Should be called when effects are no longer needed.
     */
    fun release() {
        try {
            equalizerController.release()
            bassBoostController.release()
            virtualizerController.release()
            presetReverbController.release()
            currentAudioSessionId = null
            isInitialized = false
            Timber.d("AudioEffectsManager released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing AudioEffectsManager")
        }
    }
}
