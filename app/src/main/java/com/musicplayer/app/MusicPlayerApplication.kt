package com.musicplayer.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main application class for the Music Player app.
 * Initializes Hilt dependency injection and Timber logging.
 */
@HiltAndroidApp
class MusicPlayerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, plant a tree that logs only errors to Crashlytics or similar
            Timber.plant(ReleaseTree())
        }
        
        Timber.d("MusicPlayerApplication initialized")
    }

    /**
     * Custom Timber tree for release builds that filters out debug and verbose logs.
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                // Log to crash reporting service (e.g., Firebase Crashlytics)
                // CrashlyticsHelper.log(priority, tag, message, t)
            }
        }
    }
}
