package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Use case for deleting a playlist.
 */
class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long): Result<Unit> {
        return playlistRepository.deletePlaylist(playlistId)
    }
}
