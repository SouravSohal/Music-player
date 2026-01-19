package com.musicplayer.app.effects

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing all audio enhancement settings.
 * Used for persisting and restoring effect configurations.
 */
data class AudioEnhancementSettings(
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: Int = -1,
    val equalizerBandLevels: List<Int> = emptyList(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Short = 0,
    val reverbEnabled: Boolean = false,
    val reverbPreset: Short = 0
) {
    companion object {
        const val PREF_KEY_EQUALIZER_ENABLED = "equalizer_enabled"
        const val PREF_KEY_EQUALIZER_PRESET = "equalizer_preset"
        const val PREF_KEY_EQUALIZER_BANDS = "equalizer_bands"
        const val PREF_KEY_BASS_BOOST_ENABLED = "bass_boost_enabled"
        const val PREF_KEY_BASS_BOOST_STRENGTH = "bass_boost_strength"
        const val PREF_KEY_VIRTUALIZER_ENABLED = "virtualizer_enabled"
        const val PREF_KEY_VIRTUALIZER_STRENGTH = "virtualizer_strength"
        const val PREF_KEY_REVERB_ENABLED = "reverb_enabled"
        const val PREF_KEY_REVERB_PRESET = "reverb_preset"
    }
}

/**
 * Represents equalizer preset information.
 */
data class EqualizerPreset(
    val id: Int,
    val name: String
)

/**
 * Represents frequency band information.
 */
data class FrequencyBand(
    val index: Short,
    val centerFrequency: Int,
    val level: Short
)

/**
 * Represents reverb preset information.
 */
enum class ReverbPreset(val value: Short, val displayName: String) {
    NONE(0, "None"),
    SMALL_ROOM(1, "Small Room"),
    MEDIUM_ROOM(2, "Medium Room"),
    LARGE_ROOM(3, "Large Room"),
    MEDIUM_HALL(4, "Medium Hall"),
    LARGE_HALL(5, "Large Hall"),
    PLATE(6, "Plate");

    companion object {
        fun fromValue(value: Short): ReverbPreset {
            return entries.find { it.value == value } ?: NONE
        }
    }
}

/**
 * State holder for audio effects UI.
 */
data class AudioEffectsState(
    val equalizerEnabled: Boolean = false,
    val equalizerPresets: List<EqualizerPreset> = emptyList(),
    val currentPreset: Int = -1,
    val frequencyBands: List<FrequencyBand> = emptyList(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Short = 0,
    val reverbEnabled: Boolean = false,
    val reverbPreset: ReverbPreset = ReverbPreset.NONE,
    val isInitialized: Boolean = false,
    val error: String? = null
)

/**
 * Manager for audio effects state flow.
 */
class AudioEffectsStateManager {
    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    fun updateState(update: (AudioEffectsState) -> AudioEffectsState) {
        _state.value = update(_state.value)
    }

    fun getState(): AudioEffectsState = _state.value
}
