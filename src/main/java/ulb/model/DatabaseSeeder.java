package ulb.model;

import ulb.Config;
import ulb.dao.DbInitializer;
import ulb.dao.DbManagerInsert;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.nio.file.Paths;

/**
 * Classe responsable du remplissage automatique de la base de données.
 * Elle scanne un dossier contenant des fichiers `.mp3`, extrait leurs
 * métadonnées et les insère dans la base.
 */
public class DatabaseSeeder {
    private DbManagerInsert dbInsert; // permet d'effectuer des insertions
    private MetadataManager metadataManager;
    private String musicDirectory = Config.MUSIC_DIRECTORY; // Chemin du dossier contenant les fichiers `.mp3`
    public static final Logger logger = Logger.getLogger(DbInitializer.class.getName());

    /**
     * Constructeur de `DatabaseSeeder`
     *
     * @param metadataManager Instance de `MetadataManager` pour extraire les
     *                        métadonnées des fichiers audio.
     */
    public DatabaseSeeder(DbManagerInsert dbInsert, MetadataManager metadataManager) {
        this.dbInsert = dbInsert;
        this.metadataManager = metadataManager;
    }

    public void addSampleMusic() throws IOException {
        Path targetDir = Paths.get(Config.getFullPathFromRelative(Config.MUSIC_DIRECTORY));

        // Get the class loader of the current class to access resources
        ClassLoader classLoader = getClass().getClassLoader();

        // Access the resource folder (assuming the resources are in "musiques")
        String resourceFolder = Config.SAMPLE_MUSIC_DIRECTORY;  // Folder inside resources


        for (String resourceFileName : Config.SAMPLE_MUSICS) {
            // Build the resource path using Path
            Path resourcePath = Paths.get(resourceFolder, resourceFileName);

            // Get the resource URL
            URL resourceFileUrl = classLoader.getResource(resourcePath.toString());

            if (resourceFileUrl == null) {
                throw new FileNotFoundException("Resource file not found: " + resourcePath);
            }

            // Define the target file path where the resource will be copied
            Path targetFilePath = targetDir.resolve(resourceFileName);

            // Use Buffered Streams to copy the resource file
            try (InputStream resourceStream = resourceFileUrl.openStream();
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(resourceStream);
                 OutputStream outputStream = Files.newOutputStream(targetFilePath);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {

                byte[] buffer = new byte[4096]; // You can adjust the buffer size
                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
                }

                bufferedOutputStream.flush();  // Ensure all data is written out

                logger.info("Copied: " + resourceFileName + " → " + targetFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    /**
     * Scanne le dossier et ajoute les morceaux en base de données.
     */
    public void seedDatabase() {
        try {
            addSampleMusic();
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("could not add sample music" + e.getMessage());
        }

        File folder = new File(Config.getFullPathFromRelative(Config.MUSIC_DIRECTORY));
        if (!folder.exists() || !folder.isDirectory()) {
            logger.warning("❌ Le dossier spécifié n'existe pas ou n'est pas un dossier valide.");
            return;
        }
        logger.info("chemin absolu du fichier => " + folder.getAbsolutePath() + folder.exists());
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files == null || files.length == 0) {
            logger.info("ℹ️ Aucun fichier .mp3 trouvé dans le dossier.");
            return;
        }
        logger.info("🔍 Début du scan des fichiers audio...");

        for (File file : files) {
            processTrack(file);
        }
        logger.info("✅ Base de données remplie avec succès !");
    }

    /**
     * Traite un fichier audio, extrait ses métadonnées et l'ajoute en base de
     * données.
     *
     * @param file Fichier audio `.mp3`
     */
    private void processTrack(File file) {
        // Extraction des métadonnées avec MetadataManager
        Optional<Track> track = metadataManager.extractMetadata(file);
        if (track.isEmpty()) {
            logger.warning("⚠️ Impossible d'extraire les métadonnées pour : " + file.getName());
            return;
        }
        // Ajout du morceau à la base de données
        boolean trackId = dbInsert.insertTrack(track.get());

        if (trackId == false) {
            logger.warning("❌ Échec de l'ajout du morceau : " + track.get().getTitle());
            return;
        }
        logger.info("🎵 Ajouté en base : " + track.get().getTitle() + " - " + track.get().getArtist() + " (" + track.get().getAlbum() + ") [" + track.get().getYear() + "] [" + track.get().getGenre() + "]");
    }
}
