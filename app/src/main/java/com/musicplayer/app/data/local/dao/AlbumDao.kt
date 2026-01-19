package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.musicplayer.app.data.local.entity.Album
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Album entity operations.
 */
@Dao
interface AlbumDao {
    
    /**
     * Get all albums as a Flow for reactive updates.
     */
    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<Album>>
    
    /**
     * Get an album by its ID.
     */
    @Query("SELECT * FROM albums WHERE id = :albumId")
    suspend fun getAlbumById(albumId: Long): Album?
    
    /**
     * Get albums by artist ID.
     */
    @Query("SELECT * FROM albums WHERE artist_id = :artistId ORDER BY year DESC, name ASC")
    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>>
    
    /**
     * Search albums by name.
     */
    @Query("SELECT * FROM albums WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchAlbums(query: String): Flow<List<Album>>
    
    /**
     * Get recently added albums.
     */
    @Query("SELECT * FROM albums ORDER BY id DESC LIMIT :limit")
    fun getRecentAlbums(limit: Int = 20): Flow<List<Album>>
    
    /**
     * Insert an album.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album): Long
    
    /**
     * Insert multiple albums.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<Album>): List<Long>
    
    /**
     * Update an album.
     */
    @Update
    suspend fun updateAlbum(album: Album)
    
    /**
     * Delete an album.
     */
    @Delete
    suspend fun deleteAlbum(album: Album)
    
    /**
     * Update album song count.
     */
    @Query("UPDATE albums SET song_count = :count WHERE id = :albumId")
    suspend fun updateSongCount(albumId: Long, count: Int)
    
    /**
     * Get total album count.
     */
    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int
}
