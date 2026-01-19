package com.musicplayer.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHandler {
    
    /**
     * Storage permissions based on Android version
     */
    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    /**
     * Notification permission (Android 13+)
     */
    val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
    
    /**
     * Bluetooth permissions based on Android version
     */
    val BLUETOOTH_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH
        )
    }
    
    /**
     * Audio recording permission
     */
    val AUDIO_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )
    
    /**
     * All permissions required by the app
     */
    fun getAllRequiredPermissions(): Array<String> {
        return (STORAGE_PERMISSIONS + NOTIFICATION_PERMISSION + BLUETOOTH_PERMISSIONS).distinct().toTypedArray()
    }
    
    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermission(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Check if bluetooth permissions are granted
     */
    fun hasBluetoothPermission(context: Context): Boolean {
        return BLUETOOTH_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if audio recording permission is granted
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasStoragePermission(context) && hasNotificationPermission(context)
    }
    
    /**
     * Get list of denied permissions from a given array
     */
    fun getDeniedPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get list of missing storage permissions
     */
    fun getMissingStoragePermissions(context: Context): List<String> {
        return getDeniedPermissions(context, STORAGE_PERMISSIONS)
    }
    
    /**
     * Check if permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Permission request codes
     */
    object RequestCodes {
        const val STORAGE_PERMISSION = 1001
        const val NOTIFICATION_PERMISSION = 1002
        const val BLUETOOTH_PERMISSION = 1003
        const val AUDIO_PERMISSION = 1004
        const val ALL_PERMISSIONS = 1005
    }
    
    /**
     * Check if we should show rationale for permissions
     */
    fun shouldShowRationale(
        activity: android.app.Activity,
        permissions: Array<String>
    ): Boolean {
        return permissions.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }
    
    /**
     * Get user-friendly permission name
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage (Read)"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage (Write)"
            Manifest.permission.READ_MEDIA_AUDIO -> "Audio Files"
            Manifest.permission.READ_MEDIA_IMAGES -> "Images"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth"
            Manifest.permission.BLUETOOTH -> "Bluetooth"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            else -> permission.substringAfterLast('.')
        }
    }
    
    /**
     * Get permission description for rationale dialog
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_AUDIO -> 
                "Required to access and play music files on your device"
            
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> 
                "Required to download and save music files"
            
            Manifest.permission.READ_MEDIA_IMAGES -> 
                "Required to display album artwork"
            
            Manifest.permission.POST_NOTIFICATIONS -> 
                "Required to show playback controls and notifications"
            
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH -> 
                "Required to connect to bluetooth audio devices"
            
            Manifest.permission.RECORD_AUDIO -> 
                "Required for audio effects and equalizer"
            
            else -> "Required for app functionality"
        }
    }
}
