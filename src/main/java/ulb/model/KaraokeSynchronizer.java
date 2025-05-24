package ulb.model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import ulb.model.handbleError.LyricsLoadException;

public class KaraokeSynchronizer {
    private final List<LyricsLine> lyricsLines = new ArrayList<>();

    public static class LyricsLine {
        long timestamp;
        String line;

        public LyricsLine(long timestamp, String line) {
            this.timestamp = timestamp;
            this.line = line;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getLine() {
            return line;
        }
    }

    public KaraokeSynchronizer(String filePath) throws LyricsLoadException{
        try {
            if (filePath.endsWith(".lrc")) {
                safeReadLrc(filePath);
            } else if (filePath.endsWith(".txt")) {
                safeReadTxt(filePath);
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + filePath);
            }
        } catch (Exception e) {
            throw new LyricsLoadException("Erreur lors du chargement des paroles : " + filePath, e);
        }
    }

    private void safeReadLrc(String filePath) throws LyricsLoadException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)");

            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                // Vérification si la ligne commence par un horodatage
                if (matcher.matches()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    int milliseconds = Integer.parseInt(matcher.group(3)) * 10;
                    long timestamp = (minutes * 60 + seconds) * 1000L + milliseconds;
                    String lyrics = matcher.group(4).trim();

                    lyricsLines.add(new LyricsLine(timestamp, lyrics));
                }
            }
            // reader.close() is handled by try-with-resources
        } catch (FileNotFoundException e) {
            throw new LyricsLoadException("Fichier introuvable : " + filePath, e);
        } catch (MalformedInputException e) {
            throw new LyricsLoadException("Le fichier contient des caractères mal encodés : " + filePath, e);
        } catch (IOException e) {
            throw new LyricsLoadException("Erreur lors de la lecture du fichier LRC : " + filePath, e);
        } catch (NumberFormatException e) { // Catch potential parsing errors for timestamp
            throw new LyricsLoadException("Format de timestamp invalide dans le fichier LRC : " + filePath, e);
        }
    }

    private void safeReadTxt(String filePath) throws LyricsLoadException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ajouter chaque ligne comme une LyricsLine avec un timestamp de 0
                lyricsLines.add(new LyricsLine(0, line.trim()));
            }
            // reader.close() is handled by try-with-resources
        } catch (IOException e) {
            throw new LyricsLoadException("Erreur lors de la lecture du fichier TXT : " + filePath, e);
        }
    }

    public List<LyricsLine> getLyricsLines() {
        return lyricsLines;
    }
}
