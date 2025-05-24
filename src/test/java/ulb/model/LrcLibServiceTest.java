package ulb.model;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
// Removed unused ArgumentCaptor imports if not used
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*; // Use static imports for Mockito methods

import ulb.model.handbleError.LyricsDownloadException;
import ulb.model.handbleError.LyricsParsingException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
// Removed unused URLEncoder import
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LrcLibService.
 * Assumes LrcLibService has been refactored with protected `openConnection`.
 * IMPORTANT: Requires the ulb.model.Track class to function correctly,
 * especially ensuring getKaraokePath() returns null initially and isn't
 * mistakenly assigned the composer value.
 */
class LrcLibServiceTest {

    // Temporarily disable logging from the service during tests to avoid console spam
    private static final Logger serviceLogger = Logger.getLogger(LrcLibService.class.getName());
    private Level originalLogLevel;

    @BeforeEach
    void disableServiceLogging() {
        originalLogLevel = serviceLogger.getLevel();
         serviceLogger.setLevel(Level.OFF);
    }

    @AfterEach
    void restoreServiceLogging() {
        // Restore only if it was set
        if (originalLogLevel != null) {
            serviceLogger.setLevel(originalLogLevel);
        }
    }


    @TempDir
    Path tempDir; // JUnit 5 temporary directory

    private LrcLibService lrcLibService; // Instance under test (will be the anonymous subclass)
    private Track testTrack;
    private String lyricsDirPath;

    // --- Mocks ---
    @Mock
    private HttpURLConnection mockConnection;
    @Mock
    private InputStream mockInputStream;
    @Mock
    private InputStream mockErrorStream;

    private AutoCloseable mockitoCloseable; // For closing mocks

    @BeforeEach
    void setUp() throws IOException {
        mockitoCloseable = MockitoAnnotations.openMocks(this); // Initialize mocks
        lyricsDirPath = tempDir.toAbsolutePath().toString();

        // --- Instantiate LrcLibService using an Anonymous Subclass ---
        // This injects the mock connection via the overridden method.
        lrcLibService = new LrcLibService(lyricsDirPath) {
            @Override
            protected HttpURLConnection openConnection(URL url) throws IOException {
                 // Add a debug print if you suspect this isn't being called:
                 // System.out.println("DEBUG: Test using overridden openConnection for URL: " + url);
                if (mockConnection == null) {
                    throw new IllegalStateException("Mock connection not initialized in test setup!");
                }
                return mockConnection;
            }
        };
        // --- End of Anonymous Subclass ---


        // --- Create Test Track ---
        // --- CORRECTED Test Track Creation in LrcLibServiceTest.setUp() ---
        testTrack = new Track(
            "Test Title",         // title (String)
            "Test Artist",        // artist (String)
            "Test Album",         // album (String)
            "2023",               // year (String) - Use null or an appropriate value
            180,                  // duration (int)
            "Pop",                // genre (String) - Use null or an appropriate value
            "/fake/path.mp3",     // filePath (String)
            null,                 // coverPath (String) - Provide null if not needed
            null,                 // lyricsPath (String) - Provide null if not needed
            null                  // karaokePath (String) - **CRITICAL: Start with null**
        );
        // Explicitly assert initial state if needed (depends on Track constructor)
        // assertNull(testTrack.getKaraokePath(), "Initial karaoke path should be null");


        // --- Default Mock Setup ---
        // Ensure streams are available even if not overridden in specific tests
        when(mockConnection.getInputStream()).thenReturn(mockInputStream);
        when(mockConnection.getErrorStream()).thenReturn(mockErrorStream);
        // Mock disconnect to allow verification
        doNothing().when(mockConnection).disconnect();
        // Default response code set in individual tests where needed
    }

    @AfterEach
    void tearDown() throws Exception {
        // Close Mockito resources
        if (mockitoCloseable != null) {
            mockitoCloseable.close();
        }
    }

    // --- Test Cases ---

    @Test
    void searchAndSaveLyrics_Success() throws Exception {
        // Arrange
        String expectedLyrics = "[00:10.00]Line 1\n[00:15.50]Line 2";
        // Simulate a valid JSON response from the API
        JSONObject jsonResponse = new JSONObject().put("syncedLyrics", expectedLyrics);
        String jsonResponseString = jsonResponse.toString();
        // Provide the valid JSON via the mocked input stream
        mockInputStream = new ByteArrayInputStream(jsonResponseString.getBytes(StandardCharsets.UTF_8));

        // Set up the mock connection for this specific test case
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(mockInputStream); // Return the stream with lyrics

        // Act
        boolean result = lrcLibService.searchAndSaveLyrics(testTrack);

        // Assert
        assertTrue(result, "Should return true when lyrics are found and saved.");

        // Verify the file was created with the sanitized name
        String sanitizedTitle = lrcLibService.sanitizeFilename(testTrack.getTitle());
        Path expectedFilePath = Paths.get(lyricsDirPath, sanitizedTitle + ".lrc");

        assertTrue(Files.exists(expectedFilePath), "LRC file should be created with sanitized name: " + expectedFilePath.getFileName());
        // Verify file content
        String fileContent = Files.readString(expectedFilePath, StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("[ar: Test Artist]"), "File should contain artist metadata.");
        assertTrue(fileContent.contains("[al: Test Album]"), "File should contain album metadata.");
        assertTrue(fileContent.contains("[ti: Test Title]"), "File should contain non-sanitized title metadata.");
        assertTrue(fileContent.contains("[length: 03:00]"), "File should contain formatted duration.");
        assertTrue(fileContent.contains(expectedLyrics), "File should contain the synced lyrics."); // Check contains instead of endsWith if needed
        // Verify the track object's path was updated
        assertEquals(expectedFilePath.toString(), testTrack.getKaraokePath(), "Track karaoke path should be updated.");

        // Verify expected interactions with the mock connection
        verify(mockConnection).setRequestMethod("GET");
        verify(mockConnection).setRequestProperty(eq("Lrclib-Client"), anyString());
        verify(mockConnection).getResponseCode();
        verify(mockConnection).getInputStream();
        verify(mockConnection, never()).getErrorStream(); // Should not read error stream on 200 OK
        verify(mockConnection).disconnect(); // Should be called in finally block
    }

    @Test
    void searchAndSaveLyrics_ApiSuccess_NoSyncedLyrics() throws Exception {
        // Arrange: API returns OK, but JSON has null syncedLyrics
        JSONObject jsonResponse = new JSONObject().put("syncedLyrics", JSONObject.NULL); // Use JSONObject.NULL for explicit null
        String jsonResponseString = jsonResponse.toString();
        mockInputStream = new ByteArrayInputStream(jsonResponseString.getBytes(StandardCharsets.UTF_8));

        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(mockInputStream);

        // Act
        boolean result = lrcLibService.searchAndSaveLyrics(testTrack);

        // Assert
        assertFalse(result, "Should return false when syncedLyrics is null.");
        // Verify no file was created
        String sanitizedTitle = lrcLibService.sanitizeFilename(testTrack.getTitle());
        Path nonExistentFilePath = Paths.get(lyricsDirPath, sanitizedTitle + ".lrc");
        assertFalse(Files.exists(nonExistentFilePath), "LRC file should not be created.");
        // *** CRITICAL ASSERTION DEPENDING ON YOUR Track CLASS ***
        assertNull(testTrack.getKaraokePath(), "Track karaoke path should remain null (Check Track class if this fails).");

        // Verify interactions
        verify(mockConnection).getResponseCode();
        verify(mockConnection).getInputStream();
        verify(mockConnection).disconnect();
    }

     @Test
    void searchAndSaveLyrics_ApiSuccess_EmptySyncedLyrics() throws Exception {
        // Arrange: API returns OK, but JSON has empty string for syncedLyrics
        JSONObject jsonResponse = new JSONObject().put("syncedLyrics", "");
        String jsonResponseString = jsonResponse.toString();
        mockInputStream = new ByteArrayInputStream(jsonResponseString.getBytes(StandardCharsets.UTF_8));

        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(mockInputStream);

        // Act
        boolean result = lrcLibService.searchAndSaveLyrics(testTrack);

        // Assert
        assertFalse(result, "Should return false when syncedLyrics is empty.");
        // Verify no file was created
        String sanitizedTitle = lrcLibService.sanitizeFilename(testTrack.getTitle());
        Path nonExistentFilePath = Paths.get(lyricsDirPath, sanitizedTitle + ".lrc");
        assertFalse(Files.exists(nonExistentFilePath), "LRC file should not be created.");
        // *** CRITICAL ASSERTION DEPENDING ON YOUR Track CLASS ***
        assertNull(testTrack.getKaraokePath(), "Track karaoke path should remain null (Check Track class if this fails).");

        // Verify interactions
        verify(mockConnection).getResponseCode();
        verify(mockConnection).getInputStream();
        verify(mockConnection).disconnect();
    }

    @Test
    void searchAndSaveLyrics_NotFound404() throws Exception {
        // Arrange: Mock connection to return 404
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);

        // Act
        boolean result = lrcLibService.searchAndSaveLyrics(testTrack);

        // Assert
        assertFalse(result, "Should return false on HTTP 404 Not Found.");
        // Verify no file was created
        String sanitizedTitle = lrcLibService.sanitizeFilename(testTrack.getTitle());
        Path nonExistentFilePath = Paths.get(lyricsDirPath, sanitizedTitle + ".lrc");
        assertFalse(Files.exists(nonExistentFilePath), "LRC file should not be created on 404.");
        // *** CRITICAL ASSERTION DEPENDING ON YOUR Track CLASS ***
        assertNull(testTrack.getKaraokePath(), "Track karaoke path should remain null (Check Track class if this fails).");

        // Verify interactions
        verify(mockConnection).getResponseCode();
        verify(mockConnection, never()).getInputStream(); // Should not try to get input stream on 404
        // getErrorStream might be called depending on implementation, let's verify it is handled
        verify(mockConnection, atMostOnce()).getErrorStream(); // Allow it to be called 0 or 1 times
        verify(mockConnection).disconnect();
    }

    @Test
    void searchAndSaveLyrics_HttpError500() throws Exception {
        // Arrange: Mock connection returns 500 and provides an error stream
        String errorResponse = "{\"error\":\"Server exploded\"}"; // Example error JSON
        mockErrorStream = new ByteArrayInputStream(errorResponse.getBytes(StandardCharsets.UTF_8));

        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(mockConnection.getErrorStream()).thenReturn(mockErrorStream); // Provide the error stream

        // Act
        boolean result = lrcLibService.searchAndSaveLyrics(testTrack);

        // Assert
        assertFalse(result, "Should return false on HTTP 500 Internal Server Error.");
        // Verify no file created
        String sanitizedTitle = lrcLibService.sanitizeFilename(testTrack.getTitle());
        Path nonExistentFilePath = Paths.get(lyricsDirPath, sanitizedTitle + ".lrc");
        assertFalse(Files.exists(nonExistentFilePath), "LRC file should not be created on 500.");
        // *** CRITICAL ASSERTION DEPENDING ON YOUR Track CLASS ***
        assertNull(testTrack.getKaraokePath(), "Track karaoke path should remain null (Check Track class if this fails).");

        // Verify interactions
        verify(mockConnection).getResponseCode();
        verify(mockConnection).getErrorStream(); // Should attempt to read error stream
        verify(mockConnection).disconnect();
    }

    @Test
    void searchAndSaveLyrics_NetworkIOException_OnConnect() throws Exception {
        // Arrange: Simulate IOException when getting response code (after connect)
        IOException ioException = new IOException("Connection timed out");
        when(mockConnection.getResponseCode()).thenThrow(ioException);

        // Act & Assert
        LyricsDownloadException thrown = assertThrows(LyricsDownloadException.class, () -> {
            lrcLibService.searchAndSaveLyrics(testTrack);
        }, "Should throw LyricsDownloadException on network I/O error during connection phase.");

        assertEquals("Erreur réseau lors de la recherche LrcLib", thrown.getMessage());
        assertSame(ioException, thrown.getCause(), "Original IOException should be the cause.");

        // Verify disconnect is still called by finally block
        verify(mockConnection).disconnect();
    }

     @Test
    void searchAndSaveLyrics_NetworkIOException_OnRead() throws Exception {
        // Arrange: Simulate IOException while reading the input stream
        IOException ioException = new IOException("Error reading response stream");
        // Need an InputStream mock that actually throws when read is called
        InputStream throwingInputStream = mock(InputStream.class);
        when(throwingInputStream.read()).thenThrow(ioException);
        when(throwingInputStream.read(any(byte[].class))).thenThrow(ioException);
        when(throwingInputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(ioException);

        // Setup connection to return OK, but provide the throwing stream
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(throwingInputStream);

        // Act & Assert
        LyricsDownloadException thrown = assertThrows(LyricsDownloadException.class, () -> {
            lrcLibService.searchAndSaveLyrics(testTrack);
        }, "Should throw LyricsDownloadException on read error.");

        // Verify the exception message and cause
        assertEquals("Erreur réseau lors de la recherche LrcLib", thrown.getMessage());
        assertNotNull(thrown.getCause(), "Cause should not be null.");
        // The direct cause might be the IOException thrown by the stream,
        // or potentially one wrapped by BufferedReader - check for IOException type.
        assertTrue(thrown.getCause() instanceof IOException, "Cause should be an IOException.");

        // Verify disconnect is still called by finally block
        verify(mockConnection).disconnect();
    }

    @Test
    void searchAndSaveLyrics_JsonParsingError() throws Exception {
        // Arrange: Provide invalid JSON data in the stream
        String invalidJsonResponse = "This is not valid JSON";
        mockInputStream = new ByteArrayInputStream(invalidJsonResponse.getBytes(StandardCharsets.UTF_8));

        // Setup connection to return OK and provide the bad stream
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(mockInputStream);

        // Act & Assert: Expect LyricsParsingException to be thrown
        LyricsParsingException thrown = assertThrows(LyricsParsingException.class, () -> {
            lrcLibService.searchAndSaveLyrics(testTrack);
        }, "Should throw LyricsParsingException on invalid JSON response.");

        // Verify the exception details
        assertEquals("Réponse JSON LrcLib invalide", thrown.getMessage());
        assertNotNull(thrown.getCause(), "Cause should not be null.");
        assertTrue(thrown.getCause() instanceof org.json.JSONException, "Cause should be JSONException.");

        // Verify disconnect is still called by finally block
        verify(mockConnection).disconnect();
    }

     @Test
    void searchAndSaveLyrics_EmptyResponse() throws Exception {
        // Arrange: API returns OK, but the response body is empty
        String emptyResponse = "";
        mockInputStream = new ByteArrayInputStream(emptyResponse.getBytes(StandardCharsets.UTF_8));

        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(mockInputStream); // Provide stream with empty response

        // Act
        boolean result = lrcLibService.searchAndSaveLyrics(testTrack);

        // Assert
        assertFalse(result, "Should return false for an empty API response.");
        // Verify no file created
        String sanitizedTitle = lrcLibService.sanitizeFilename(testTrack.getTitle());
        Path nonExistentFilePath = Paths.get(lyricsDirPath, sanitizedTitle + ".lrc");
        assertFalse(Files.exists(nonExistentFilePath), "LRC file should not be created for empty response.");
        // *** CRITICAL ASSERTION DEPENDING ON YOUR Track CLASS ***
        assertNull(testTrack.getKaraokePath(), "Track karaoke path should remain null (Check Track class if this fails).");

        // Verify interactions
        verify(mockConnection).getResponseCode();
        verify(mockConnection).getInputStream();
        verify(mockConnection).disconnect();
    }

    @Test
    void searchAndSaveLyrics_UrlEncodingAndFileSanitization() throws Exception {
         // Arrange: Track with characters needing URL encoding and filename sanitization
        Track trackWithSpecialChars = new Track(
            "Title / Track",      // title
            "Artist & Co",        // artist
            "Album+Name=?",       // album
            "2024",               // year - Example value
            200,                  // duration
            null,                 // genre
            "/path",              // filePath
            null,                 // coverPath
            null,                 // lyricsPath
            null                  // karaokePath - Start with null
        );

        // Use the sanitize method from the service instance to predict filename
        String sanitizedTitle = lrcLibService.sanitizeFilename(trackWithSpecialChars.getTitle());
        assertEquals("Title _ Track", sanitizedTitle, "Filename should be sanitized correctly.");
        Path expectedFilePath = Paths.get(lyricsDirPath, sanitizedTitle + ".lrc");

        // Mocking for a successful response
        String lyricsContent = "[00:01.00]Special Lyrics";
        JSONObject jsonResponse = new JSONObject().put("syncedLyrics", lyricsContent);
        mockInputStream = new ByteArrayInputStream(jsonResponse.toString().getBytes(StandardCharsets.UTF_8));
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(mockInputStream);

        // Act
        boolean result = lrcLibService.searchAndSaveLyrics(trackWithSpecialChars);

        // Assert
        assertTrue(result, "Result should be true for URL encoding and sanitization test.");

        // Verify file exists with sanitized name
        assertTrue(Files.exists(expectedFilePath), "File with SANITIZED name '" + expectedFilePath.getFileName() + "' should be created.");
        // Verify file content uses ORIGINAL (non-sanitized) metadata from Track object
        String fileContent = Files.readString(expectedFilePath, StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("[ti: Title / Track]"), "Metadata should contain original title.");
        assertTrue(fileContent.contains("[al: Album+Name=?]"), "Metadata should contain original album name.");
        assertTrue(fileContent.contains("[ar: Artist & Co]"), "Metadata should contain original artist name.");
        assertTrue(fileContent.contains(lyricsContent), "File content should contain the lyrics.");
        // Verify track path update
        assertEquals(expectedFilePath.toString(), trackWithSpecialChars.getKaraokePath(), "Track karaoke path should be updated.");

        // Verify connection disconnected
        verify(mockConnection).disconnect();
        // Optionally: Capture the URL used in openConnection to verify encoding,
        // but this requires modifying the anonymous class setup.
    }
}