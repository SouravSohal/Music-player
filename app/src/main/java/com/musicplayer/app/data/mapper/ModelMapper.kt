package com.musicplayer.app.data.mapper

import com.musicplayer.app.data.local.entity.Album
import com.musicplayer.app.data.local.entity.Artist
import com.musicplayer.app.data.local.entity.Playlist
import com.musicplayer.app.data.local.entity.Song
import com.musicplayer.app.domain.model.AlbumModel
import com.musicplayer.app.domain.model.ArtistModel
import com.musicplayer.app.domain.model.PlaylistModel
import com.musicplayer.app.domain.model.SongModel

/**
 * Extension functions to map between data layer entities and domain layer models.
 */

fun Song.toModel(albumName: String, artistName: String, artworkUri: String?): SongModel {
    return SongModel(
        id = id,
        title = title,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        duration = duration,
        uri = uri,
        trackNumber = trackNumber,
        year = year,
        genre = genre,
        dateAdded = dateAdded,
        isFavorite = isFavorite,
        artworkUri = artworkUri
    )
}

fun Album.toModel(artistName: String): AlbumModel {
    return AlbumModel(
        id = id,
        name = name,
        artistId = artistId,
        artistName = artistName,
        year = year,
        artworkUri = artworkUri,
        songCount = songCount
    )
}

fun Artist.toModel(): ArtistModel {
    return ArtistModel(
        id = id,
        name = name,
        albumCount = albumCount,
        songCount = songCount,
        imageUri = imageUri
    )
}

fun Playlist.toModel(): PlaylistModel {
    return PlaylistModel(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        songCount = songCount,
        coverUri = coverUri
    )
}

fun PlaylistModel.toEntity(): Playlist {
    return Playlist(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        songCount = songCount,
        coverUri = coverUri
    )
}
