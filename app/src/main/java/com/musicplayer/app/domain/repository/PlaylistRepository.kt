package com.musicplayer.app.domain.repository

import com.musicplayer.app.domain.model.PlaylistModel
import com.musicplayer.app.domain.model.SongModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for playlist operations.
 */
interface PlaylistRepository {
    
    /**
     * Get all playlists.
     */
    fun getAllPlaylists(): Flow<Result<List<PlaylistModel>>>
    
    /**
     * Get a playlist by its ID.
     */
    suspend fun getPlaylistById(playlistId: Long): Result<PlaylistModel>
    
    /**
     * Get playlist as Flow.
     */
    fun getPlaylistByIdFlow(playlistId: Long): Flow<Result<PlaylistModel>>
    
    /**
     * Get songs in a playlist.
     */
    fun getSongsInPlaylist(playlistId: Long): Flow<Result<List<SongModel>>>
    
    /**
     * Create a new playlist.
     */
    suspend fun createPlaylist(name: String, description: String?): Result<Long>
    
    /**
     * Update a playlist.
     */
    suspend fun updatePlaylist(playlist: PlaylistModel): Result<Unit>
    
    /**
     * Delete a playlist.
     */
    suspend fun deletePlaylist(playlistId: Long): Result<Unit>
    
    /**
     * Add a song to a playlist.
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Result<Unit>
    
    /**
     * Remove a song from a playlist.
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Result<Unit>
    
    /**
     * Check if a song is in a playlist.
     */
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Result<Boolean>
    
    /**
     * Clear all songs from a playlist.
     */
    suspend fun clearPlaylist(playlistId: Long): Result<Unit>
    
    /**
     * Search playlists by query.
     */
    fun searchPlaylists(query: String): Flow<Result<List<PlaylistModel>>>
}
