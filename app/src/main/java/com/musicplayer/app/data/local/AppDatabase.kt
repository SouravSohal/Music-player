package com.musicplayer.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.PlaylistSongDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.Album
import com.musicplayer.app.data.local.entity.Artist
import com.musicplayer.app.data.local.entity.Playlist
import com.musicplayer.app.data.local.entity.PlaylistSong
import com.musicplayer.app.data.local.entity.Song

/**
 * Room database for the Music Player application.
 * Contains tables for songs, albums, artists, playlists, and playlist-song relationships.
 * 
 * Database version: 1
 */
@Database(
    entities = [
        Song::class,
        Album::class,
        Artist::class,
        Playlist::class,
        PlaylistSong::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to Song table operations.
     */
    abstract fun songDao(): SongDao
    
    /**
     * Provides access to Album table operations.
     */
    abstract fun albumDao(): AlbumDao
    
    /**
     * Provides access to Artist table operations.
     */
    abstract fun artistDao(): ArtistDao
    
    /**
     * Provides access to Playlist table operations.
     */
    abstract fun playlistDao(): PlaylistDao
    
    /**
     * Provides access to PlaylistSong table operations.
     */
    abstract fun playlistSongDao(): PlaylistSongDao
    
    companion object {
        const val DATABASE_NAME = "music_player_database"
    }
}
