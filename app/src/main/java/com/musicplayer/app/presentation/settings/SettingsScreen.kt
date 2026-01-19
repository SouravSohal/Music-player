package com.musicplayer.app.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings screen for app configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            ListItem(
                headlineContent = { Text("Offline Mode") },
                supportingContent = { Text("Download songs for offline playback") },
                trailingContent = {
                    Switch(
                        checked = settings.offlineMode,
                        onCheckedChange = { viewModel.setOfflineMode(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Equalizer") },
                supportingContent = { Text("Customize sound settings") },
                trailingContent = {
                    Switch(
                        checked = settings.equalizerEnabled,
                        onCheckedChange = { viewModel.setEqualizerEnabled(it) }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text("Current: ${settings.theme}") }
            )

            ListItem(
                headlineContent = { Text("Audio Quality") },
                supportingContent = { Text("Current: ${settings.audioQuality}") }
            )
        }
    }
}
