package com.musicplayer.app.data.repository

import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.mapper.toModel
import com.musicplayer.app.domain.model.AlbumModel
import com.musicplayer.app.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AlbumRepository.
 */
@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) : AlbumRepository {

    override fun getAllAlbums(): Flow<Result<List<AlbumModel>>> {
        return albumDao.getAllAlbums()
            .map { albums ->
                val models = albums.map { album ->
                    val artist = artistDao.getArtistById(album.artistId)
                    album.toModel(artistName = artist?.name ?: "Unknown Artist")
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting all albums")
                emit(Result.failure(e))
            }
    }

    override suspend fun getAlbumById(albumId: Long): Result<AlbumModel> {
        return try {
            val album = albumDao.getAlbumById(albumId)
                ?: return Result.failure(Exception("Album not found"))
            
            val artist = artistDao.getArtistById(album.artistId)
            Result.success(album.toModel(artistName = artist?.name ?: "Unknown Artist"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting album by id: $albumId")
            Result.failure(e)
        }
    }

    override fun getAlbumsByArtist(artistId: Long): Flow<Result<List<AlbumModel>>> {
        return albumDao.getAlbumsByArtist(artistId)
            .map { albums ->
                val artist = artistDao.getArtistById(artistId)
                val models = albums.map { album ->
                    album.toModel(artistName = artist?.name ?: "Unknown Artist")
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting albums by artist: $artistId")
                emit(Result.failure(e))
            }
    }

    override fun searchAlbums(query: String): Flow<Result<List<AlbumModel>>> {
        return albumDao.searchAlbums(query)
            .map { albums ->
                val models = albums.map { album ->
                    val artist = artistDao.getArtistById(album.artistId)
                    album.toModel(artistName = artist?.name ?: "Unknown Artist")
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error searching albums: $query")
                emit(Result.failure(e))
            }
    }

    override fun getRecentAlbums(limit: Int): Flow<Result<List<AlbumModel>>> {
        return albumDao.getRecentAlbums(limit)
            .map { albums ->
                val models = albums.map { album ->
                    val artist = artistDao.getArtistById(album.artistId)
                    album.toModel(artistName = artist?.name ?: "Unknown Artist")
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting recent albums")
                emit(Result.failure(e))
            }
    }

    override suspend fun getAlbumCount(): Result<Int> {
        return try {
            val count = albumDao.getAlbumCount()
            Result.success(count)
        } catch (e: Exception) {
            Timber.e(e, "Error getting album count")
            Result.failure(e)
        }
    }
}
