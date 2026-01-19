package com.musicplayer.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an album in the local database.
 * 
 * @property id Unique identifier for the album
 * @property name Album name
 * @property artistId Reference to the artist of this album
 * @property year Release year
 * @property artworkUri URI to the album artwork image
 * @property songCount Number of songs in the album
 */
@Entity(
    tableName = "albums",
    indices = [Index(value = ["artist_id"])],
    foreignKeys = [
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Album(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "artist_id")
    val artistId: Long,
    
    @ColumnInfo(name = "year")
    val year: Int? = null,
    
    @ColumnInfo(name = "artwork_uri")
    val artworkUri: String? = null,
    
    @ColumnInfo(name = "song_count")
    val songCount: Int = 0
)
