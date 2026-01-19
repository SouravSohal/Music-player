package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.PlaylistModel
import com.musicplayer.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all playlists.
 */
class GetPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<Result<List<PlaylistModel>>> {
        return playlistRepository.getAllPlaylists()
    }
}
