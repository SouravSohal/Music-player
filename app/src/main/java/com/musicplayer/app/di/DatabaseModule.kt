package com.musicplayer.app.di

import android.content.Context
import androidx.room.Room
import com.musicplayer.app.data.local.AppDatabase
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.PlaylistSongDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.repository.AlbumRepositoryImpl
import com.musicplayer.app.data.repository.ArtistRepositoryImpl
import com.musicplayer.app.data.repository.PlaylistRepositoryImpl
import com.musicplayer.app.data.repository.SongRepositoryImpl
import com.musicplayer.app.domain.repository.AlbumRepository
import com.musicplayer.app.domain.repository.ArtistRepository
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SongRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database and repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: AppDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun provideAlbumDao(database: AppDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    @Singleton
    fun provideArtistDao(database: AppDatabase): ArtistDao {
        return database.artistDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun providePlaylistSongDao(database: AppDatabase): PlaylistSongDao {
        return database.playlistSongDao()
    }

    @Provides
    @Singleton
    fun provideSongRepository(
        songDao: SongDao,
        albumDao: AlbumDao,
        artistDao: ArtistDao
    ): SongRepository {
        return SongRepositoryImpl(songDao, albumDao, artistDao)
    }

    @Provides
    @Singleton
    fun provideAlbumRepository(
        albumDao: AlbumDao,
        artistDao: ArtistDao
    ): AlbumRepository {
        return AlbumRepositoryImpl(albumDao, artistDao)
    }

    @Provides
    @Singleton
    fun provideArtistRepository(
        artistDao: ArtistDao
    ): ArtistRepository {
        return ArtistRepositoryImpl(artistDao)
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        playlistDao: PlaylistDao,
        playlistSongDao: PlaylistSongDao,
        albumDao: AlbumDao,
        artistDao: ArtistDao
    ): PlaylistRepository {
        return PlaylistRepositoryImpl(playlistDao, playlistSongDao, albumDao, artistDao)
    }
}
