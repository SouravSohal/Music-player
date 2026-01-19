package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.musicplayer.app.data.local.entity.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Playlist entity operations.
 */
@Dao
interface PlaylistDao {
    
    /**
     * Get all playlists as a Flow for reactive updates.
     */
    @Query("SELECT * FROM playlists ORDER BY modified_at DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    /**
     * Get a playlist by its ID.
     */
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?
    
    /**
     * Get a playlist by its ID as Flow.
     */
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistByIdFlow(playlistId: Long): Flow<Playlist?>
    
    /**
     * Search playlists by name.
     */
    @Query("SELECT * FROM playlists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPlaylists(query: String): Flow<List<Playlist>>
    
    /**
     * Insert a playlist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    /**
     * Update a playlist.
     */
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    /**
     * Delete a playlist.
     */
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    /**
     * Update playlist modification time.
     */
    @Query("UPDATE playlists SET modified_at = :timestamp WHERE id = :playlistId")
    suspend fun updateModifiedTime(playlistId: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Update playlist song count.
     */
    @Query("UPDATE playlists SET song_count = :count WHERE id = :playlistId")
    suspend fun updateSongCount(playlistId: Long, count: Int)
    
    /**
     * Get total playlist count.
     */
    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int
}
