package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.musicplayer.app.data.local.entity.Artist
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Artist entity operations.
 */
@Dao
interface ArtistDao {
    
    /**
     * Get all artists as a Flow for reactive updates.
     */
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<Artist>>
    
    /**
     * Get an artist by its ID.
     */
    @Query("SELECT * FROM artists WHERE id = :artistId")
    suspend fun getArtistById(artistId: Long): Artist?
    
    /**
     * Search artists by name.
     */
    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchArtists(query: String): Flow<List<Artist>>
    
    /**
     * Get top artists by song count.
     */
    @Query("SELECT * FROM artists ORDER BY song_count DESC LIMIT :limit")
    fun getTopArtists(limit: Int = 20): Flow<List<Artist>>
    
    /**
     * Insert an artist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: Artist): Long
    
    /**
     * Insert multiple artists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<Artist>): List<Long>
    
    /**
     * Update an artist.
     */
    @Update
    suspend fun updateArtist(artist: Artist)
    
    /**
     * Delete an artist.
     */
    @Delete
    suspend fun deleteArtist(artist: Artist)
    
    /**
     * Update artist counts.
     */
    @Query("UPDATE artists SET album_count = :albumCount, song_count = :songCount WHERE id = :artistId")
    suspend fun updateCounts(artistId: Long, albumCount: Int, songCount: Int)
    
    /**
     * Get total artist count.
     */
    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getArtistCount(): Int
}
