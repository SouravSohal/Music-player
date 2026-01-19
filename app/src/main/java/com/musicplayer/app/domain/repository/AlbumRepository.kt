package com.musicplayer.app.domain.repository

import com.musicplayer.app.domain.model.AlbumModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for album operations.
 */
interface AlbumRepository {
    
    /**
     * Get all albums from the library.
     */
    fun getAllAlbums(): Flow<Result<List<AlbumModel>>>
    
    /**
     * Get an album by its ID.
     */
    suspend fun getAlbumById(albumId: Long): Result<AlbumModel>
    
    /**
     * Get albums by artist ID.
     */
    fun getAlbumsByArtist(artistId: Long): Flow<Result<List<AlbumModel>>>
    
    /**
     * Search albums by query.
     */
    fun searchAlbums(query: String): Flow<Result<List<AlbumModel>>>
    
    /**
     * Get recently added albums.
     */
    fun getRecentAlbums(limit: Int): Flow<Result<List<AlbumModel>>>
    
    /**
     * Get total album count.
     */
    suspend fun getAlbumCount(): Result<Int>
}
