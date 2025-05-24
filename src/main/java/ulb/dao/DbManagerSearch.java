package ulb.dao;
import ulb.model.*;
import ulb.Config;
import ulb.view.utils.AlertManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Classe spécialisée dans la recherche des morceaux, artistes, albums et playlists dans la base de données.
 * Hérite de `DbManager` pour réutiliser les méthodes de gestion des clés primaires.
 */
public class DbManagerSearch extends DbManager {
    private final SQLLoader searchSqlLoader = new SQLLoader(Config.SEARCH_QUERIES_SQL_FILE); // Chargeur SQL pour les requêtes de recherche dans la base de données
    private static final Logger logger = Logger.getLogger(DbManagerSearch.class.getName());

    public DbManagerSearch(Connection  connection) {
        super(connection);
    }

    /**
     * Recherche tous les morceaux dont le titre commence par la chaîne donnée.
     * @param title Début du titre recherché.
     * @return Une liste contenant tous les morceaux correspondants, ou une liste vide si aucun trouvé.
     */
    public ArrayList<Track> searchTracksByTitle(String title) {
        ArrayList<Track> tracks = new ArrayList<>();
        String query = searchSqlLoader.getQuery("searchTracksByTitle");
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, title + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Track track = new Track(
                        rs.getInt("track_id"),
                        rs.getString("title"),
                        getArtistName(rs.getInt("artist_id")),
                        getAlbumName(rs.getInt("album_id")),
                        String.valueOf(rs.getInt("year")),
                        rs.getInt("duration_sec"),
                        getTrackTag(rs.getInt("track_id")),
                        rs.getString("file_path"),
                        rs.getString("cover_path"),
                        rs.getString("lyrics_path"),
                        rs.getString("karaoke_path")
                    );
                    tracks.add(track);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recherche par titre", e);
            AlertManager.showErrorWithException("Erreur de recherche", 
                "Impossible de rechercher les morceaux par titre", e);
        }
        return tracks;
    }

    /**
     * Recherche tous les morceaux d'un artiste donné.
     */
    public ArrayList<Track> searchTracksByArtist(String artistName) {
        ArrayList<Track> tracks = new ArrayList<>();
        int artistId = getArtistId(artistName);
        if (artistId == -1) return tracks;
        String query = searchSqlLoader.getQuery("searchTrackByArtist");
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, artistId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Track track = new Track(
                        rs.getInt("track_id"),
                        rs.getString("title"),
                        artistName,
                        getAlbumName(rs.getInt("album_id")),
                        String.valueOf(rs.getInt("year")),
                        rs.getInt("duration_sec"),
                        getTrackTag(rs.getInt("track_id")),
                        rs.getString("file_path"),
                        rs.getString("cover_path"),
                        rs.getString("lyrics_path"),
                        rs.getString("karaoke_path")
                    );
                    tracks.add(track);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recherche par artiste", e);
            AlertManager.showErrorWithException("Erreur de recherche", 
                "Impossible de rechercher les morceaux par artiste", e);
        }
        return tracks;
    }

    /**
     * Recherche tous les morceaux d'un album donné.
     */
    public ArrayList<Track> searchTracksByAlbum(String albumTitle) {
        ArrayList<Track> tracks = new ArrayList<>();
        int albumId = getAlbumId(albumTitle);
        if (albumId == -1) return tracks;
        String query = searchSqlLoader.getQuery("searchTrackByAlbum");
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, albumId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Track track = new Track(
                        rs.getInt("track_id"),
                        rs.getString("title"),
                        getArtistName(rs.getInt("artist_id")),
                        albumTitle,
                        String.valueOf(rs.getInt("year")),
                        rs.getInt("duration_sec"),
                        getTrackTag(rs.getInt("track_id")),
                        rs.getString("file_path"),
                        rs.getString("cover_path"),
                        rs.getString("lyrics_path"),
                        rs.getString("karaoke_path")
                    );
                    tracks.add(track);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recherche par album", e);
            AlertManager.showErrorWithException("Erreur de recherche", 
                "Impossible de rechercher les morceaux par album", e);
        }
        return tracks;
    }

    /**
     * Recherche tous les morceaux associés à un tag donné.
     */
    public ArrayList<Track> searchTracksByTag(String tagName) {
        ArrayList<Track> tracks = new ArrayList<>();
        int tagId = getTagId(tagName);
        if (tagId == -1) return tracks;
        String query = searchSqlLoader.getQuery("searchTracksByTag");
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, tagId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Track track = new Track(
                        rs.getInt("track_id"),
                        rs.getString("title"),
                        getArtistName(rs.getInt("artist_id")),
                        getAlbumName(rs.getInt("album_id")),
                        String.valueOf(rs.getInt("year")),
                        rs.getInt("duration_sec"),
                        getTrackTag(rs.getInt("track_id")),
                        rs.getString("file_path"),
                        rs.getString("cover_path"),
                        rs.getString("lyrics_path"),
                        rs.getString("karaoke_path")
                    );
                    tracks.add(track);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recherche par tag", e);
            AlertManager.showErrorWithException("Erreur de recherche", 
                "Impossible de rechercher les morceaux par genre musical", e);
        }
        return tracks;
    }

    /**
     * Récupère le nom d'un artiste à partir de son ID.
     */
    private String getArtistName(int artistId) {
        return getStringValue(searchSqlLoader.getQuery("getArtistNameFromArtistID"), artistId);
    }

    /**
     * Récupère le titre d'un album à partir de son ID.
     */
    private String getAlbumName(int albumId) {
        return getStringValue(searchSqlLoader.getQuery("getAlbumNameFromAbumID"), albumId);
    }

    /**
     * Récupère le tag (genre musical) associé à un morceau.
     * @param trackId L'ID du morceau.
     * @return Le nom du tag associé, ou une chaîne vide si aucun tag trouvé.
     */
    private String getTrackTag(int trackId) {
        return getStringValue(searchSqlLoader.getQuery("getTagTrackFromTrackID"), trackId);
    }

    /**
     * Méthode utilitaire pour récupérer une valeur `String` depuis la base de données.
     */
    private String getStringValue(String query, int id) {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des données", e);
            AlertManager.showErrorWithException("Erreur de base de données", 
                "Impossible de récupérer les données", e);
        }
        return "";
    }

    /**
     * Récupère toutes les playlists avec leurs morceaux.
     * @return Une map associant chaque nom de playlist à une liste d'objets `Track`.
     */
    public Map<String, List<Track>> getAllPlaylistsWithTracks() {
        Map<String, List<Track>> playlists = new HashMap<>();
        String query = searchSqlLoader.getQuery("getAllPlaylistsWithTracks");

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String playlistName = rs.getString("title");
                int playlistId = rs.getInt("playlist_id");
                playlists.put(playlistName, getTracksForPlaylist(playlistId));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des playlists", e);
            AlertManager.showErrorWithException("Erreur de chargement", 
                "Impossible de récupérer les playlists", e);
        }
        return playlists;
    }

    /**
     * Récupère les morceaux associés à une playlist donnée.
     * @param playlistId L'ID de la playlist.
     * @return Liste des morceaux sous forme d'objets `Track`.
     */
    private List<Track> getTracksForPlaylist(int playlistId) {
        List<Track> tracks = new ArrayList<>();
        String query = searchSqlLoader.getQuery("getTracksForPlaylist");

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, playlistId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tracks.add(createTrackFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des morceaux pour la playlist ID: " + playlistId, e);
            AlertManager.showErrorWithException("Erreur de chargement",
                "Impossible de récupérer les morceaux pour la playlist", e);
        }
        return tracks;
    }

    /**
     * Crée un objet `Track` à partir d'un ResultSet.
     * @param rs Résultat d'une requête SQL.
     * @return Un objet `Track`.
     */
    private Track createTrackFromResultSet(ResultSet rs) throws SQLException {
        return new Track(
            rs.getInt("track_id"),  // ✅ Correctement défini ici
            rs.getString("title"),
            rs.getString("artist"),
            rs.getString("album"),
            rs.getString("year"),
            rs.getInt("duration_sec"),
            rs.getString("genre"),
            rs.getString("file_path"),
            rs.getString("cover_path"),
            rs.getString("lyrics_path"),
            rs.getString("karaoke_path")
        );
    }

    /**
     * Récupère tous les morceaux présents dans la table Track de la base de données.
     * <p>
     * Cette méthode parcourt l'ensemble des enregistrements de la table Track
     * et utilise les méthodes utilitaires pour récupérer les noms d'artistes, d'albums
     * et le genre musical (tag) associés à chaque morceau.
     * </p>
     *
     * @return Une liste contenant tous les objets Track enregistrés dans la base de données.
     */
    public ArrayList<Track> getAllTracks() {
        ArrayList<Track> tracksList = new ArrayList<>();

        String query = searchSqlLoader.getQuery("getAllTracks");

        try (PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Track track = new Track(
                    rs.getInt("track_id"),
                    rs.getString("title"),
                    getArtistName(rs.getInt("artist_id")),
                    getAlbumName(rs.getInt("album_id")),
                    String.valueOf(rs.getInt("year")),
                    rs.getInt("duration_sec"),
                    getTrackTag(rs.getInt("track_id")),
                    rs.getString("file_path"),
                    rs.getString("cover_path"),
                    rs.getString("lyrics_path"),
                    rs.getString("karaoke_path")
                );
                tracksList.add(track);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de tous les morceaux", e);
            AlertManager.showErrorWithException("Erreur de chargement", 
                "Impossible de récupérer tous les morceaux", e);
        }
        return tracksList;
    }
}

