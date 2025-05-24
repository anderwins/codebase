package ulb.dao;

import org.junit.jupiter.api.*;
import ulb.model.Track;
import ulb.Config;
import ulb.view.utils.AlertManager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DbManagerUpdateTest {

    private static final String TEST_DB_PATH = "test_db_update.db";
    private Connection connection;
    private DbManagerInsert dbInsert;
    private DbManagerUpdate dbUpdate;

    @BeforeAll
    public static void setUpClass() {
        // Disable logging before each test
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler); // Remove the default console handler
        rootLogger.setLevel(Level.OFF); // Set the root logger level to OFF

        // Désactiver les alertes visuelles pour tous les tests
        AlertManager.setAlertsDisabled(true);
    }
    
    @AfterAll
    public static void tearDownClass() {
        // Réactiver les alertes visuelles après tous les tests
        AlertManager.setAlertsDisabled(false);
    }

    @BeforeEach
    public void setUp() throws Exception {
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) dbFile.delete();
        connection = DriverManager.getConnection("jdbc:sqlite:" + TEST_DB_PATH);

        SQLLoader loader = new SQLLoader(Config.CREATE_TABLES_SQL_FILE);
        connection.createStatement().executeUpdate(loader.getQuery("createAllTablesAndTriggers"));

        dbInsert = new DbManagerInsert(connection);
        dbUpdate = new DbManagerUpdate(connection, dbInsert);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
        File file = new File(TEST_DB_PATH);
        if (file.exists()) file.delete();
    }

    @Test
    public void testUpdateTrackShouldSucceed() {
        Track original = new Track(0, "Titre", "Artiste", "Album", "2020", 300, "Pop",
                "chemin.mp3", "cover.jpg", "lyrics.txt", "karaoke.lrc");

        dbInsert.insertArtist("Artiste");
        dbInsert.insertAlbum("Album", "Artiste");
        dbInsert.insertTag("Pop");
        dbInsert.insertTrack(original);

        Track updated = new Track(0, "Nouveau titre", "Nouvel artiste", "Nouvel album", "2021", 350, "Rock",
                "chemin.mp3", "new_cover.jpg", "lyrics.txt", "karaoke.lrc");

        assertTrue(dbUpdate.updateTrack(updated));
    }

    @Test
    public void testUpdateTrackShouldFailIfTrackDoesNotExist() {
        Track fake = new Track(0, "Fake", "Nobody", "Nothing", "1999", 100, "Unknown",
                "inexistant.mp3", "", "", "");
        assertFalse(dbUpdate.updateTrack(fake));
    }

    @Test
    public void testUpdateTrackArtistAndAlbumUpdateTogether() {
        Track original = new Track(0, "Ma Chanson", "Initial Artist", "Initial Album", "2015", 240, "Jazz",
                "chemin2.mp3", "cover.jpg", "", "");

        dbInsert.insertArtist("Initial Artist");
        dbInsert.insertAlbum("Initial Album", "Initial Artist");
        dbInsert.insertTag("Jazz");
        dbInsert.insertTrack(original);

        Track modified = new Track(0, "Ma Chanson", "Updated Artist", "Updated Album", "2015", 240, "Jazz",
                "chemin2.mp3", "cover.jpg", "", "");

        assertTrue(dbUpdate.updateTrack(modified));
    }

    @Test
    public void testUpdateTrackWithTagChange() {
        Track original = new Track(0, "Tag Test", "Tag Artist", "Tag Album", "2022", 200, "Electro",
                "tagtest.mp3", "cover.jpg", "", "");

        dbInsert.insertArtist("Tag Artist");
        dbInsert.insertAlbum("Tag Album", "Tag Artist");
        dbInsert.insertTag("Electro");
        dbInsert.insertTrack(original);

        Track modified = new Track(0, "Tag Test", "Tag Artist", "Tag Album", "2022", 200, "Hip-Hop",
                "tagtest.mp3", "cover.jpg", "", "");

        assertTrue(dbUpdate.updateTrack(modified));
    }

    @Test
    public void testUpdateTrackWithOnlyTrackInfoChanges() {
        Track original = new Track(0, "Old Title", "Artist", "Album", "2019", 180, "Rock",
                "onlytrack.mp3", "old_cover.jpg", "", "");

        dbInsert.insertArtist("Artist");
        dbInsert.insertAlbum("Album", "Artist");
        dbInsert.insertTag("Rock");
        dbInsert.insertTrack(original);

        Track modified = new Track(0, "New Title", "Artist", "Album", "2023", 240, "Rock",
                "onlytrack.mp3", "new_cover.jpg", "", "");

        assertTrue(dbUpdate.updateTrack(modified));
    }

    @Test
    public void testUpdateTrackWithOnlyTagChanges() {
        Track original = new Track(0, "Solo Tag", "Same Artist", "Same Album", "2021", 220, "Electronic",
                "tagchange.mp3", "cover.jpg", "", "");

        dbInsert.insertArtist("Same Artist");
        dbInsert.insertAlbum("Same Album", "Same Artist");
        dbInsert.insertTag("Electronic");
        dbInsert.insertTrack(original);

        Track modified = new Track(0, "Solo Tag", "Same Artist", "Same Album", "2021", 220, "Funk",
                "tagchange.mp3", "cover.jpg", "", "");

        assertTrue(dbUpdate.updateTrack(modified));
    }
}
