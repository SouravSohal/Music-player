package com.musicplayer.app.domain.repository

import com.musicplayer.app.domain.model.ArtistModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for artist operations.
 */
interface ArtistRepository {
    
    /**
     * Get all artists from the library.
     */
    fun getAllArtists(): Flow<Result<List<ArtistModel>>>
    
    /**
     * Get an artist by its ID.
     */
    suspend fun getArtistById(artistId: Long): Result<ArtistModel>
    
    /**
     * Search artists by query.
     */
    fun searchArtists(query: String): Flow<Result<List<ArtistModel>>>
    
    /**
     * Get top artists by song count.
     */
    fun getTopArtists(limit: Int): Flow<Result<List<ArtistModel>>>
    
    /**
     * Get total artist count.
     */
    suspend fun getArtistCount(): Result<Int>
}
