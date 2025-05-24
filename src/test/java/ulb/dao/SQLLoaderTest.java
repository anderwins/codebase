package ulb.dao;

import org.junit.jupiter.api.*;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class SQLLoaderTest {

    private SQLLoader loader;

    @BeforeEach
    public void setup() {
        // Disable logging before each test
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler); // Remove the default console handler
        rootLogger.setLevel(Level.OFF); // Set the root logger level to OFF
        loader = new SQLLoader("sql/search_queries.sql");
    }

    @Test
    public void testQueryIsLoadedCorrectly() {
        String query = loader.getQuery("searchTracksByTitle");
        assertNotNull(query, "La requête 'searchTracksByTitle' doit être chargée.");
        assertTrue(query.contains("SELECT"), "La requête doit contenir un SELECT.");
    }

    @Test
    public void testNonExistentQueryReturnsNull() {
        String query = loader.getQuery("nonExistentTag");
        assertNull(query, "Une requête inexistante doit retourner null.");
    }

    @Test
    public void testMultipleQueriesAreLoaded() {
        assertNotNull(loader.getQuery("searchTrackByArtist"), "La requête 'searchTrackByArtist' doit être chargée.");
        assertNotNull(loader.getQuery("getAllTracks"), "La requête 'getAllTracks' doit être chargée.");
    }

    @Test
    public void testEmptyQueryFileDoesNotCrash() {
        SQLLoader emptyLoader = new SQLLoader("sql/empty.sql");
        assertNull(emptyLoader.getQuery("anything"), "Aucune requête ne doit être chargée depuis un fichier vide.");
    }

    @Test
    public void testMalformedFileIsHandledGracefully() {
        SQLLoader brokenLoader = new SQLLoader("sql/malformed.sql");
        assertNull(brokenLoader.getQuery("someTag"), "Les requêtes mal formées doivent être ignorées ou retournées null.");
    }
}
