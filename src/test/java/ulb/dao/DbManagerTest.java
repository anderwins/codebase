package ulb.dao;

import org.junit.jupiter.api.*;
import ulb.Config;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de test pour DbManager.
 * Elle vérifie le comportement des méthodes de récupération d'ID
 * lorsque les éléments recherchés n'existent pas dans la base.
 */
public class DbManagerTest {

    private static final String TEST_DB_PATH = "test_db_manager.db";
    private Connection connection;
    private DbManager dbManager;

    // Implémentation concrète minimale pour tester l'abstraite DbManager
    private static class TestableDbManager extends DbManager {
        public TestableDbManager(Connection connection) {
            super(connection);
        }
    }



    @BeforeEach
    public void setupDatabase() throws Exception {
        // Disable logging before each test
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler); // Remove the default console handler
        rootLogger.setLevel(Level.OFF); // Set the root logger level to OFF

        // Supprimer l'ancienne base si elle existe
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) dbFile.delete();

        // Créer une nouvelle connexion SQLite
        connection = DriverManager.getConnection("jdbc:sqlite:" + TEST_DB_PATH);

        // Charger les requêtes de création des tables depuis le fichier SQL
        SQLLoader loader = new SQLLoader(Config.CREATE_TABLES_SQL_FILE);
        String createSQL = loader.getQuery("createAllTablesAndTriggers");

        // Exécuter la requête de création
        connection.createStatement().executeUpdate(createSQL);

        // Instancier le DbManager à tester
        dbManager = new TestableDbManager(connection);
    }

    @AfterEach
    public void cleanUp() throws Exception {
        if (connection != null) connection.close();
        File file = new File(TEST_DB_PATH);
        if (file.exists()) file.delete();
    }

    @Test
    public void testGetTrackIdReturnsMinusOneForNonExistentTitle() {
        int id = dbManager.getTrackId("titre_inconnu");
        assertEquals(-1, id);
    }

    @Test
    public void testGetArtistIdReturnsMinusOneForNonExistentArtist() {
        int id = dbManager.getArtistId("artiste_inconnu");
        assertEquals(-1, id);
    }

    @Test
    public void testGetAlbumIdReturnsMinusOneForNonExistentAlbum() {
        int id = dbManager.getAlbumId("album_inconnu");
        assertEquals(-1, id);
    }

    @Test
    public void testGetUserIdReturnsMinusOneForNonExistentUser() {
        int id = dbManager.getUserId("utilisateur_inconnu");
        assertEquals(-1, id);
    }

    @Test
    public void testGetPlaylistIdReturnsMinusOneForNonExistentPlaylist() {
        int id = dbManager.getPlaylistId("playlist_inconnue");
        assertEquals(-1, id);
    }

    @Test
    public void testGetTagIdReturnsMinusOneForNonExistentTag() {
        int id = dbManager.getTagId("tag_inconnu");
        assertEquals(-1, id);
    }

    @Test
    public void testCloseConnectionDoesNotThrow() {
        assertDoesNotThrow(() -> dbManager.closeConnection());
    }
}
