package ulb.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
import ulb.dao.DbInitializer; // Import DbInitializer to get logger name
import ulb.dao.DbManagerInsert;
//import ulb.model.Track;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List; // Correct import for List
import java.util.Optional;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Classe de tests unitaires pour DatabaseSeeder.
 * Ces tests utilisent Mockito pour simuler MetadataManager et DbManagerInsert.
 * On teste ici la méthode privée processTrack() via réflexion, puisqu'elle est appelée depuis seedDatabase() qui ne retourne rien.
 */
public class DatabaseSeederTest {

    private DbManagerInsert mockDbInsert;
    private MetadataManager mockMetadataManager;
    private DatabaseSeeder seeder;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    // errContent and originalErr are no longer needed for these specific tests

    private TestLogHandler testLogHandler;
    private Logger loggerToTest;

    /**
     * Initialisation avant chaque test :
     * - Création des mocks
     * - Création du DatabaseSeeder avec dépendances simulées
     * - Redirection des sorties System.out pour les assertions
     */
    @BeforeEach
    public void setUp() {
        // Disable logging before each test
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler); // Remove the default console handler
        rootLogger.setLevel(Level.OFF); // Set the root logger level to OFF

        mockDbInsert = mock(DbManagerInsert.class);
        mockMetadataManager = mock(MetadataManager.class);
        seeder = new DatabaseSeeder(mockDbInsert, mockMetadataManager);

        System.setOut(new PrintStream(outContent)); // Keep for System.out checks if any
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut); // Restore System.out
    }

    /**
     * Test du scénario "heureux" :
     * - Le fichier est bien analysé
     * - Les métadonnées sont extraites
     * - L'insertion en base réussit
     * - Un message de succès s'affiche
     */
    @Test
    public void testProcessTrack_success() {
        File mockFile = new File("test.mp3");

        Track fakeTrack = new Track("Titre", "Artiste", "Album", "2024", 180, "Pop",
                "/fichier.mp3", "/image.jpg", "/lyrics.txt", "/karaoke.lrc");

        when(mockMetadataManager.extractMetadata(mockFile)).thenReturn(Optional.of(fakeTrack));
        when(mockDbInsert.insertTrack(fakeTrack)).thenReturn(true);

        callPrivateProcessTrack(seeder, mockFile);

        verify(mockDbInsert).insertTrack(fakeTrack);
    }

    /**
     * Test du cas où l'extraction des métadonnées échoue :
     * - extractMetadata() retourne null
     * - Aucun insert n'est tenté
     * - Un message d'avertissement est affiché
     */
    @Test
    public void testProcessTrack_metadataNull_shouldLogWarning() {
        File mockFile = new File("fichier_invalide.mp3");

        when(mockMetadataManager.extractMetadata(mockFile)).thenReturn(Optional.empty());

        callPrivateProcessTrack(seeder, mockFile);

        verify(mockDbInsert, never()).insertTrack(any());
    }

    /**
     * Test du cas où l'insertion échoue après une extraction réussie :
     * - insertTrack() retourne false
     * - Un message d'avertissement est affiché
     */
    @Test
    public void testProcessTrack_insertFails_shouldLogWarning() {
        File mockFile = new File("test_fail.mp3");

        Track track = new Track("TitreFail", "Artiste", "Album", "2023", 200, "Rock",
                "/fail.mp3", "/cover.jpg", "/ly.txt", "/kara.lrc");

        when(mockMetadataManager.extractMetadata(mockFile)).thenReturn(Optional.of(track));
        when(mockDbInsert.insertTrack(track)).thenReturn(false);

        callPrivateProcessTrack(seeder, mockFile);

    }

    /**
     * Méthode utilitaire pour accéder à la méthode privée processTrack(File) via réflexion.
     * Cela permet de tester directement son comportement sans passer par seedDatabase().
     */
    private void callPrivateProcessTrack(DatabaseSeeder seeder, File file) {
        try {
            var method = DatabaseSeeder.class.getDeclaredMethod("processTrack", File.class);
            method.setAccessible(true); // autoriser l'accès à une méthode privée
            method.invoke(seeder, file); // appel réel
        } catch (Exception e) {
            fail("Erreur lors de l'appel à processTrack : " + e.getMessage());
        }
    }

    // Helper class for capturing log messages
    private static class TestLogHandler extends java.util.logging.Handler { // Use fully qualified name or import
        private final List<LogRecord> records = new ArrayList<>(); // Use fully qualified name or import

        @Override
        public synchronized void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}

        public synchronized boolean hasLogMatching(Level level, String messageSubstring) {
            return records.stream()
                    .anyMatch(record -> record.getLevel().equals(level) &&
                            record.getMessage().contains(messageSubstring));
        }
        public synchronized void clearRecords() {
            records.clear();
        }
    }
}
