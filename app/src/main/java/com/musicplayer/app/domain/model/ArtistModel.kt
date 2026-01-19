package com.musicplayer.app.domain.model

/**
 * Domain model representing an artist.
 */
data class ArtistModel(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val imageUri: String?
)
