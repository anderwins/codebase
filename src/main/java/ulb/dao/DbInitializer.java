package ulb.dao;

import ulb.Config;
import ulb.controller.handleError.DatabaseInitializationException;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;


/**
 * Classe responsable de l'initialisation de la base de données.
 * Elle vérifie si la base et les tables existent, sinon elle crée tout.
 */
public class DbInitializer {

    private final String DbPath = String.valueOf(Config.getFullPathFromRelative(Config.DATABASE_PATH));
    private final SQLLoader createSqlLoader = new SQLLoader(Config.CREATE_TABLES_SQL_FILE); // Chargeur SQL pour les requêtes de creation des tables 
    private final Connection conn;
    public static final Logger logger = Logger.getLogger(DbInitializer.class.getName());

    /**
     * Initialise la base de données et crée les tables si elles n'existent pas.
     */
    public DbInitializer() throws DatabaseInitializationException {
        try {
            Class.forName("org.sqlite.JDBC"); // for some obscure reason, explicit loading is needed in jar files
        }catch(ClassNotFoundException e){
            logger.severe("Pilote JDBC SQLite non trouvé : " + e.getMessage());
            throw new RuntimeException("Impossible de charger le pilote JDBC", e);
        }
        try {
            if (!databaseExists()) {
                logger.info("📁 Création de la base de données : " + DbPath);
            }
            conn = DriverManager.getConnection("jdbc:sqlite:" +  DbPath);

            if (conn == null) {
                throw new SQLException("Connexion JDBC retournée null.");
            }
            logger.info("✅ Connexion établie avec la base de données.");
            if (!tablesExist()) {
                createTables();
                logger.info("✅ Tables et triggers créés avec succès !");
            } else {
                logger.info("ℹ️ Les tables existent déjà, aucune création nécessaire.");
            }
        } catch (SQLException e) {
            logger.severe("❌ Erreur d'initialisation de la base de données : " + e.getMessage());
            throw new DatabaseInitializationException("Échec de connexion à la base de données", e);
        }
    }

    /**
     * Vérifie si la base de données existe physiquement sur le disque.
     * @return true si le fichier existe, sinon false.
     */
    private boolean databaseExists() {
        File dbFile = new File(DbPath);
        return dbFile.exists();
    }

    /**
     * Vérifie si au moins une table utilisateur existe dans la base.
     * @return true si une table est trouvée, sinon false.
     */
    private boolean tablesExist() {
        String sql = createSqlLoader.getQuery("tablesExist");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            logger.info("❌ Erreur lors de la vérification des tables : " + e.getMessage());
            return false;
        }
    }

    /**
     * Crée toutes les tables et triggers à partir du fichier SQL.
     * @throws SQLException si une erreur se produit lors de l'exécution SQL.
     */
    private void createTables() throws SQLException {
        String sql = createSqlLoader.getQuery("createAllTablesAndTriggers");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.severe("❌ Erreur lors de la création des tables : " + e.getMessage());
            throw e; // Re-throw SQLException
        }
    }

    /**
     * Fournit la connexion à la base de données.
     * @return objet Connection actif.
     */
    public Connection getConnection() {
        return conn;
    }

    /**
     * Ferme proprement la connexion à la base de données.
     */
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                logger.info("🔒 Connexion fermée.");
            } catch (SQLException e) {
                logger.warning("❌ Erreur lors de la fermeture de la connexion : " + e.getMessage());
            }
        }
    }
}
