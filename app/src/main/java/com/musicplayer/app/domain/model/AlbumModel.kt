package com.musicplayer.app.domain.model

/**
 * Domain model representing an album.
 */
data class AlbumModel(
    val id: Long,
    val name: String,
    val artistId: Long,
    val artistName: String,
    val year: Int?,
    val artworkUri: String?,
    val songCount: Int
)
