package com.musicplayer.app.di

import com.musicplayer.app.converter.AudioEncoder
import com.musicplayer.app.converter.ConversionManager
import com.musicplayer.app.converter.VideoToAudioConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing converter-related dependencies.
 * 
 * This module provides singleton instances of:
 * - AudioEncoder for encoding audio to various formats
 * - VideoToAudioConverter for converting video to audio
 * - ConversionManager for managing conversion queue
 */
@Module
@InstallIn(SingletonComponent::class)
object ConverterModule {

    @Provides
    @Singleton
    fun provideAudioEncoder(): AudioEncoder {
        return AudioEncoder()
    }

    @Provides
    @Singleton
    fun provideVideoToAudioConverter(
        audioEncoder: AudioEncoder,
        context: android.content.Context
    ): VideoToAudioConverter {
        return VideoToAudioConverter(context, audioEncoder)
    }

    @Provides
    @Singleton
    fun provideConversionManager(
        converter: VideoToAudioConverter
    ): ConversionManager {
        return ConversionManager(converter)
    }
}
