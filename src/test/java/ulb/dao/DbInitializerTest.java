package ulb.dao;

import org.junit.jupiter.api.*;

import ulb.Config;
import ulb.controller.handleError.DatabaseInitializationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DbInitializerTest {

    private DbInitializer dbInitializer;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setup() throws DatabaseInitializationException, Config.CouldNotSetUpDataFolder {
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler);
        rootLogger.setLevel(Level.OFF);
        Config.setUpFolders();
        // Rediriger la sortie console pour vérifier les messages
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        // Initialiser la base (utilise le chemin par défaut dans Config)
        dbInitializer = new DbInitializer();
    }


    @AfterEach
    public void tearDown() {
        dbInitializer.closeConnection();

        // Rétablir la sortie standard
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testConnectionIsEstablished() {
        Connection conn = dbInitializer.getConnection();
        assertNotNull(conn, "La connexion doit être active.");
        //assertTrue(outContent.toString().contains("Connexion établie"), "Message de connexion manquant.");
    }

    @Test
    public void testDatabaseFileIsCreated() {
        File dbFile = new File(Config.getFullPathFromRelative(Config.DATABASE_PATH));
        assertTrue(dbFile.exists(), "Le fichier de base de données doit exister.");
    }

    @Test
    public void testTablesAreCreated() {
        String[] expectedTables = {
            "Track", "Artist", "Album", "Tag", "Playlist", "PlaylistTrack",
            "Users", "UserFavorites", "Track_Tag"
        };

        try {
            Connection conn = dbInitializer.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");

            // Stocker les noms de tables existantes
            java.util.Set<String> existingTables = new java.util.HashSet<>();
            while (rs.next()) {
                existingTables.add(rs.getString("name"));
            }

            // Vérifier chaque table attendue
            for (String table : expectedTables) {
                assertTrue(existingTables.contains(table), "La table " + table + " devrait exister.");
            }

        } catch (Exception e) {
            fail("Erreur lors de la vérification des tables : " + e.getMessage());
        }
    }

}
