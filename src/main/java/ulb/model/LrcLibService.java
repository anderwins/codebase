package ulb.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException; // Keep for URL constructor
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

import ulb.model.handbleError.LyricsDownloadException;
import ulb.model.handbleError.LyricsParsingException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour rechercher et télécharger des fichiers LRC depuis l'API
 * lrclib.net.
 * Cette classe permet de rechercher des paroles synchronisées pour les morceaux
 * qui n'en ont pas localement.
 */
public class LrcLibService {
    private static final String API_BASE_URL = "https://lrclib.net/api/get";
    private final String lyricsDirectory;
    private static final Logger logger = Logger.getLogger(LrcLibService.class.getName());

    /**
     * Constructeur du service LrcLib.
     *
     * @param lyricsDirectory Le répertoire où les fichiers LRC seront enregistrés
     */
    public LrcLibService(String lyricsDirectory) {
        if (lyricsDirectory == null || lyricsDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Lyrics directory cannot be null or empty.");
        }
        this.lyricsDirectory = lyricsDirectory;
        File dir = new File(lyricsDirectory);
        if (!dir.exists()) {
            // If the directory doesn't exist, try to create it.
            if (!dir.mkdirs()) {
                logger.log(Level.SEVERE, "Could not create lyrics directory: {0}", lyricsDirectory);
                throw new IllegalArgumentException("Could not create lyrics directory: " + lyricsDirectory);
            }
            logger.log(Level.INFO, "Lyrics directory created: {0}", lyricsDirectory);
        } else if (!dir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Lyrics directory path exists but is not a directory: " + lyricsDirectory);
        } 
        // Check writability after ensuring it exists and is a directory
        if (!dir.canWrite()) {
            logger.log(Level.SEVERE, "Cannot write to lyrics directory: {0}", lyricsDirectory);
            throw new IllegalArgumentException("Cannot write to lyrics directory: " + lyricsDirectory);
        }
    }

    /**
     * Ouvre une connexion HTTP vers l'URL spécifiée.
     * Protected pour permettre le remplacement dans les tests (mocking).
     *
     * @param url L'URL à laquelle se connecter.
     * @return La connexion HTTP établie.
     * @throws IOException Si une erreur d'I/O se produit lors de l'ouverture de la
     *                     connexion.
     */
    protected HttpURLConnection openConnection(URL url) throws IOException {
        if (url == null) {
            throw new IOException("Cannot open connection to a null URL.");
        }
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Sanitizes a string to be used as a filename by removing or replacing invalid
     * characters.
     * Handles null input.
     *
     * @param name The original name derived from track data.
     * @return A sanitized string suitable for use as a filename, or "Unknown" if
     *         input is null/empty.
     */
    protected String sanitizeFilename(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Unknown";
        }
        // Remove characters invalid in Windows/Unix filenames
        // Replaces: \ / : * ? " < > |
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        // Additionally, trim leading/trailing whitespace and replace consecutive
        // underscores
        sanitized = sanitized.trim().replaceAll("_+", "_");
        if (sanitized.isEmpty()) {
            return "Unknown"; // Handle cases where name consists only of invalid chars
        }
        return sanitized;
    }

    /**
     * Recherche des paroles synchronisées pour un morceau.
     *
     * @param track Le morceau pour lequel rechercher des paroles. Must not be null.
     * @return true si des paroles ont été trouvées et enregistrées, false sinon
     * @throws LyricsDownloadException Si une erreur réseau ou d'écriture de fichier
     *                                 survient.
     * @throws LyricsParsingException  Si une erreur survient lors de l'analyse de
     *                                 la réponse JSON.
     * @throws NullPointerException    if track is null.
     */
    public boolean searchAndSaveLyrics(Track track) throws LyricsDownloadException, LyricsParsingException {
        if (track == null) {
            throw new NullPointerException("Track cannot be null.");
        }
        if (track.getArtist() == null || track.getTitle() == null) {
            logger.warning("Track artist or title is null, cannot search for lyrics.");
            return false;
        }

        HttpURLConnection connection = null;
        URL url = null; // Define URL outside try for logging in finally block
        try {
            // Encode parameters
            String artist = URLEncoder.encode(track.getArtist(), StandardCharsets.UTF_8.toString());
            String title = URLEncoder.encode(track.getTitle(), StandardCharsets.UTF_8.toString());
            String album = (track.getAlbum() != null && !track.getAlbum().trim().isEmpty())
                    ? URLEncoder.encode(track.getAlbum().trim(), StandardCharsets.UTF_8.toString())
                    : "";

            // Construct URL
            StringBuilder urlBuilder = new StringBuilder(API_BASE_URL);
            urlBuilder.append("?artist_name=").append(artist);
            urlBuilder.append("&track_name=").append(title);
            if (!album.isEmpty()) {
                urlBuilder.append("&album_name=").append(album);
            }
            if (track.getDuration() > 0) {
                urlBuilder.append("&duration=").append(track.getDuration());
            }

            String urlStr = urlBuilder.toString();
            logger.log(Level.FINE, "Recherche LrcLib URL: {0}", urlStr);
            url = new URL(urlStr); // Assign url here

            // Use the overridable method to get the connection
            connection = openConnection(url);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000); // 15 seconds
            connection.setReadTimeout(15000); // 15 seconds
            connection.setRequestProperty("Lrclib-Client", "ULB Music Player v1.0");

            int responseCode = connection.getResponseCode();
            logger.log(Level.FINE, "LrcLib Response Code: {0}", responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (InputStream inputStream = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n"); // Append newline for multi-line lyrics/JSON
                    }
                }

                String responseStr = response.toString().trim(); // Trim whitespace
                if (responseStr.isEmpty()) {
                    logger.info("Réponse vide de l'API LrcLib.");
                    return false;
                }

                // --- Inner try-catch SPECIFICALLY for JSONException ---
                try {
                    JSONObject result = new JSONObject(responseStr);
                    String syncedLyrics = result.optString("syncedLyrics", null);

                    if (syncedLyrics != null && !syncedLyrics.isEmpty()) {
                        String sanitizedTitle = sanitizeFilename(track.getTitle());
                        String fileName = sanitizedTitle + ".lrc";
                        File targetFile = new File(lyricsDirectory, fileName); // Use File constructor for clarity

                        try (BufferedWriter writer = new BufferedWriter(
                                new FileWriter(targetFile, StandardCharsets.UTF_8))) {
                            writeMetadata(writer, track);
                            writer.write(syncedLyrics); // Write the synced lyrics

                            logger.log(Level.INFO, "Paroles enregistrées dans : {0}", targetFile.getAbsolutePath());
                            track.setKaraokePath(targetFile.getAbsolutePath());
                            return true; // Success!
                        } catch (IOException e) {
                            // Catch file writing errors specifically
                            logger.log(Level.SEVERE,
                                    "Erreur lors de l'écriture du fichier LRC: " + targetFile.getAbsolutePath(), e);
                            throw new LyricsDownloadException(
                                    "Erreur lors de l'écriture du fichier LRC: " + e.getMessage(), e);
                        }
                    } else {
                        logger.info("Paroles synchronisées non disponibles dans la réponse LrcLib.");
                        return false;
                    }
                } catch (JSONException e) {
                    // This block specifically handles JSON errors and throws the correct custom
                    // exception
                    logger.log(Level.WARNING, "Erreur de parsing JSON LrcLib: {0}. Réponse: {1}",
                            new Object[] { e.getMessage(),
                                    responseStr.substring(0, Math.min(responseStr.length(), 500)) });
                    throw new LyricsParsingException("Réponse JSON LrcLib invalide", e);
                }
                // --- End of inner try-catch for JSONException ---

            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                logger.log(Level.INFO, "Morceau non trouvé sur LrcLib (404): {0} - {1}",
                        new Object[] { track.getArtist(), track.getTitle() });
                return false;
            } else {
                // Handle other non-OK HTTP errors
                logger.log(Level.WARNING, "Erreur HTTP LrcLib: {0}", responseCode);
                logErrorStream(connection); // Log details from error stream
                return false; // Return false for other HTTP errors
            }

        } catch (MalformedURLException e) {
            // This indicates a programming error in URL construction
            logger.log(Level.SEVERE, "URL LrcLib mal formée: " + e.getMessage(), e);
            // Throw a runtime exception as this shouldn't happen with proper encoding
            throw new RuntimeException("URL LrcLib mal formée", e);
        } catch (IOException e) {
            // Catch general network/IO errors during connection or reading response
            logger.log(Level.SEVERE, "Erreur réseau lors de la recherche LrcLib: " + e.getMessage(), e);
            throw new LyricsDownloadException("Erreur réseau lors de la recherche LrcLib", e);
        } finally {
            // Ensure the connection is disconnected regardless of outcome
            if (connection != null) {
                connection.disconnect();
                logger.log(Level.FINEST, "LrcLib connection disconnected.");
            }
        }
        // This point should ideally not be reached if all paths return/throw,
        // but added as a safeguard. It indicates no lyrics found/saved.
        // return false;
    }

    /** Helper to log error stream content */
    private void logErrorStream(HttpURLConnection connection) {
        if (connection == null)
            return;
        try (InputStream errorStream = connection.getErrorStream()) {
            if (errorStream != null) {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line).append("\n");
                    }
                }
                logger.log(Level.WARNING, "Message d'erreur LrcLib du serveur: {0}", errorResponse.toString().trim());
            } else {
                logger.log(Level.WARNING, "Aucun message d'erreur LrcLib fourni par le serveur.");
            }
        } catch (Exception e) {
            // Gestion spécifique des erreurs liées aux entrées/sorties (I/O)
            if (e instanceof IOException) {
                logger.log(Level.WARNING, "Erreur I/O lors du traitement du flux d'erreur LrcLib", e);
            }
            // Gestion de toute autre exception inattendue (NullPointerException,
            // IllegalStateException, etc.)
            else {
                logger.log(Level.WARNING, "Erreur inattendue lors du traitement du flux d'erreur LrcLib", e);
            }
        }
    }

    /**
     * Écrit les métadonnées dans le fichier LRC. Adds basic null checks.
     */
    private void writeMetadata(BufferedWriter writer, Track track) throws IOException {
        writer.write("[ar: " + (track.getArtist() != null ? track.getArtist() : "Unknown Artist") + "]\n");
        writer.write("[al: " + (track.getAlbum() != null ? track.getAlbum() : "Unknown Album") + "]\n");
        writer.write("[ti: " + (track.getTitle() != null ? track.getTitle() : "Unknown Title") + "]\n");
        writer.write("[length: " + formatDuration(track.getDuration()) + "]\n");
    }

    /**
     * Formate la durée en secondes au format MM:SS. Handles negative input.
     */
    private String formatDuration(int durationInSeconds) {
        if (durationInSeconds < 0)
            durationInSeconds = 0;
        int minutes = durationInSeconds / 60;
        int seconds = durationInSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}