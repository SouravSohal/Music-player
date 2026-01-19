package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.repository.SongRepository
import javax.inject.Inject

/**
 * Use case for toggling favorite status of a song.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: Long): Result<Unit> {
        return songRepository.toggleFavorite(songId)
    }
}
