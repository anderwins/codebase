package ulb.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ulb.controller.MainController;

/**
 * Classe représentant les paroles d'une piste avec des informations de timing
 * pour la fonctionnalité karaoké.
 * Chaque ligne de paroles a un temps de début en millisecondes pour la
 * synchronisation avec la lecture audio.
 */
public class Lyrics {
    private List<LyricLine> lines;
    private String trackTitle;
    private String artist;
    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    public Lyrics() {
        this.lines = new ArrayList<>();
    }

    /**
     * Représente une seule ligne de paroles avec des informations de timing.
     */
    public static class LyricLine {
        private String text;
        private long startTimeMs;

        public LyricLine(String text, long startTimeMs) {
            this.text = text;
            this.startTimeMs = startTimeMs;
        }

        public String getText() {
            return text;
        }

        public long getStartTimeMs() {
            return startTimeMs;
        }
    }

    /**
     * Ajoute une ligne de paroles avec son temps de début.
     * 
     * @param text        Le texte des paroles
     * @param startTimeMs Le temps de début en millisecondes
     */
    public void addLine(String text, long startTimeMs) {
        lines.add(new LyricLine(text, startTimeMs));
    }

    /**
     * Obtient la ligne de paroles actuelle en fonction du temps de lecture actuel.
     * 
     * @param currentTimeMs Temps de lecture actuel en millisecondes
     * @return La ligne de paroles actuelle, ou null si aucune ligne ne correspond
     *         au temps actuel
     */
    public LyricLine getCurrentLine(long currentTimeMs) {
        LyricLine currentLine = null;

        for (LyricLine line : lines) {
            if (line.getStartTimeMs() <= currentTimeMs) {
                currentLine = line;
            } else {
                break;
            }
        }

        return currentLine;
    }

    /**
     * Obtient la prochaine ligne de paroles en fonction du temps de lecture actuel.
     * 
     * @param currentTimeMs Temps de lecture actuel en millisecondes
     * @return La prochaine ligne de paroles, ou null s'il n'y a pas de ligne
     *         suivante
     */
    public LyricLine getNextLine(long currentTimeMs) {
        for (LyricLine line : lines) {
            if (line.getStartTimeMs() > currentTimeMs) {
                return line;
            }
        }

        return null;
    }

    /**
     * Obtient toutes les lignes de paroles.
     * 
     * @return Liste de toutes les lignes de paroles
     */
    public List<LyricLine> getAllLines() {
        return lines;
    }

    public void setTrackTitle(String trackTitle) {
        this.trackTitle = trackTitle;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTrackTitle() {
        return trackTitle;
    }

    public String getArtist() {
        return artist;
    }

    public boolean isValid() {
        return !lines.isEmpty();
    }

    /**
     * Charge les paroles depuis un fichier au format LRC.
     * Format: [MM:SS.xx]Texte des paroles
     * 
     * @param filePath Chemin vers le fichier de paroles
     * @return Objet Lyrics ou null si le chargement échoue
     */
    public static Lyrics loadFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.warning("Lyrics file not found: " + filePath);
            return null;
        }

        Lyrics lyrics = new Lyrics();

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignorer les lignes vides
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Vérifier les métadonnées
                if (line.startsWith("[ti:")) {
                    lyrics.setTrackTitle(line.substring(4, line.length() - 1));
                    continue;
                }
                if (line.startsWith("[ar:")) {
                    lyrics.setArtist(line.substring(4, line.length() - 1));
                    continue;
                }
                if (line.startsWith("[al:")) {
                    // Gérer les métadonnées de l'album
                    // Vous pouvez ajouter une méthode pour définir l'album si nécessaire
                    continue;
                }
                if (line.startsWith("[length:")) {
                    // Gérer les métadonnées de durée
                    // Vous pouvez ajouter une méthode pour définir la durée si nécessaire
                    continue;
                }

                // Analyser les paroles horodatées
                if (line.startsWith("[") && line.contains("]")) {
                    int closeBracketIndex = line.indexOf("]");
                    String timeStr = line.substring(1, closeBracketIndex);
                    String text = line.substring(closeBracketIndex + 1);

                    try {
                        long timeMs = parseTimeToMs(timeStr);
                        lyrics.addLine(text, timeMs);
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid time format: " + timeStr);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // utile pour des cas comme "droits d'accès insuffisants" (plus spécifique que
            // IOException)
            // logger.warning("Lyrics file does not exist: " + e.getMessage());
            logger.warning("Le fichier des paroles n'existe pas : " + e.getMessage());
            return null;
        } catch (UnsupportedEncodingException e) {
            // encodage de caractères choisi (UTF-8) n'est pas supporté
            // logger.warning("Unsupported encoding when reading lyrics file: " +
            // e.getMessage());
            logger.warning("Encodage non supporté lors de la lecture du fichier des paroles : " + e.getMessage());
            return null;
        } catch (IOException e) {
            // logger.warning("Error reading lyrics file: " + e.getMessage());
            logger.warning("Erreur lors de la lecture du fichier des paroles : " + e.getMessage());
            return null;
        } catch (Exception e) {
            // logger.severe("Unexpected error while loading lyrics: " + e.getMessage());
            logger.severe("Erreur inattendue lors du chargement des paroles : " + e.getMessage());
            return null;
        }

        return lyrics;
    }

    /**
     * Analyse une chaîne de temps au format MM:SS.xx en millisecondes.
     * 
     * @param timeStr Chaîne de temps au format MM:SS.xx
     * @return Temps en millisecondes
     */
    private static long parseTimeToMs(String timeStr) {
        String[] parts = timeStr.split(":");
        int minutes = Integer.parseInt(parts[0]);

        String[] secondParts = parts[1].split("\\.");
        int seconds = Integer.parseInt(secondParts[0]);
        int milliseconds = 0;

        if (secondParts.length > 1) {
            // Gérer la partie décimale des secondes
            String msStr = secondParts[1];
            if (msStr.length() == 2) { // Format: SS.xx (hundredths of a second)
                milliseconds = Integer.parseInt(msStr) * 10;
            } else if (msStr.length() == 3) { // Format: SS.xxx (milliseconds)
                milliseconds = Integer.parseInt(msStr);
            }
        }

        return (minutes * 60 * 1000) + (seconds * 1000) + milliseconds;
    }
}