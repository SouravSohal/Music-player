package com.musicplayer.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a playlist in the local database.
 * 
 * @property id Unique identifier for the playlist
 * @property name Playlist name
 * @property description Optional description of the playlist
 * @property createdAt Timestamp when the playlist was created
 * @property modifiedAt Timestamp when the playlist was last modified
 * @property songCount Number of songs in the playlist
 * @property coverUri URI to the playlist cover image (can be generated from songs)
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,
    
    @ColumnInfo(name = "cover_uri")
    val coverUri: String? = null
)
