package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.AlbumModel
import com.musicplayer.app.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all albums from the library.
 */
class GetAlbumsUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    operator fun invoke(): Flow<Result<List<AlbumModel>>> {
        return albumRepository.getAllAlbums()
    }
}
