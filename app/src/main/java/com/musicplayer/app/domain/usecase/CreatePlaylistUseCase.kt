package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Use case for creating a new playlist.
 */
class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(name: String, description: String? = null): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        }
        return playlistRepository.createPlaylist(name, description)
    }
}
