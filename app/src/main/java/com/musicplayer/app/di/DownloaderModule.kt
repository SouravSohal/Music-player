package com.musicplayer.app.di

import com.musicplayer.app.downloader.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt dependency injection module for the downloader components.
 * 
 * This module provides:
 * - Configured OkHttpClient for downloads
 * - UrlValidator instance
 * - MediaDownloader instance
 * - DownloadManager instance
 * 
 * All components are provided as singletons to ensure efficient resource usage.
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloaderModule {

    /**
     * Provides a configured OkHttpClient specifically for downloads.
     * 
     * Configuration includes:
     * - Extended timeouts for large file downloads
     * - Connection pooling for efficiency
     * - Logging for debugging (only in debug builds)
     * - Support for redirects
     * 
     * @return Configured OkHttpClient instance
     */
    @Provides
    @Singleton
    @DownloaderClient
    fun provideDownloaderOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.musicplayer.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.MINUTES) // For large downloads
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provides URL validator instance.
     * 
     * @param okHttpClient HTTP client for validation
     * @return UrlValidator instance
     */
    @Provides
    @Singleton
    fun provideUrlValidator(
        @DownloaderClient okHttpClient: OkHttpClient
    ): UrlValidator {
        return UrlValidator(okHttpClient)
    }

    /**
     * Provides media downloader instance.
     * 
     * @param okHttpClient HTTP client for downloads
     * @param urlValidator URL validator
     * @return MediaDownloader instance
     */
    @Provides
    @Singleton
    fun provideMediaDownloader(
        @DownloaderClient okHttpClient: OkHttpClient,
        urlValidator: UrlValidator
    ): MediaDownloader {
        return MediaDownloader(okHttpClient, urlValidator)
    }

    /**
     * Provides download manager instance.
     * 
     * @param mediaDownloader Media downloader
     * @return DownloadManager instance
     */
    @Provides
    @Singleton
    fun provideDownloadManager(
        mediaDownloader: MediaDownloader
    ): DownloadManager {
        return DownloadManager(mediaDownloader)
    }
}

/**
 * Qualifier annotation for downloader-specific OkHttpClient.
 * 
 * This ensures the downloader gets its own configured HTTP client
 * separate from other HTTP clients in the app (e.g., for API calls).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloaderClient
