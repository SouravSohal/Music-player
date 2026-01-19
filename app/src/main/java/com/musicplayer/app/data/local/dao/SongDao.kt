package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.musicplayer.app.data.local.entity.Song
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Song entity operations.
 * Provides methods for querying, inserting, updating, and deleting songs.
 */
@Dao
interface SongDao {
    
    /**
     * Get all songs as a Flow for reactive updates.
     */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>
    
    /**
     * Get a song by its ID.
     */
    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): Song?
    
    /**
     * Get songs by album ID.
     */
    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number ASC, title ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
    
    /**
     * Get songs by artist ID.
     */
    @Query("SELECT * FROM songs WHERE artist_id = :artistId ORDER BY title ASC")
    fun getSongsByArtist(artistId: Long): Flow<List<Song>>
    
    /**
     * Get all favorite songs.
     */
    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY date_added DESC")
    fun getFavoriteSongs(): Flow<List<Song>>
    
    /**
     * Search songs by title, artist name, or album name.
     */
    @Query("""
        SELECT songs.* FROM songs 
        INNER JOIN artists ON songs.artist_id = artists.id
        INNER JOIN albums ON songs.album_id = albums.id
        WHERE songs.title LIKE '%' || :query || '%'
        OR artists.name LIKE '%' || :query || '%'
        OR albums.name LIKE '%' || :query || '%'
        ORDER BY songs.title ASC
    """)
    fun searchSongs(query: String): Flow<List<Song>>
    
    /**
     * Get recently added songs.
     */
    @Query("SELECT * FROM songs ORDER BY date_added DESC LIMIT :limit")
    fun getRecentlyAddedSongs(limit: Int = 20): Flow<List<Song>>
    
    /**
     * Insert a single song.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long
    
    /**
     * Insert multiple songs.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>): List<Long>
    
    /**
     * Update a song.
     */
    @Update
    suspend fun updateSong(song: Song)
    
    /**
     * Delete a song.
     */
    @Delete
    suspend fun deleteSong(song: Song)
    
    /**
     * Delete all songs.
     */
    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
    
    /**
     * Toggle favorite status of a song.
     */
    @Query("UPDATE songs SET is_favorite = NOT is_favorite WHERE id = :songId")
    suspend fun toggleFavorite(songId: Long)
    
    /**
     * Get total song count.
     */
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}
