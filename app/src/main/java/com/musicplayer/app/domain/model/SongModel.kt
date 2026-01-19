package com.musicplayer.app.domain.model

/**
 * Domain model representing a song.
 * This is separate from the database entity to maintain clean architecture.
 */
data class SongModel(
    val id: Long,
    val title: String,
    val albumId: Long,
    val albumName: String,
    val artistId: Long,
    val artistName: String,
    val duration: Long,
    val uri: String,
    val trackNumber: Int?,
    val year: Int?,
    val genre: String?,
    val dateAdded: Long,
    val isFavorite: Boolean,
    val artworkUri: String?
) {
    /**
     * Format duration in mm:ss format.
     */
    fun getFormattedDuration(): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
