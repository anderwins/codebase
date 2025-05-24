/*
 * ==========================================================================
 *  FICHIER : search_queries.sql
 *  DESCRIPTION :
 *    Ce fichier contient toutes les requêtes SQL liées aux recherches dans
 *    la base de données.
 *
 *  STRUCTURE :
 *    ➤ Chaque requête est précédée d’un tag unique :
 *    ➤ Le chargement est effectué via SQLLoader.
 *
 * ==========================================================================
 */

-- [searchTracksByTitle]
SELECT track_id, title, artist_id, album_id, year, duration_sec, file_path, cover_path, lyrics_path, karaoke_path
FROM Track
WHERE title LIKE ?;

-- [searchTrackByArtist]
SELECT track_id, title, album_id, year, duration_sec, file_path, cover_path, lyrics_path, karaoke_path
FROM Track
WHERE artist_id = ?;

-- [searchTrackByAlbum]
SELECT track_id, title, artist_id, year, duration_sec, file_path, cover_path, lyrics_path, karaoke_path
FROM Track
WHERE album_id = ?;

-- [searchTracksByTag]
SELECT t.track_id, t.title, t.artist_id, t.album_id, t.year, t.duration_sec, t.file_path, t.cover_path, t.lyrics_path, t.karaoke_path
FROM Track t INNER JOIN Track_Tag tt ON t.track_id = tt.track_id
WHERE tt.tag_id = ?

-- [getArtistNameFromArtistID]
SELECT name FROM Artist WHERE artist_id = ?

-- [getAlbumNameFromAbumID]
SELECT title FROM Album WHERE album_id = ?

-- [getTagTrackFromTrackID]
SELECT name FROM Tag INNER JOIN Track_Tag ON Tag.tag_id = Track_Tag.tag_id WHERE Track_Tag.track_id = ?

-- [getAllPlaylistsWithTracks]
SELECT playlist_id, name As title FROM Playlist

-- [getTracksForPlaylist]
-- [getTracksForPlaylist]
SELECT t.track_id, t.title, a.name AS artist, al.title AS album,
    t.year, t.duration_sec, tg.name AS genre,
    t.file_path, t.cover_path, t.lyrics_path, t.karaoke_path
    FROM PlaylistTrack pt
    JOIN Track t ON pt.track_id = t.track_id
    JOIN Artist a ON t.artist_id = a.artist_id
    JOIN Album al ON t.album_id = al.album_id
    LEFT JOIN Track_Tag tt ON t.track_id = tt.track_id
    LEFT JOIN Tag tg ON tt.tag_id = tg.tag_id
    WHERE pt.playlist_id = ?

-- [getAllTracks]
SELECT track_id, title, artist_id, album_id, year, duration_sec, file_path, cover_path, lyrics_path, karaoke_path
FROM Track