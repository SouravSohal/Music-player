package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.SongModel
import com.musicplayer.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting songs in a playlist.
 */
class GetPlaylistSongsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(playlistId: Long): Flow<Result<List<SongModel>>> {
        return playlistRepository.getSongsInPlaylist(playlistId)
    }
}
