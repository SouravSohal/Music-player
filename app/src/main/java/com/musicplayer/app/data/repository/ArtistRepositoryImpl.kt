package com.musicplayer.app.data.repository

import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.mapper.toModel
import com.musicplayer.app.domain.model.ArtistModel
import com.musicplayer.app.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ArtistRepository.
 */
@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val artistDao: ArtistDao
) : ArtistRepository {

    override fun getAllArtists(): Flow<Result<List<ArtistModel>>> {
        return artistDao.getAllArtists()
            .map { artists ->
                Result.success(artists.map { it.toModel() })
            }
            .catch { e ->
                Timber.e(e, "Error getting all artists")
                emit(Result.failure(e))
            }
    }

    override suspend fun getArtistById(artistId: Long): Result<ArtistModel> {
        return try {
            val artist = artistDao.getArtistById(artistId)
                ?: return Result.failure(Exception("Artist not found"))
            
            Result.success(artist.toModel())
        } catch (e: Exception) {
            Timber.e(e, "Error getting artist by id: $artistId")
            Result.failure(e)
        }
    }

    override fun searchArtists(query: String): Flow<Result<List<ArtistModel>>> {
        return artistDao.searchArtists(query)
            .map { artists ->
                Result.success(artists.map { it.toModel() })
            }
            .catch { e ->
                Timber.e(e, "Error searching artists: $query")
                emit(Result.failure(e))
            }
    }

    override fun getTopArtists(limit: Int): Flow<Result<List<ArtistModel>>> {
        return artistDao.getTopArtists(limit)
            .map { artists ->
                Result.success(artists.map { it.toModel() })
            }
            .catch { e ->
                Timber.e(e, "Error getting top artists")
                emit(Result.failure(e))
            }
    }

    override suspend fun getArtistCount(): Result<Int> {
        return try {
            val count = artistDao.getArtistCount()
            Result.success(count)
        } catch (e: Exception) {
            Timber.e(e, "Error getting artist count")
            Result.failure(e)
        }
    }
}
