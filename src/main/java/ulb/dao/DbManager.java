package ulb.dao;

import ulb.Config;
import ulb.controller.handleError.DbManagerException;
import ulb.model.ChangeTracker;
import ulb.model.TrackLibrary;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Classe de base pour la gestion de la base de données.
 * Contient les méthodes utilitaires permettant d'obtenir les clés primaires 
 * des différentes entités de la base de données, ou -1 si elles n'existent pas.
 */
public abstract class DbManager {
    protected Connection connection; // Connexion active à la base de données
    protected final SQLLoader commonSqlLoader = new SQLLoader(Config.COMMON_QUERIES_SQL_FILE); // Chargeur SQL pour les requêtes génériques (lookup d'ID, vérification d'existence, etc.)
    private static final Logger logger = Logger.getLogger(DbManager.class.getName());
    protected TrackLibrary trackLibrary;// tmp, should be moved
    protected ChangeTracker changes; // same

    /**
     * Constructeur qui initialise la connexion à la base de données.
     * @param dbInitializer Instance de DbInitializer pour récupérer la connexion.
     */
    public DbManager(Connection connection) {
        this.connection = connection;
    }

    public void setChanges(ChangeTracker changes) {
        this.changes = changes;
    }

    public void setTrackLibrary(TrackLibrary trackLibrary) {
        this.trackLibrary = trackLibrary;
    }

    /**
     * Récupère l'ID d'un morceau à partir de son titre, ou -1 s'il n'existe pas.
     * @param title Le titre du morceau.
     * @return L'ID du morceau si trouvé, sinon -1.
     */
    public int getTrackId(String title) {
        return getId(commonSqlLoader.getQuery("getTrackIdByTitle"), title);
    }

    /**
     * Récupère l'ID d'un artiste à partir de son nom, ou -1 s'il n'existe pas.
     * @param artistName Le nom de l'artiste.
     * @return L'ID de l'artiste si trouvé, sinon -1.
     */
    public int getArtistId(String artistName) {
        return getId(commonSqlLoader.getQuery("getArtistIdByName"), artistName);
    }

    /**
     * Récupère l'ID d'un album à partir de son titre, ou -1 s'il n'existe pas.
     * @param albumTitle Le titre de l'album.
     * @return L'ID de l'album si trouvé, sinon -1.
     */
    public int getAlbumId(String albumTitle) {
        return getId(commonSqlLoader.getQuery("getAlbumIdByTitle"), albumTitle);
    }

    /**
     * Récupère l'ID d'un utilisateur à partir de son username, ou -1 s'il n'existe pas.
     * @param username Le nom d'utilisateur.
     * @return L'ID de l'utilisateur si trouvé, sinon -1.
     */
    public int getUserId(String username) {
        return getId(commonSqlLoader.getQuery("getUserIdByUsername"), username);
    }

    /**
     * Récupère l'ID d'une playlist à partir de son titre, ou -1 s'il n'existe pas.
     * @param playlistTitle Le titre de la playlist.
     * @return L'ID de la playlist si trouvé, sinon -1.
     */
    public int getPlaylistId(String playlistTitle) {
        return getId(commonSqlLoader.getQuery("getPlaylistIdByName"), playlistTitle);
    }

    /**
     * Récupère l'ID d'un tag à partir de son nom, ou -1 s'il n'existe pas.
     * @param tagName Le nom du tag.
     * @return L'ID du tag si trouvé, sinon -1.
     */
    public int getTagId(String tagName) {
        return getId(commonSqlLoader.getQuery("getTagIdByName"), tagName);
    }

    /**
     * Méthode générique pour récupérer une clé primaire dans une table donnée.
     * Renvoie -1 si l'élément n'existe pas dans la table.
     * @param query La requête SQL pour récupérer l'ID.
     * @param parameter La valeur à rechercher.
     * @return L'ID trouvé ou -1 si l'élément n'existe pas.
     */
    public int getId(String query, String parameter) {
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, parameter);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.warning("⚠️ Échec lors de l'exécution de la requête '" + query + "' avec paramètre : " + parameter + " | " + e.getMessage());

        }
        return -1;
    }

    /**
     * Ferme la connexion à la base de données.
     */
    public void closeConnection() throws DbManagerException {
        try {
            if (connection != null) {
                connection.close();
                logger.info("Connexion fermée.");
            }
        } catch (SQLException e) {
            logger.warning("Erreur lors de la fermeture de la connexion : " + e.getMessage());
            throw new DbManagerException("Erreur lors de la fermeture de la connexion", e); 
        }
    }
}
