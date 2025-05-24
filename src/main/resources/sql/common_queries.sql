/*
 * ==========================================================================
 *  FICHIER : common_queries.sql
 *  DESCRIPTION :
 *    Ce fichier contient les requêtes SQL partagées et communes, utilisées
 *    dans la classe DbManager pour rechercher les IDs et vérifier l’existence
 *    d'éléments dans la base de données.
 *
 *  STRUCTURE :
 *    ➤ Chaque requête est précédée d’un tag unique:
 *    ➤ Chargé via SQLLoader dans DbManager, et réutilisé dans les classes heritantes  
 *
 * ==========================================================================
 */

-- [getTrackIdByTitle]
SELECT track_id FROM Track WHERE title = ?;

-- [getArtistIdByName]
SELECT artist_id FROM Artist WHERE name = ?;

-- [getAlbumIdByTitle]
SELECT album_id FROM Album WHERE title = ?;

-- [getTagIdByName]
SELECT tag_id FROM Tag WHERE name = ?;

-- [getPlaylistIdByName]
SELECT playlist_id FROM Playlist WHERE name = ?;

-- [getUserIdByUsername]
SELECT user_id FROM Users WHERE username = ?;
