package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.SongModel
import com.musicplayer.app.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all songs from the library.
 */
class GetSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<Result<List<SongModel>>> {
        return songRepository.getAllSongs()
    }
}
