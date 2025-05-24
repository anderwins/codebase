/*
 * ==========================================================================
 *  FICHIER : insert_queries.sql
 *  DESCRIPTION :
 *    Ce fichier contient les requêtes SQL liées aux insertions dans la base
 *    de données.
 *
 *  STRUCTURE :
 *    ➤ Chaque requête est précédée d’un tag unique:
 *    ➤ Le chargement est effectué via SQLLoader qui récupère la requête par tag.
 *
 * ==========================================================================
 */

-- [insertUser]
INSERT INTO Users (username) VALUES (?);

-- [insertArtist]
INSERT INTO Artist (name) VALUES (?);

-- [insertAlbum]
INSERT INTO Album (title, artist_id) VALUES (?, ?);

-- [insertTag]
INSERT INTO Tag (name) VALUES (?);

-- [insertTrack]
INSERT INTO Track (title, artist_id, album_id, year, duration_sec, file_path, cover_path, lyrics_path, karaoke_path)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

-- [insertTrackTag]
INSERT INTO Track_Tag (track_id, tag_id) VALUES (?, ?);

-- [insertPlaylist]
INSERT INTO Playlist (name, user_id) VALUES (?, ?);

-- [insertTrackIntoPlaylist]
INSERT INTO PlaylistTrack (playlist_id, track_id) VALUES (?, ?);

-- [removeTrackFromPlaylist]
DELETE FROM PlaylistTrack WHERE playlist_id = ? AND track_id = ?

-- [removeAllTracksFromPlaylist]
DELETE FROM PlaylistTrack WHERE playlist_id = ?

-- [insertUserFavorite]
INSERT INTO UserFavorites (user_id, track_id) VALUES (?, ?);
