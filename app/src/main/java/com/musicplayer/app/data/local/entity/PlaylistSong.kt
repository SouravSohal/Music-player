package com.musicplayer.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing the many-to-many relationship between playlists and songs.
 * This allows songs to appear in multiple playlists with different positions.
 * 
 * @property id Unique identifier for this playlist-song relationship
 * @property playlistId Reference to the playlist
 * @property songId Reference to the song
 * @property position Position of the song in the playlist (for ordering)
 * @property addedAt Timestamp when the song was added to the playlist
 */
@Entity(
    tableName = "playlist_songs",
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["song_id"]),
        Index(value = ["playlist_id", "position"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    
    @ColumnInfo(name = "song_id")
    val songId: Long,
    
    @ColumnInfo(name = "position")
    val position: Int,
    
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
