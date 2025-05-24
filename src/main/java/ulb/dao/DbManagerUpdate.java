package ulb.dao;
import ulb.model.*;
import ulb.Config;
import ulb.view.utils.AlertManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Classe spécialisée dans la mise à jour des données de la base de données.
 * Hérite de DbManager pour réutiliser les méthodes de gestion des clés primaires.
 */
public class DbManagerUpdate extends DbManager {
     private final SQLLoader updateSqlLoader = new SQLLoader(Config.UPDATE_QUERIES_SQL_FILE);//Chargeur SQL pour les requêtes de mise à jour dans la base de données
    private DbManagerInsert dbInsert;
    private static final Logger logger = Logger.getLogger(DbManagerUpdate.class.getName());

    public DbManagerUpdate(Connection  connection, DbManagerInsert dbInsert) {
        super(connection);
        this.dbInsert = dbInsert;
    }

    public void syncTracks(){
        for(Integer id : changes.getTracksChanged()){
            updateTrack(trackLibrary.get(id));
        }
        changes.clearChanges();
    }

    public boolean updateTrack(Track track) {
        logger.info("🔍 Recherche du morceau avec le file_path : " + track.getFilePath());
        Track existingTrack = findTrackByFilePath(track.getFilePath());

        if (existingTrack == null) {
            logger.warning("Le morceau à mettre à jour n'existe pas: " + track.getFilePath());
            AlertManager.showWarning("Mise à jour impossible", 
                "Le morceau à mettre à jour n'existe pas.");
            return false;
        }

        logger.info("✅ Track trouvé : " + existingTrack.getTitle());
        
        boolean autoCommitStatus = false;
        try {
            autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false); // Start transaction

            if (!updateTrackInfo(track, existingTrack)) {
                connection.rollback();
                return false;
            }
            if (!updateArtistInfo(track.getArtist(), existingTrack.getArtist(), existingTrack.getTrackId())) { // Pass trackId for artist update
                connection.rollback();
                return false;
            }
            if (!updateAlbumInfo(track.getAlbum(), existingTrack.getAlbum(), track.getArtist(), existingTrack.getTrackId())) { // Pass trackId for album update
                connection.rollback();
                return false;
            }
            if (!updateTagInfo(track, existingTrack)) {
                connection.rollback();
                return false;
            }

            connection.commit(); // Commit transaction
            logger.info("✅ Mise à jour complète du morceau !");
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL lors de la mise à jour transactionnelle du morceau", e);
            try {
                connection.rollback(); // Rollback on error
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Erreur lors du rollback de la mise à jour du morceau", ex);
            }
            AlertManager.showErrorWithException("Erreur de base de données", 
                "Impossible de mettre à jour le morceau transactionnellement.", e);
            return false;
        } finally {
            try {
                if (autoCommitStatus) { // Only set back if it was true
                    connection.setAutoCommit(true); // Restore auto-commit status
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Erreur lors de la restauration de l'auto-commit", ex);
            }
        }
    }

    private Track findTrackByFilePath(String filePath) {
        String query = updateSqlLoader.getQuery("findTrackByFilePath");

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, filePath);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int artistId = rs.getInt("artist_id");
                int albumId = rs.getInt("album_id");
                int tagId = rs.getInt("tag_id");

                return new Track(
                    rs.getInt("track_id"),
                    rs.getString("title"),
                    getArtistNameById(artistId),  
                    getAlbumTitleById(albumId),   
                    rs.getString("year"),
                    rs.getInt("duration_sec"),
                    getTagNameById(tagId),        
                    rs.getString("file_path"),
                    rs.getString("cover_path"),
                    rs.getString("lyrics_path"),
                    rs.getString("karaoke_path")
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recherche du morceau", e);
            AlertManager.showErrorWithException("Erreur de recherche", 
                "Impossible de trouver le morceau avec le chemin spécifié", e);
        }
        return null;
    }

    private boolean updateTrackInfo(Track newTrack, Track existingTrack) {
        logger.info("🔄 Mise à jour des informations du morceau...");
        String query = updateSqlLoader.getQuery("updateTrackInfo");
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newTrack.getTitle());
            stmt.setString(2, newTrack.getYear());
            stmt.setInt(3, newTrack.getDuration());
            stmt.setString(4, newTrack.getCoverPath());
            stmt.setInt(5, existingTrack.getTrackId());
            stmt.executeUpdate();
            logger.info("✅ Mise à jour du morceau réussie.");
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour du morceau", e);
            AlertManager.showErrorWithException("Erreur de mise à jour", 
                "Impossible de mettre à jour les informations du morceau", e);
            return false;
        }
    }

    private boolean updateArtistInfo(String newArtist, String oldArtist, int trackId) { // Added trackId parameter
        if (newArtist.equals(oldArtist)) return true; 

        logger.info("🔄 Mise à jour de l'artiste...");
        int artistId = getArtistId(newArtist);
        if (artistId == -1) {
            if (!dbInsert.insertArtist(newArtist)) { // Check return value
                logger.warning("Échec de l'insertion du nouvel artiste: " + newArtist);
                return false; // Failed to insert new artist
            }
            artistId = getArtistId(newArtist);
            if (artistId == -1) { // Verify artist was indeed created
                 logger.warning("Nouvel artiste " + newArtist + " non trouvé après tentative d'insertion.");
                 return false;
            }
        }

        String query = updateSqlLoader.getQuery("updateTrackArtist"); // Changed query name for clarity if needed
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, artistId);
            stmt.setInt(2, trackId); // Use trackId to update specific track
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("✅ Mise à jour de l'artiste pour le morceau réussie.");
                return true;
            } else {
                logger.warning("Aucune ligne affectée lors de la mise à jour de l'artiste pour le track ID: " + trackId);
                return false; // Or handle as appropriate if 0 rows affected is an error
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour de l'artiste pour le morceau", e);
            AlertManager.showErrorWithException("Erreur de mise à jour", 
                "Impossible de mettre à jour l'artiste pour le morceau", e);
            return false;
        }
    }

    private boolean updateAlbumInfo(String newAlbum, String oldAlbum, String artistName, int trackId) { // Added artistName and trackId
        if (newAlbum.equals(oldAlbum)) return true;

        logger.info("🔄 Mise à jour de l'album...");
        int albumId = getAlbumId(newAlbum);
        if (albumId == -1) {
            if (!dbInsert.insertAlbum(newAlbum, artistName)) { // Check return value
                logger.warning("Échec de l'insertion du nouvel album: " + newAlbum + " pour l'artiste " + artistName);
                return false; // Failed to insert new album
            }
            albumId = getAlbumId(newAlbum);
             if (albumId == -1) { // Verify album was indeed created
                 logger.warning("Nouvel album " + newAlbum + " non trouvé après tentative d'insertion.");
                 return false;
            }
        }

        String query = updateSqlLoader.getQuery("updateTrackAlbum"); // Changed query name for clarity if needed
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, albumId);
            stmt.setInt(2, trackId); // Use trackId to update specific track
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("✅ Mise à jour de l'album pour le morceau réussie.");
                return true;
            } else {
                logger.warning("Aucune ligne affectée lors de la mise à jour de l'album pour le track ID: " + trackId);
                return false; // Or handle as appropriate
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour de l'album pour le morceau", e);
            AlertManager.showErrorWithException("Erreur de mise à jour", 
                "Impossible de mettre à jour l'album pour le morceau", e);
            return false;
        }
    }

    private boolean updateTagInfo(Track newTrack, Track existingTrack) {
        logger.info("🔄 Mise à jour des tags...");
        String deleteQuery = updateSqlLoader.getQuery("updateTagInfoDelete");
        try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
            stmt.setInt(1, existingTrack.getTrackId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la suppression des anciens tags", e);
            AlertManager.showErrorWithException("Erreur de mise à jour", 
                "Impossible de supprimer les anciens tags", e);
            return false;
        }

        int tagId = getTagId(newTrack.getGenre());
        if (tagId == -1) {
            dbInsert.insertTag(newTrack.getGenre());
            tagId = getTagId(newTrack.getGenre());
        }

        String insertQuery = updateSqlLoader.getQuery("updateTagInfoInsert");
        try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            stmt.setInt(1, existingTrack.getTrackId());
            stmt.setInt(2, tagId);
            stmt.executeUpdate();
            logger.info("✅ Mise à jour des tags réussie.");
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'ajout du tag", e);
            AlertManager.showErrorWithException("Erreur de mise à jour", 
                "Impossible d'ajouter le nouveau tag", e);
            return false;
        }
    }

    private String getArtistNameById(int artistId) {
        return getStringValue(updateSqlLoader.getQuery("getArtistNameByArtistId"), artistId);
    }

    private String getAlbumTitleById(int albumId) {
        return getStringValue(updateSqlLoader.getQuery("getAlbumTitleByAlbumId"), albumId);
    }

    private String getTagNameById(int tagId) {
        return getStringValue(updateSqlLoader.getQuery("getTagNameByTagId"), tagId);
    }

    private String getStringValue(String query, int id) {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des données", e);
            AlertManager.showErrorWithException("Erreur de base de données", 
                "Impossible de récupérer les données", e);
        }
        return null;
    }

    /**
     * Supprime une playlist de la base de données.
     * @param playlistTitle Le titre de la playlist à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deletePlaylist(String playlistTitle) {
        logger.info("🔄 Suppression de la playlist : " + playlistTitle);
        
        boolean autoCommitStatus = false;
        try {
            autoCommitStatus = connection.getAutoCommit();
            connection.setAutoCommit(false); // Start transaction

            // Supprimer d'abord les entrées dans la table PlaylistTrack
            String deleteTracksQuery = "DELETE FROM PlaylistTrack WHERE playlist_id = (SELECT playlist_id FROM Playlist WHERE name = ?);";
            try (PreparedStatement stmt = connection.prepareStatement(deleteTracksQuery)) {
                stmt.setString(1, playlistTitle);
                stmt.executeUpdate(); 
                // We don't strictly need to check rows affected here, 
                // as it's okay if a playlist had no tracks.
            }
            
            // Ensuite supprimer la playlist elle-même
            String deletePlaylistQuery = "DELETE FROM Playlist WHERE name = ?;";
            try (PreparedStatement stmt = connection.prepareStatement(deletePlaylistQuery)) {
                stmt.setString(1, playlistTitle);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    connection.commit(); // Commit transaction
                    logger.info("✅ Playlist supprimée avec succès.");
                    return true;
                } else {
                    connection.rollback(); // Rollback if playlist not found / not deleted
                    logger.info("⚠️ Aucune playlist trouvée avec ce titre ou échec de la suppression.");
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL lors de la suppression de la playlist", e);
            try {
                connection.rollback(); // Rollback on error
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Erreur lors du rollback de la suppression de playlist", ex);
            }
            AlertManager.showErrorWithException("Erreur de suppression", 
                "Impossible de supprimer la playlist", e);
            return false;
        } finally {
            try {
                if (autoCommitStatus) { // Only set back if it was true
                     connection.setAutoCommit(true); // Restore auto-commit status
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Erreur lors de la restauration de l'auto-commit pour la suppression de playlist", ex);
            }
        }
    }
}
