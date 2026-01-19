package com.musicplayer.app.data.repository

import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.mapper.toModel
import com.musicplayer.app.domain.model.SongModel
import com.musicplayer.app.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SongRepository.
 * Handles data operations for songs using Room database.
 */
@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) : SongRepository {

    override fun getAllSongs(): Flow<Result<List<SongModel>>> {
        return songDao.getAllSongs()
            .map { songs ->
                val models = songs.map { song ->
                    val album = albumDao.getAlbumById(song.albumId)
                    val artist = artistDao.getArtistById(song.artistId)
                    song.toModel(
                        albumName = album?.name ?: "Unknown Album",
                        artistName = artist?.name ?: "Unknown Artist",
                        artworkUri = album?.artworkUri
                    )
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting all songs")
                emit(Result.failure(e))
            }
    }

    override suspend fun getSongById(songId: Long): Result<SongModel> {
        return try {
            val song = songDao.getSongById(songId)
                ?: return Result.failure(Exception("Song not found"))
            
            val album = albumDao.getAlbumById(song.albumId)
            val artist = artistDao.getArtistById(song.artistId)
            
            Result.success(
                song.toModel(
                    albumName = album?.name ?: "Unknown Album",
                    artistName = artist?.name ?: "Unknown Artist",
                    artworkUri = album?.artworkUri
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting song by id: $songId")
            Result.failure(e)
        }
    }

    override fun getSongsByAlbum(albumId: Long): Flow<Result<List<SongModel>>> {
        return songDao.getSongsByAlbum(albumId)
            .map { songs ->
                val album = albumDao.getAlbumById(albumId)
                val artist = album?.let { artistDao.getArtistById(it.artistId) }
                
                val models = songs.map { song ->
                    song.toModel(
                        albumName = album?.name ?: "Unknown Album",
                        artistName = artist?.name ?: "Unknown Artist",
                        artworkUri = album?.artworkUri
                    )
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting songs by album: $albumId")
                emit(Result.failure(e))
            }
    }

    override fun getSongsByArtist(artistId: Long): Flow<Result<List<SongModel>>> {
        return songDao.getSongsByArtist(artistId)
            .map { songs ->
                val artist = artistDao.getArtistById(artistId)
                
                val models = songs.map { song ->
                    val album = albumDao.getAlbumById(song.albumId)
                    song.toModel(
                        albumName = album?.name ?: "Unknown Album",
                        artistName = artist?.name ?: "Unknown Artist",
                        artworkUri = album?.artworkUri
                    )
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting songs by artist: $artistId")
                emit(Result.failure(e))
            }
    }

    override fun getFavoriteSongs(): Flow<Result<List<SongModel>>> {
        return songDao.getFavoriteSongs()
            .map { songs ->
                val models = songs.map { song ->
                    val album = albumDao.getAlbumById(song.albumId)
                    val artist = artistDao.getArtistById(song.artistId)
                    song.toModel(
                        albumName = album?.name ?: "Unknown Album",
                        artistName = artist?.name ?: "Unknown Artist",
                        artworkUri = album?.artworkUri
                    )
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting favorite songs")
                emit(Result.failure(e))
            }
    }

    override fun searchSongs(query: String): Flow<Result<List<SongModel>>> {
        return songDao.searchSongs(query)
            .map { songs ->
                val models = songs.map { song ->
                    val album = albumDao.getAlbumById(song.albumId)
                    val artist = artistDao.getArtistById(song.artistId)
                    song.toModel(
                        albumName = album?.name ?: "Unknown Album",
                        artistName = artist?.name ?: "Unknown Artist",
                        artworkUri = album?.artworkUri
                    )
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error searching songs: $query")
                emit(Result.failure(e))
            }
    }

    override fun getRecentlyAddedSongs(limit: Int): Flow<Result<List<SongModel>>> {
        return songDao.getRecentlyAddedSongs(limit)
            .map { songs ->
                val models = songs.map { song ->
                    val album = albumDao.getAlbumById(song.albumId)
                    val artist = artistDao.getArtistById(song.artistId)
                    song.toModel(
                        albumName = album?.name ?: "Unknown Album",
                        artistName = artist?.name ?: "Unknown Artist",
                        artworkUri = album?.artworkUri
                    )
                }
                Result.success(models)
            }
            .catch { e ->
                Timber.e(e, "Error getting recently added songs")
                emit(Result.failure(e))
            }
    }

    override suspend fun toggleFavorite(songId: Long): Result<Unit> {
        return try {
            songDao.toggleFavorite(songId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error toggling favorite: $songId")
            Result.failure(e)
        }
    }

    override suspend fun scanMusicLibrary(): Result<Int> {
        return try {
            // TODO: Implement media scanning using MediaStore
            // This will scan the device for audio files and add them to the database
            Timber.d("Music library scan requested - implementation pending")
            Result.success(0)
        } catch (e: Exception) {
            Timber.e(e, "Error scanning music library")
            Result.failure(e)
        }
    }

    override suspend fun getSongCount(): Result<Int> {
        return try {
            val count = songDao.getSongCount()
            Result.success(count)
        } catch (e: Exception) {
            Timber.e(e, "Error getting song count")
            Result.failure(e)
        }
    }
}
