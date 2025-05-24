package ulb.dao;

import org.junit.jupiter.api.*;
import ulb.Config;
import ulb.model.Track;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DbManagerInsertTest {

    private static final String TEST_DB_PATH = "test_db_insert.db";
    private Connection connection;
    private DbManagerInsert dbInsert;

    @BeforeEach
    public void setup() throws Exception {

        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler);
        rootLogger.setLevel(Level.OFF);

        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) dbFile.delete();

        connection = DriverManager.getConnection("jdbc:sqlite:" + TEST_DB_PATH);
        SQLLoader loader = new SQLLoader(Config.CREATE_TABLES_SQL_FILE);
        connection.createStatement().executeUpdate(loader.getQuery("createAllTablesAndTriggers"));

        dbInsert = new DbManagerInsert(connection);
    }

    @AfterEach
    public void cleanup() throws Exception {
        if (connection != null) connection.close();
        File file = new File(TEST_DB_PATH);
        if (file.exists()) file.delete();
    }

    @Test
    public void testInsertUser() {
        assertTrue(dbInsert.insertUser("admin"));
        assertFalse(dbInsert.insertUser("admin")); // déjà inséré
    }

    @Test
    public void testInsertArtist() {
        assertTrue(dbInsert.insertArtist("Drake"));
        assertFalse(dbInsert.insertArtist("Drake"));
    }

    @Test
    public void testInsertTag() {
        assertTrue(dbInsert.insertTag("Rap"));
        assertFalse(dbInsert.insertTag("Rap"));
    }

    @Test
    public void testInsertAlbumFailsIfArtistMissing() {
        assertFalse(dbInsert.insertAlbum("Certified Lover Boy", "Drake")); // Drake n'est pas encore inséré
    }

    @Test
    public void testInsertAlbumSuccess() {
        dbInsert.insertArtist("Drake");
        assertTrue(dbInsert.insertAlbum("Certified Lover Boy", "Drake"));
    }

    @Test
    public void testInsertTrack() {
        Track track = new Track(
            "God's Plan", "Drake", "Scorpion", "2018", 210,
            "Hip-Hop", "/tracks/plan.mp3", "/covers/plan.jpg",
            "/lyrics/plan.txt", "/karaoke/plan.lrc"
        );
        assertTrue(dbInsert.insertTrack(track));
        assertFalse(dbInsert.insertTrack(track)); // doublon
    }

    @Test
    public void testInsertPlaylist() {
        dbInsert.insertUser("admin");
        assertTrue(dbInsert.insertPlaylist("My Playlist", "admin"));
        assertFalse(dbInsert.insertPlaylist("My Playlist", "admin"));
    }

    @Test
    public void testAddAndRemoveTrackFromPlaylist() {
        dbInsert.insertUser("admin");
        dbInsert.insertArtist("Drake");
        dbInsert.insertAlbum("Scorpion", "Drake");
        dbInsert.insertTag("Hip-Hop");

        Track track = new Track(
            "Nice For What", "Drake", "Scorpion", "2018", 190,
            "Hip-Hop", "/tracks/nice.mp3", "/covers/nice.jpg",
            "/lyrics/nice.txt", "/karaoke/nice.lrc"
        );
        dbInsert.insertTrack(track);
        dbInsert.insertPlaylist("My Playlist", "admin");

        assertTrue(dbInsert.addTrackToPlaylist("My Playlist", "Nice For What"));
        assertFalse(dbInsert.addTrackToPlaylist("My Playlist", "Nice For What")); // déjà ajouté
        assertTrue(dbInsert.removeTrackFromPlaylist("My Playlist", "Nice For What"));
        assertFalse(dbInsert.removeTrackFromPlaylist("My Playlist", "Nice For What")); // déjà retiré
    }
}
