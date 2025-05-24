package ulb.model;

import org.jaudiotagger.audio.AudioFileIO;
import org.junit.jupiter.api.*;
import javafx.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataManagerTest {

    private MetadataManager metadataManager;

    @BeforeEach
    void setUp() {
        metadataManager = new MetadataManager();
    }

    @Test
    void testExtractMetadata_FileWithNoMetadata_ShouldReturnNull() {
        File fakeFile = new File("src/main/resources/musiques/fake.mp3");
        MetadataManager manager = new MetadataManager();
        Optional<Track> track = manager.extractMetadata(fakeFile);

        // Basically, we expect the metadata extraction to fail and return an empty Optional
        assertTrue(track.isEmpty(), "Le fichier invalide devrait retourner un Optional vide");
    }

    @Test
    void testUpdateMetadata_NonExistentFile_ShouldReturnFalse() {
        Track track = new Track(
                "Titre", "Artiste", "Album", "2024", 180,
                "Pop", "nonexistent.mp3", null, null, null
        );
        boolean result = metadataManager.updateMetadata(track);
        assertFalse(result, "La mise à jour doit échouer pour un fichier inexistant.");
    }

    @Test
    void testCreateLyricsAndKaraokeFiles_InvalidWavPath_ShouldReturnNull() {
        Pair<String, String> result = metadataManager.createLyricsAndKaraokeFiles("invalid_path.wav");
        assertNull(result, "Doit retourner null pour un fichier WAV inexistant.");
    }

    @Test
    void testCreateLyricsAndKaraokeFiles_WrongExtension_ShouldReturnNull() throws Exception {
        // Fichier non .wav
        File file = new File("src/main/resources/musiques/test.mp3");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("not a wav".getBytes());
        }

        Pair<String, String> result = metadataManager.createLyricsAndKaraokeFiles(file.getAbsolutePath());
        assertNull(result, "Doit retourner null pour une mauvaise extension.");

        file.delete();
    }

    @Test
    public void testExtractMetadataIOException() throws IOException {
        assertThrows(IOException.class, () -> {
            File mockMp3File = mock(File.class);
            AudioFileIO audioFileIO = mock(AudioFileIO.class);
            when(audioFileIO.read(mockMp3File)).thenThrow(new IOException("Simulated IOException"));

            metadataManager = new MetadataManager();
            metadataManager.extractMetadata(mockMp3File);
        });
    }
}
