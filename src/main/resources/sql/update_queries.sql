/*
 * ==========================================================================
 *  FICHIER : update_queries.sql
 *  DESCRIPTION :
 *    Ce fichier contient les requêtes SQL utilisées pour la mise à jour
 *    des données dans la base.
 *
 *  STRUCTURE :
 *    ➤ Chaque requête est précédée d'un tag unique :
 *    ➤ Le chargement est effectué via SQLLoader.
 *
 * ==========================================================================
 */

-- [findTrackByFilePath]
 SELECT t.track_id, t.title, t.year, t.duration_sec, t.cover_path, t.file_path, t.lyrics_path, t.karaoke_path,
                   t.artist_id, t.album_id, tt.tag_id 
            FROM Track t
            LEFT JOIN Track_Tag tt ON t.track_id = tt.track_id
            WHERE t.file_path = ?

-- [updateTrackInfo]
UPDATE Track SET title = ?, year = ?, duration_sec = ?, cover_path = ? WHERE track_id = ?

-- [updateTrackArtist]
UPDATE Track SET artist_id = ? WHERE track_id = ?

-- [updateTrackAlbum]
UPDATE Track SET album_id = ? WHERE track_id = ?

-- [updateTagInfoDelete]
DELETE FROM Track_Tag WHERE track_id = ?

-- [updateTagInfoInsert]
INSERT INTO Track_Tag (track_id, tag_id) VALUES (?, ?)

-- [getArtistNameByArtistId]
SELECT name FROM Artist WHERE artist_id = ?

-- [getAlbumTitleByAlbumId]
SELECT title FROM Album WHERE album_id = ?

-- [getTagNameByTagId]
SELECT name FROM Tag WHERE tag_id = ?

