package ulb.dao;
import ulb.model.*;
import ulb.Config;
import java.util.logging.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection; 

/**
 * Classe spécialisée dans l'insertion de données dans la base de données.
 * Hérite de `DbManager` pour profiter des méthodes utilitaires de gestion des clés primaires.
 */
public class DbManagerInsert extends DbManager {
    private final SQLLoader insertSqlLoader = new SQLLoader(Config.INSERT_QUERIES_SQL_FILE);// Chargeur SQL pour les requêtes d'insertion dans la base de données

    /**
     * Constructeur qui initialise la connexion via `DbInitializer`.
     */
    public DbManagerInsert(Connection  connection) {
        super(connection);
    }

    /**
     * Insère un nouvel utilisateur dans la base de données s'il n'existe pas déjà.
     * @param username Nom unique de l'utilisateur.
     * @return `true` si l'insertion a réussi, sinon `false`.
     */
    public boolean insertUser(String username) {
        if (getUserId(username) != -1) return false;
        String query = insertSqlLoader.getQuery("insertUser");
        return executeInsert(query, username);
    }

    /**
     * Insère un nouvel artiste dans la base de données s'il n'existe pas déjà.
     * @param artistName Nom de l'artiste.
     * @return `true` si l'insertion a réussi, sinon `false`.
     */
    public boolean insertArtist(String artistName) {
        if (getArtistId(artistName) != -1) return false;
        String query = insertSqlLoader.getQuery("insertArtist");
        return executeInsert(query, artistName);
    }

    /**
     * Insère un nouvel album dans la base de données s'il n'existe pas déjà.
     * @param albumTitle Titre de l'album.
     * @param artistName Nom de l'artiste associé à l'album.
     * @return `true` si l'insertion a réussi, sinon `false`.
     */
    public boolean insertAlbum(String albumTitle, String artistName) {
        if (getAlbumId(albumTitle) != -1) return false;
        int artistId = getArtistId(artistName);
        if (artistId == -1) return false;
        String query = insertSqlLoader.getQuery("insertAlbum");
        return executeInsert(query, albumTitle, artistId);
    }

    /**
     * Insère un nouveau tag (genre musical) dans la base de données s'il n'existe pas déjà.
     * @param tagName Nom du genre musical.
     * @return `true` si l'insertion a réussi, sinon `false`.
     */
    public boolean insertTag(String tagName) {
        if (getTagId(tagName) != -1) return false;
        String query = insertSqlLoader.getQuery("insertTag");
        return executeInsert(query, tagName);
    }

    /**
     * Insère un nouveau morceau dans la base de données s'il n'existe pas déjà.
     * Vérifie et crée l'artiste, l'album et le genre musical si nécessaire.
     * @param track Objet `Track` contenant les informations du morceau.
     * @return `true` si l'insertion a réussi, sinon `false`.
     */
    public boolean insertTrack(Track track) {
        if (getTrackId(track.getTitle()) != -1) return false;

        int artistId = getArtistId(track.getArtist());
        if (artistId == -1) {
            insertArtist(track.getArtist());
            artistId = getArtistId(track.getArtist());
        }
        int albumId = getAlbumId(track.getAlbum());
        if (albumId == -1) {
            insertAlbum(track.getAlbum(), track.getArtist());
            albumId = getAlbumId(track.getAlbum());
        }
        int tagId = getTagId(track.getGenre());
        if (tagId == -1) {
            insertTag(track.getGenre());
            tagId = getTagId(track.getGenre());
        }
        String query = insertSqlLoader.getQuery("insertTrack");
        boolean trackInserted = executeInsert(query, track.getTitle(), artistId, albumId, track.getYear(), track.getDuration(), track.getFilePath(), track.getCoverPath(), track.getLyricsPath(), track.getKaraokePath());
        
        if (trackInserted) {
            int trackId = getTrackId(track.getTitle());
            String tagQuery = insertSqlLoader.getQuery("insertTrackTag");
            executeInsert(tagQuery, trackId, tagId);
        }
        return trackInserted;
    }

    /**
     * Insère une nouvelle playlist dans la base de données si elle n'existe pas déjà.
     * @param playlistTitle Titre de la playlist.
     * @param username Nom d'utilisateur du propriétaire.
     * @return `true` si l'insertion a réussi, sinon `false`.
     */
    public boolean insertPlaylist(String playlistTitle, String username) {
        if (getPlaylistId(playlistTitle) != -1) {
            return false;}

        String query = insertSqlLoader.getQuery("insertPlaylist");
        return executeInsert(query, playlistTitle, -1);
    }

    /**
     * Ajoute un morceau à une playlist.
     * @param playlistTitle Titre de la playlist.
     * @param trackTitle Titre du morceau.
     * @return `true` si l'ajout a réussi, sinon `false`.
     */
    public boolean addTrackToPlaylist(String playlistTitle, String trackTitle) {
        int playlistId = getPlaylistId(playlistTitle);
        int trackId = getTrackId(trackTitle);
        if (playlistId == -1 || trackId == -1) return false;
        
        // Vérifie si le morceau existe déjà dans la playlist
        try {
            String checkQuery = "SELECT 1 FROM PlaylistTrack WHERE playlist_id = ? AND track_id = ?";
            java.sql.PreparedStatement stmt = connection.prepareStatement(checkQuery);
            stmt.setInt(1, playlistId);
            stmt.setInt(2, trackId);
            java.sql.ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Le morceau existe déjà dans la playlist, ne pas insérer
                rs.close();
                stmt.close();
                return false;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            Logger logger = Logger.getLogger(DbManagerInsert.class.getName());
            logger.warning("Erreur lors de la vérification de doublon dans la playlist : " + e.getMessage());
            // On continue et on essaie d'insérer quand même
        }
        
        String query = insertSqlLoader.getQuery("insertTrackIntoPlaylist");
        return executeInsert(query, playlistId, trackId);
    }

    /**
     * Supprime un morceau d'une playlist.
     * @param playlistTitle Titre de la playlist.
     * @param trackTitle Titre du morceau.
     * @return `true` si la suppression a réussi, sinon `false`.
     */
    public boolean removeTrackFromPlaylist(String playlistTitle, String trackTitle) {
        int playlistId = getPlaylistId(playlistTitle);
        int trackId = getTrackId(trackTitle);
        if (playlistId == -1 || trackId == -1) return false;
        String query = insertSqlLoader.getQuery("removeTrackFromPlaylist");
        return executeInsert(query, playlistId, trackId);
    }
    
    /**
     * Supprime tous les morceaux d'une playlist.
     * @param playlistTitle Titre de la playlist.
     * @return `true` si la suppression a réussi, sinon `false`.
     */
    public boolean removeAllTracksFromPlaylist(String playlistTitle) {
        int playlistId = getPlaylistId(playlistTitle);
        if (playlistId == -1) return false;
        String query = insertSqlLoader.getQuery("removeAllTracksFromPlaylist");
        return executeInsert(query, playlistId);
    }

    /**
     * Méthode générique pour exécuter une requête d'insertion dans la base de données.
     * @param query Requête SQL à exécuter.
     * @param parameters Paramètres de la requête SQL.
     * @return `true` si l'insertion a réussi, sinon `false`.
     */
    private boolean executeInsert(String query, Object... parameters) {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof String) {
                    stmt.setString(i + 1, (String) parameters[i]);
                } else if (parameters[i] instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) parameters[i]);
                }
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Logger logger = Logger.getLogger(DbManagerInsert.class.getName());
            logger.severe("Erreur lors de l'insertion dans la base de données : " + e.getMessage());
            return false;
        }
    }
}
