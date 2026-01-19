package com.musicplayer.app.data.repository

import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.PlaylistSongDao
import com.musicplayer.app.data.local.entity.Playlist
import com.musicplayer.app.data.local.entity.PlaylistSong
import com.musicplayer.app.data.mapper.toEntity
import com.musicplayer.app.data.mapper.toModel
import com.musicplayer.app.domain.model.PlaylistModel
import com.musicplayer.app.domain.model.SongModel
import com.musicplayer.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PlaylistRepository.
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistSongDao: PlaylistSongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<Result<List<PlaylistModel>>> {
        return playlistDao.getAllPlaylists()
            .map { playlists ->
                Result.success(playlists.map { it.toModel() })
            }
            .catch { e ->
                Timber.e(e, "Error getting all playlists")
                emit(Result.failure(e))
            }
    }

    override suspend fun getPlaylistById(playlistId: Long): Result<PlaylistModel> {
        return try {
            val playlist = playlistDao.getPlaylistById(playlistId)
                ?: return Result.failure(Exception("Playlist not found"))
            
            Result.success(playlist.toModel())
        } catch (e: Exception) {
            Timber.e(e, "Error getting playlist by id: $playlistId")
            Result.failure(e)
        }
    }

    override fun getPlaylistByIdFlow(playlistId: Long): Flow<Result<PlaylistModel>> {
        return playlistDao.getPlaylistByIdFlow(playlistId)
            .map { playlist ->
                if (playlist != null) {
                    Result.success(playlist.toModel())
                } else {
                    Result.failure(Exception("Playlist not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Error getting playlist by id flow: $playlistId")
                emit(Result.failure(e))
            }
    }

    override fun getSongsInPlaylist(playlistId: Long): Flow<Result<List<SongModel>>> {
        return playlistSongDao.getSongsInPlaylist(playlistId)
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
                Timber.e(e, "Error getting songs in playlist: $playlistId")
                emit(Result.failure(e))
            }
    }

    override suspend fun createPlaylist(name: String, description: String?): Result<Long> {
        return try {
            val playlist = Playlist(
                name = name,
                description = description
            )
            val id = playlistDao.insertPlaylist(playlist)
            Timber.d("Created playlist: $name with id: $id")
            Result.success(id)
        } catch (e: Exception) {
            Timber.e(e, "Error creating playlist: $name")
            Result.failure(e)
        }
    }

    override suspend fun updatePlaylist(playlist: PlaylistModel): Result<Unit> {
        return try {
            playlistDao.updatePlaylist(playlist.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating playlist: ${playlist.id}")
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(playlistId: Long): Result<Unit> {
        return try {
            val playlist = playlistDao.getPlaylistById(playlistId)
                ?: return Result.failure(Exception("Playlist not found"))
            
            playlistDao.deletePlaylist(playlist)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting playlist: $playlistId")
            Result.failure(e)
        }
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Result<Unit> {
        return try {
            val exists = playlistSongDao.isSongInPlaylist(playlistId, songId)
            if (exists) {
                return Result.failure(Exception("Song already in playlist"))
            }
            
            val position = playlistSongDao.getNextPosition(playlistId)
            val playlistSong = PlaylistSong(
                playlistId = playlistId,
                songId = songId,
                position = position
            )
            
            playlistSongDao.insertPlaylistSong(playlistSong)
            
            // Update playlist song count and modified time
            val count = playlistSongDao.getPlaylistSongCount(playlistId)
            playlistDao.updateSongCount(playlistId, count)
            playlistDao.updateModifiedTime(playlistId)
            
            Timber.d("Added song $songId to playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding song to playlist")
            Result.failure(e)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Result<Unit> {
        return try {
            playlistSongDao.removeSongFromPlaylist(playlistId, songId)
            playlistSongDao.reorderPlaylist(playlistId)
            
            // Update playlist song count and modified time
            val count = playlistSongDao.getPlaylistSongCount(playlistId)
            playlistDao.updateSongCount(playlistId, count)
            playlistDao.updateModifiedTime(playlistId)
            
            Timber.d("Removed song $songId from playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing song from playlist")
            Result.failure(e)
        }
    }

    override suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Result<Boolean> {
        return try {
            val exists = playlistSongDao.isSongInPlaylist(playlistId, songId)
            Result.success(exists)
        } catch (e: Exception) {
            Timber.e(e, "Error checking if song in playlist")
            Result.failure(e)
        }
    }

    override suspend fun clearPlaylist(playlistId: Long): Result<Unit> {
        return try {
            playlistSongDao.clearPlaylist(playlistId)
            playlistDao.updateSongCount(playlistId, 0)
            playlistDao.updateModifiedTime(playlistId)
            
            Timber.d("Cleared playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error clearing playlist")
            Result.failure(e)
        }
    }

    override fun searchPlaylists(query: String): Flow<Result<List<PlaylistModel>>> {
        return playlistDao.searchPlaylists(query)
            .map { playlists ->
                Result.success(playlists.map { it.toModel() })
            }
            .catch { e ->
                Timber.e(e, "Error searching playlists: $query")
                emit(Result.failure(e))
            }
    }
}
