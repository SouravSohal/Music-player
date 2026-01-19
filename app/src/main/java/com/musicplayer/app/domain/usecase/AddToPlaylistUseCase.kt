package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Use case for adding a song to a playlist.
 */
class AddToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: Long): Result<Unit> {
        return playlistRepository.addSongToPlaylist(playlistId, songId)
    }
}
