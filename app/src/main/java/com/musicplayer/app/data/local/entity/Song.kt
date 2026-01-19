package com.musicplayer.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a song in the local database.
 * 
 * @property id Unique identifier for the song
 * @property title Song title
 * @property albumId Reference to the album this song belongs to
 * @property artistId Reference to the artist of this song
 * @property duration Duration of the song in milliseconds
 * @property uri Content URI or file path to the audio file
 * @property trackNumber Track number in the album
 * @property year Release year
 * @property genre Music genre
 * @property dateAdded Timestamp when the song was added to the library
 * @property isFavorite Whether the song is marked as favorite
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["album_id"]),
        Index(value = ["artist_id"]),
        Index(value = ["is_favorite"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "album_id")
    val albumId: Long,
    
    @ColumnInfo(name = "artist_id")
    val artistId: Long,
    
    @ColumnInfo(name = "duration")
    val duration: Long,
    
    @ColumnInfo(name = "uri")
    val uri: String,
    
    @ColumnInfo(name = "track_number")
    val trackNumber: Int? = null,
    
    @ColumnInfo(name = "year")
    val year: Int? = null,
    
    @ColumnInfo(name = "genre")
    val genre: String? = null,
    
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)
