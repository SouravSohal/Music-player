package com.musicplayer.app.domain.model

/**
 * Domain model representing a playlist.
 */
data class PlaylistModel(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val modifiedAt: Long,
    val songCount: Int,
    val coverUri: String?
)
