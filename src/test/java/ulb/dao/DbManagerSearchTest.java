package ulb.dao;

import org.junit.jupiter.api.*;
import ulb.Config;
import ulb.model.Track;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DbManagerSearchTest {

    private static final String TEST_DB_PATH = "test_search.db";
    private Connection connection;
    private DbManagerInsert inserter;
    private DbManagerSearch searcher;

    @BeforeEach
    public void setup() throws Exception {
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler);
        rootLogger.setLevel(Level.OFF);

        // Supprimer ancien fichier si existant
        File file = new File(TEST_DB_PATH);
        if (file.exists()) file.delete();

        connection = DriverManager.getConnection("jdbc:sqlite:" + TEST_DB_PATH);

        // Création des tables
        SQLLoader loader = new SQLLoader(Config.CREATE_TABLES_SQL_FILE);
        String createSQL = loader.getQuery("createAllTablesAndTriggers");
        connection.createStatement().executeUpdate(createSQL);

        inserter = new DbManagerInsert(connection);
        searcher = new DbManagerSearch(connection);

        // Données de base
        inserter.insertUser("admin");
        inserter.insertArtist("Aya Nakamura");
        inserter.insertAlbum("DNK", "Aya Nakamura");
        inserter.insertTag("Pop");
        Track track = new Track(
            0, "Baby", "Aya Nakamura", "DNK", "2023", 190,
            "Pop", "audio.mp3", "cover.jpg", "lyrics.txt", "karaoke.lrc"
        );
        inserter.insertTrack(track);
        inserter.insertPlaylist("Ma Playlist", "admin");
        inserter.addTrackToPlaylist("Ma Playlist", "Baby");
    }

    @AfterEach
    public void cleanup() throws Exception {
        if (connection != null) connection.close();
        File file = new File(TEST_DB_PATH);
        if (file.exists()) file.delete();
    }

    @Test
    public void testSearchByTitle() {
        var result = searcher.searchTracksByTitle("Ba");
        assertFalse(result.isEmpty(), "La recherche par titre devrait retourner des résultats.");
        assertEquals("Baby", result.get(0).getTitle());
    }

    @Test
    public void testSearchByArtist() {
        var result = searcher.searchTracksByArtist("Aya Nakamura");
        assertFalse(result.isEmpty(), "La recherche par artiste devrait retourner des résultats.");
        assertEquals("Baby", result.get(0).getTitle());
    }

    @Test
    public void testSearchByAlbum() {
        var result = searcher.searchTracksByAlbum("DNK");
        assertFalse(result.isEmpty(), "La recherche par album devrait retourner des résultats.");
        assertEquals("Baby", result.get(0).getTitle());
    }

    @Test
    public void testSearchByTag() {
        var result = searcher.searchTracksByTag("Pop");
        assertFalse(result.isEmpty(), "La recherche par tag devrait retourner des résultats.");
        assertEquals("Baby", result.get(0).getTitle());
    }

    @Test
    public void testGetAllTracks() {
        var result = searcher.getAllTracks();
        assertFalse(result.isEmpty(), "La récupération de tous les morceaux devrait retourner des résultats.");
        assertEquals("Baby", result.get(0).getTitle());
    }

    @Test
    public void testGetAllPlaylistsWithTracks() {
        Map<String, List<Track>> playlists = searcher.getAllPlaylistsWithTracks();
        // Adaptation : utiliser "Ma Playlist" comme clé car `name` est le vrai nom de la colonne
        assertTrue(playlists.containsKey("Ma Playlist"), "La playlist doit être présente dans les résultats.");
        List<Track> tracks = playlists.get("Ma Playlist");
        assertTrue(tracks.size() > 0, "La playlist doit contenir au moins un morceau.");
        assertEquals("Baby", tracks.get(0).getTitle());
    }
}
