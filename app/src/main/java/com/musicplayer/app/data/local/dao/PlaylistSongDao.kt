package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.musicplayer.app.data.local.entity.PlaylistSong
import com.musicplayer.app.data.local.entity.Song
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PlaylistSong entity operations.
 * Manages the many-to-many relationship between playlists and songs.
 */
@Dao
interface PlaylistSongDao {
    
    /**
     * Get all songs in a playlist ordered by position.
     */
    @Query("""
        SELECT songs.* FROM songs 
        INNER JOIN playlist_songs ON songs.id = playlist_songs.song_id 
        WHERE playlist_songs.playlist_id = :playlistId 
        ORDER BY playlist_songs.position ASC
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>
    
    /**
     * Get playlist-song relationships for a playlist.
     */
    @Query("SELECT * FROM playlist_songs WHERE playlist_id = :playlistId ORDER BY position ASC")
    fun getPlaylistSongs(playlistId: Long): Flow<List<PlaylistSong>>
    
    /**
     * Check if a song is in a playlist.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean
    
    /**
     * Get the next available position in a playlist.
     */
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun getNextPosition(playlistId: Long): Int
    
    /**
     * Insert a song into a playlist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSong)
    
    /**
     * Insert multiple songs into a playlist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(playlistSongs: List<PlaylistSong>)
    
    /**
     * Remove a song from a playlist.
     */
    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    
    /**
     * Remove all songs from a playlist.
     */
    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)
    
    /**
     * Update position of a song in playlist.
     */
    @Query("UPDATE playlist_songs SET position = :newPosition WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, newPosition: Int)
    
    /**
     * Get count of songs in a playlist.
     */
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: Long): Int
    
    /**
     * Reorder songs in playlist after deletion.
     * Updates positions to be sequential after removing a song.
     */
    @Transaction
    suspend fun reorderPlaylist(playlistId: Long) {
        val songs = getPlaylistSongsSync(playlistId)
        songs.forEachIndexed { index, playlistSong ->
            if (playlistSong.position != index) {
                updateSongPosition(playlistId, playlistSong.songId, index)
            }
        }
    }
    
    /**
     * Helper method to get playlist songs synchronously for reordering.
     */
    @Query("SELECT * FROM playlist_songs WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongsSync(playlistId: Long): List<PlaylistSong>
}
