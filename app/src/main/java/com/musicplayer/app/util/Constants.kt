package com.musicplayer.app.util

object Constants {
    // Database
    const val DATABASE_NAME = "music_player_database"
    const val DATABASE_VERSION = 1
    
    // Shared Preferences
    const val PREFS_NAME = "music_player_prefs"
    const val PREF_THEME_MODE = "theme_mode"
    const val PREF_REPEAT_MODE = "repeat_mode"
    const val PREF_SHUFFLE_MODE = "shuffle_mode"
    const val PREF_LAST_PLAYED_SONG_ID = "last_played_song_id"
    const val PREF_LAST_PLAYED_POSITION = "last_played_position"
    const val PREF_AUDIO_QUALITY = "audio_quality"
    const val PREF_EQUALIZER_PRESET = "equalizer_preset"
    
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Music Playback"
    const val NOTIFICATION_ID = 1001
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
    const val DOWNLOAD_NOTIFICATION_CHANNEL_NAME = "Downloads"
    const val DOWNLOAD_NOTIFICATION_ID = 2001
    
    // Intent Actions
    const val ACTION_PLAY = "com.musicplayer.app.ACTION_PLAY"
    const val ACTION_PAUSE = "com.musicplayer.app.ACTION_PAUSE"
    const val ACTION_NEXT = "com.musicplayer.app.ACTION_NEXT"
    const val ACTION_PREVIOUS = "com.musicplayer.app.ACTION_PREVIOUS"
    const val ACTION_STOP = "com.musicplayer.app.ACTION_STOP"
    
    // Extras
    const val EXTRA_SONG_ID = "extra_song_id"
    const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
    const val EXTRA_ALBUM_ID = "extra_album_id"
    const val EXTRA_ARTIST_ID = "extra_artist_id"
    
    // File Paths
    const val MUSIC_DIRECTORY = "Music"
    const val DOWNLOADS_DIRECTORY = "Downloads"
    const val CACHE_DIRECTORY = "cache"
    const val ALBUM_ART_DIRECTORY = "album_art"
    
    // Supported Audio Formats
    val SUPPORTED_AUDIO_FORMATS = arrayOf(
        ".mp3", ".m4a", ".aac", ".wav", ".flac", ".ogg", ".opus", ".wma", ".3gp"
    )
    
    // Cache Limits
    const val MAX_CACHE_SIZE_MB = 500L
    const val MAX_ALBUM_ART_CACHE_SIZE_MB = 100L
    
    // Playback
    const val SEEK_FORWARD_MS = 10000L
    const val SEEK_BACKWARD_MS = 10000L
    const val UPDATE_INTERVAL_MS = 1000L
    
    // Download
    const val MAX_CONCURRENT_DOWNLOADS = 3
    const val DOWNLOAD_TIMEOUT_SECONDS = 30L
    const val DOWNLOAD_BUFFER_SIZE = 8192
    
    // Media Scanning
    const val SCAN_DELAY_MS = 1000L
    const val BATCH_SIZE = 100
}
