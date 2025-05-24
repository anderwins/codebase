package ulb.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import ulb.model.handbleError.LyricsLoadException;


public class KaraokeSynchronizerTest {
    private File lrcFile;


    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException{
        lrcFile = tempDir.resolve("test.lrc").toFile();
        try (FileWriter writer = new FileWriter(lrcFile)) {
            writer.write("[00:10.50]Hello world\n");
            writer.write("[00:20.30]This is a test\n");
        }
    }

    @Test
    public void testLyricsAreParsedCorrrectly() throws LyricsLoadException{ 
        KaraokeSynchronizer synchronizer = new KaraokeSynchronizer(lrcFile.getAbsolutePath());
        List<KaraokeSynchronizer.LyricsLine> lyricsLines = synchronizer.getLyricsLines();
        
        assertEquals(2, lyricsLines.size());
        
        assertEquals(10500, lyricsLines.get(0).getTimestamp());
        assertEquals("Hello world", lyricsLines.get(0).getLine());
        
        assertEquals(20300, lyricsLines.get(1).getTimestamp());
        assertEquals("This is a test", lyricsLines.get(1).getLine());
    }

    @Test
    public void testEmptyFile() throws IOException, LyricsLoadException{
        File emptyFile = File.createTempFile("empty", ".lrc");
        KaraokeSynchronizer synchronizer = new KaraokeSynchronizer(emptyFile.getAbsolutePath());
        assertTrue(synchronizer.getLyricsLines().isEmpty());
    }

    @Test
    public void testInvalidFormat() throws IOException, LyricsLoadException {
        File invalidFile = File.createTempFile("invalid", ".lrc");
        try (FileWriter writer = new FileWriter(invalidFile)) {
            writer.write("Invalid line without timestamp\n");
        }
        KaraokeSynchronizer synchronizer = new KaraokeSynchronizer(invalidFile.getAbsolutePath());
        assertTrue(synchronizer.getLyricsLines().isEmpty());
    }
}
