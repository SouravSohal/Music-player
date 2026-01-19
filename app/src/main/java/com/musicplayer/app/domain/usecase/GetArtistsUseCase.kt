package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.ArtistModel
import com.musicplayer.app.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all artists from the library.
 */
class GetArtistsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    operator fun invoke(): Flow<Result<List<ArtistModel>>> {
        return artistRepository.getAllArtists()
    }
}
