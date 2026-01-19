package com.musicplayer.app.domain.repository

import com.musicplayer.app.domain.model.SongModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for song operations.
 * Defines the contract for song data access that will be implemented in the data layer.
 */
interface SongRepository {
    
    /**
     * Get all songs from the library.
     */
    fun getAllSongs(): Flow<Result<List<SongModel>>>
    
    /**
     * Get a song by its ID.
     */
    suspend fun getSongById(songId: Long): Result<SongModel>
    
    /**
     * Get songs by album ID.
     */
    fun getSongsByAlbum(albumId: Long): Flow<Result<List<SongModel>>>
    
    /**
     * Get songs by artist ID.
     */
    fun getSongsByArtist(artistId: Long): Flow<Result<List<SongModel>>>
    
    /**
     * Get favorite songs.
     */
    fun getFavoriteSongs(): Flow<Result<List<SongModel>>>
    
    /**
     * Search songs by query.
     */
    fun searchSongs(query: String): Flow<Result<List<SongModel>>>
    
    /**
     * Get recently added songs.
     */
    fun getRecentlyAddedSongs(limit: Int): Flow<Result<List<SongModel>>>
    
    /**
     * Toggle favorite status of a song.
     */
    suspend fun toggleFavorite(songId: Long): Result<Unit>
    
    /**
     * Scan device for music files and add them to the library.
     */
    suspend fun scanMusicLibrary(): Result<Int>
    
    /**
     * Get total song count.
     */
    suspend fun getSongCount(): Result<Int>
}
