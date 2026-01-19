package com.musicplayer.app.presentation.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages app settings and preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    /**
     * Update theme preference.
     */
    fun setTheme(theme: Theme) {
        _settings.value = _settings.value.copy(theme = theme)
        // TODO: Persist to DataStore
    }

    /**
     * Update audio quality preference.
     */
    fun setAudioQuality(quality: AudioQuality) {
        _settings.value = _settings.value.copy(audioQuality = quality)
        // TODO: Persist to DataStore
    }

    /**
     * Toggle offline mode.
     */
    fun setOfflineMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(offlineMode = enabled)
        // TODO: Persist to DataStore
    }

    /**
     * Toggle equalizer.
     */
    fun setEqualizerEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(equalizerEnabled = enabled)
        // TODO: Persist to DataStore
    }
}

/**
 * Settings state data class.
 */
data class SettingsState(
    val theme: Theme = Theme.SYSTEM,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val offlineMode: Boolean = false,
    val equalizerEnabled: Boolean = false
)

enum class Theme {
    LIGHT, DARK, SYSTEM
}

enum class AudioQuality {
    LOW, MEDIUM, HIGH, LOSSLESS
}
