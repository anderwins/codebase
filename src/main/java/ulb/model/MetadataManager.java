package ulb.model;

import ulb.Config;
import ulb.model.handbleError.MetadataExtractionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.datatype.Artwork;
import javafx.util.Pair;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.nio.file.Paths;

import java.util.Optional;


/**
 * Classe responsable de l'extraction des métadonnées et des images des fichiers
 * MP3 ainsi que la modification
 * des métadonnées d'un fichier audio..
 */
public class MetadataManager {

    private String default_cover_image = String
            .valueOf(getClass().getResource(Config.DEFAULT_COVER_IMAGE).toExternalForm()); // l'image de couverture par
                                                                                           // defaut pour les morceaux
                                                                                           // n'ayant pas d'image de
                                                                                           // couverture
    private String lyrics_tracks_directory_path = Config.getFullPathFromRelative(Config.LYRICS_TRACKS_DIRECTORY);
    private String karaoke_tracks_directory_path = Config.getFullPathFromRelative(Config.KARAOKE_TRACKS_DIRECTORY);
    private static final Logger logger = Logger.getLogger(MetadataManager.class.getName());

    /**
     * Constructeur de `MetadataManager`
     * 
     * @throws IOException si le modèle de transcription ne peut pas être chargé.
     */
    public MetadataManager() {

    }

    /**
     * Extrait les métadonnées d'un fichier MP3 et enregistre la pochette si
     * disponible.
     * 
     * @param mp3File Le fichier MP3 à analyser.
     * @return Une `Track` contenant les métadonnées et le chemin de l'image.
     */
    public Optional<Track> extractMetadata(File mp3File) {
        try {
            // Lecture du fichier audio et extraction des métadonnées
            AudioFile audioFile = AudioFileIO.read(mp3File);
            Tag tag = audioFile.getTag();
            // Extraction des informations principales
            String title = getTagValue(tag, FieldKey.TITLE, mp3File.getName().replace(".mp3", ""));
            String artist = getTagValue(tag, FieldKey.ARTIST, "Inconnu");
            String album = getTagValue(tag, FieldKey.ALBUM, "Inconnu");
            String year = getTagValue(tag, FieldKey.YEAR, "0000");
            String genre = getTagValue(tag, FieldKey.GENRE, "Inconnu");
            int duration = audioFile.getAudioHeader().getTrackLength(); // Durée en secondes
            String file_path = mp3File.getAbsolutePath();
            // Gestion de l'image de couverture
            String coverPath = extractCoverImage(tag, title);
            // String wavPath = convertMp3ToWav(file_path); // Convertit le MP3 en WAV pour
            // Vosk
            // Création des fichiers de paroles et karaoke
            // Pair<String, String> returPath = createLyricsAndKaraoteFiles(wavPath);
            String lyricsPath = getLyricsFilePath(mp3File, title);
            String karaokePath = getKaraokeFilePath(mp3File, title);
            // String = createKaraokeFile(wavPath, title);
            // Création de l'objet Track avec les nouveaux chemins
            return Optional.of(new Track(title, artist, album, year, duration, genre, file_path, coverPath, lyricsPath,
                    karaokePath));

        } catch (FileNotFoundException e) {
            // fichier MP3 n'est pas trouvé
            logger.warning("❌ Fichier MP3 introuvable : " + mp3File.getName() + " (" + e.getMessage() + ")");
        } catch (IOException e) {
            // erreurs liées à l'entrée/sortie, problème d'accès ou de lecture du fichier
            logger.warning(
                    "❌ Erreur lors de la lecture du fichier MP3 : " + mp3File.getName() + " (" + e.getMessage() + ")");
        } catch (TagException e) {
            // extraction des métadonnées échoue, format du fichier ou les tags
            logger.warning("❌ Erreur lors de l'extraction des métadonnées du fichier MP3 : " + mp3File.getName() + " ("
                    + e.getMessage() + ")");
        } catch (Exception e) {
            logger.severe("❌ Erreur inattendue lors de l'extraction des métadonnées du fichier MP3 : "
                    + mp3File.getName() + " (" + e.getMessage() + ")");
        }
        return Optional.empty();
    }

    /**
     * Récupère la valeur d'un tag, en utilisant une valeur par défaut si elle est
     * vide.
     * 
     * @param tag          L'objet contenant les métadonnées du fichier MP3.
     * @param key          La clé du champ à récupérer.
     * @param defaultValue Valeur par défaut si le champ est vide.
     * @return La valeur extraite ou la valeur par défaut.
     */
    private String getTagValue(Tag tag, FieldKey key, String defaultValue) {
        if (tag != null) {
            String value = tag.getFirst(key);
            return (value == null || value.isEmpty()) ? defaultValue : value;
        }
        return defaultValue;
    }

    /**
     * Extrait et enregistre l'image de couverture si elle est présente dans les
     * métadonnées du fichier MP3.
     * Sinon, retourne une image par défaut.
     * 
     * @param tag   Les métadonnées du fichier MP3.
     * @param title Le titre du morceau pour nommer le fichier image.
     * @return Le chemin de l'image enregistrée ou l'image par défaut si aucune
     *         image n'a été trouvée.
     */
    private String extractCoverImage(Tag tag, String title) throws MetadataExtractionException {
        if (tag != null && tag.getFirstArtwork() != null) {
            Artwork artwork = tag.getFirstArtwork();
            byte[] imageData = artwork.getBinaryData();
            if (imageData != null) {
                try {
                    // Vérifier et créer le dossier covers/ si nécessaire
                    String image_dir = Config.getFullPathFromRelative(Config.COVER_IMAGES_DIRECTORY);
                    File coverDir = new File(image_dir);
                    if (!coverDir.exists()) {
                        if (!coverDir.mkdirs()) {
                            // Log failure to create directory and throw
                            logger.warning("Impossible de créer le répertoire des couvertures: " + image_dir);
                            throw new MetadataExtractionException("Impossible de créer le répertoire des couvertures: " + image_dir, null);
                        }
                    }
                    // Chemin où sauvegarder l'image
                    String imagePath = String
                            .valueOf(Paths.get(image_dir, title.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg"));
                    File imageFile = new File(imagePath);

                    try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                        fos.write(imageData);
                    }
                    logger.info("✅ Image de couverture enregistrée : " + imagePath);
                    return imagePath;
                } catch (IOException e) {
                    // Erreur lors de la création du répertoire ou de l'écriture de l'image.
                    logger.log(Level.WARNING, "Impossible d'écrire l'image de couverture pour " + title, e);
                    throw new MetadataExtractionException("Impossible d'écrire l'image de couverture", e);
                } catch (Exception e) {
                    // Autres erreurs inattendues lors de la manipulation de l'artwork ou du chemin.
                    logger.log(Level.SEVERE, "Erreur inattendue lors de l'extraction de l'image de couverture pour " + title, e);
                    throw new MetadataExtractionException("Erreur inattendue lors de l'extraction de l'image de couverture", e);
                }
            }
        }
        // Si aucune image n'est trouvée dans les tags, ou si imageData est null
        logger.info("Aucune image trouvée dans les tags pour " + title + ", utilisation de l'image par défaut.");
        return default_cover_image;
    }

    private String getLyricsFilePath(File mp3File, String title) {
        String sanitizedTitle = (title != null && !title.isEmpty()) ? 
                                title.replaceAll("[^a-zA-Z0-9.-]", "_") :
                                mp3File.getName().replaceAll("[^a-zA-Z0-9.-]", "_").replace(".mp3", "");
        String lyricsFilePath = lyrics_tracks_directory_path + File.separator + sanitizedTitle + ".txt";
        File lyricsFile = new File(lyricsFilePath);
        if (lyricsFile.exists()) {
            logger.info("✅ Fichier paroles (.txt) trouvé : " + lyricsFilePath);
            return lyricsFilePath;
        }
        logger.info("ℹ️ Aucun fichier paroles (.txt) trouvé pour: " + sanitizedTitle + ".txt");
        return null; 
    }

    private String getKaraokeFilePath(File mp3File, String title) {
        String sanitizedTitle = (title != null && !title.isEmpty()) ? 
                                title.replaceAll("[^a-zA-Z0-9.-]", "_") :
                                mp3File.getName().replaceAll("[^a-zA-Z0-9.-]", "_").replace(".mp3", "");
        String karaokeFilePath = karaoke_tracks_directory_path + File.separator + sanitizedTitle + ".lrc";
        File karaokeFile = new File(karaokeFilePath);
        if (karaokeFile.exists()) {
            logger.info("✅ Fichier karaoké trouvé : " + karaokeFilePath);
            return karaokeFilePath;
        }
        logger.info("ℹ️ Aucun fichier karaoké (.lrc) trouvé pour: " + sanitizedTitle + ".lrc");
        return null;
    }

    /**
     * Crée un fichier de paroles (.txt) en transcrivant le fichier audio.
     * Si le dossier des paroles n'existe pas, il est créé automatiquement.
     *
     * @return Chemin du fichier de paroles généré.
     */
    // Crée les fichiers de paroles et karaoké
    public Pair<String, String> createLyricsAndKaraokeFiles(String wavPath) {
        ensureDirectoryExists(lyrics_tracks_directory_path);
        ensureDirectoryExists(karaoke_tracks_directory_path);

        // Récupération du fichier WAV
        File wavFile = new File(wavPath);
        if (!wavFile.exists()) {
            logger.warning("❌ Le fichier WAV spécifié est introuvable : " + wavPath);
            return null;
        }

        if (!wavFile.getName().toLowerCase().endsWith(".wav")) {
            logger.warning("❌ Le fichier spécifié n'est pas un fichier WAV valide : " + wavPath);
            return null;
        }

        // Extraction et nettoyage du titre
        String fileName = cleanFileName(wavFile.getName());

        // Génération des chemins
        String lyricsFilePath = lyrics_tracks_directory_path + fileName + ".txt";
        String karaoteFilePath = karaoke_tracks_directory_path + fileName + ".lrc";

        // Transcription
        // speechToTextGenerator.transcribeAndGenerateFiles(wavFile, fileName);

        logger.info("✅ Fichiers générés :\n - Paroles : " + lyricsFilePath + "\n - Karaoké : " + karaoteFilePath);

        return new Pair<>(lyricsFilePath, karaoteFilePath);
    }

    private String cleanFileName(String fileName) {
        return fileName.replace(".wav", "").replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Vérifie si un dossier existe, sinon le crée.
     *
     * @param directoryPath Chemin du dossier à vérifier/créer.
     */
    private void ensureDirectoryExists(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.info("📁 Dossier créé : " + directoryPath);
            } else {
                logger.warning("❌ Impossible de créer le dossier : " + directoryPath);
            }
        }
    }

    /**
     * Modifie les métadonnées d'un fichier audio.
     * 
     * @param track Objet Track contenant les nouvelles informations.
     * @return `true` si la mise à jour a réussi, sinon `false`.
     */
    public boolean updateMetadata(Track track) {
        File file = new File(track.getFilePath());
        if (!file.exists()) {
            logger.warning("Fichier introuvable : " + track.getFilePath());
            return false;
        }
        try {
            MP3File mp3File = new MP3File(file);
            Tag tag = mp3File.getTag();

            tag.setField(FieldKey.TITLE, track.getTitle());
            tag.setField(FieldKey.ARTIST, track.getArtist());
            tag.setField(FieldKey.ALBUM, track.getAlbum());

            mp3File.commit();
            return true;
        } catch (CannotWriteException e) {
            logger.warning("Erreur lors de la mise à jour des métadonnées : " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warning("Une erreur inattendue s'est produite : " + e.getMessage());
            return false;
        }
    }
}
