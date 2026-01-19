package com.musicplayer.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an artist in the local database.
 * 
 * @property id Unique identifier for the artist
 * @property name Artist name
 * @property albumCount Number of albums by this artist
 * @property songCount Number of songs by this artist
 * @property imageUri URI to the artist's image
 */
@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "album_count")
    val albumCount: Int = 0,
    
    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,
    
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null
)
