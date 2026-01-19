package com.musicplayer.app.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * BroadcastReceiver for handling Bluetooth connectivity events.
 * Pauses playback when Bluetooth audio devices disconnect.
 */
class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Timber.d("Bluetooth device disconnected: ${device?.name}")
                
                // TODO: Check if the disconnected device is an audio device
                // and pause playback if necessary
                // This would require checking the device class and current audio routing
            }
            
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Timber.d("Bluetooth device connected: ${device?.name}")
            }
        }
    }
}
