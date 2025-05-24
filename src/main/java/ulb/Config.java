package ulb;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Classe de configuration centralisée contenant toutes les constantes
 * utilisées dans le projet. Elle fournit :
 *
 * ➤ Les chemins d'accès aux ressources (musiques, images, modèles, etc.)
 * ➤ Les chemins des fichiers SQL organisés par type d'opérations (create, insert, update, search, common)
 *
 * Cette structure permet de centraliser la configuration et de faciliter
 * la maintenance future. Aucun objet de cette classe ne doit être instancié.
 */
public final class Config {

    // =========================================================================
    // 🌐 SECTION : CHEMINS DES RESSOURCES
    // =========================================================================

    // Permet de surcharger le chemin du dossier DATA_DIRECTORY_PATH pour les tests
    private static String dataDirectoryOverride = null;

    public static void setDataDirectoryPath(String path) {
        dataDirectoryOverride = path;
    }

    public static String getDataDirectoryPath() {
        return dataDirectoryOverride != null ? dataDirectoryOverride : DATA_DIRECTORY_PATH;
    }

    /**main dir of the app*/
    public static final String DATA_DIRECTORY_PATH = String.valueOf(Paths.get(System.getProperty("user.home"), ".deezify_g8"));

    /** Dossier contenant les musiques MP3 analysées. */
    public static final String MUSIC_DIRECTORY = "musiques/";

    public static final String SAMPLE_MUSIC_DIRECTORY = "musiques/";

    public static final List<String> SAMPLE_MUSICS = List.of(
            "B_U_R_N_-_bessonnitsa.mp3",
            "Formidable.mp3" // Add other file names as needed
    );

    /** Image par défaut utilisée pour les morceaux sans couverture. */
    public static final String DEFAULT_COVER_IMAGE = "/default_cover_image/default_cover_image.jpg";

    /** Dossier où sont stockées les images de couverture extraites des MP3. */
    public static final String COVER_IMAGES_DIRECTORY = "cover_images/";

    /** Chemin de la base de données SQLite (ou un fichier SQL si besoin). */
    public static final String DATABASE_PATH = "deezify.db";

    /** Dossier contenant les fichiers texte des paroles générées (format .txt). */
    public static final String LYRICS_TRACKS_DIRECTORY = "/lyrics_tracks/";

    /** Dossier contenant les fichiers karaoké synchronisés (format .lrc). */
    //public static final String KARAOKE_TRACKS_DIRECTORY = "src/main/resources/karaoke_tracks_directory/";
    public static final String KARAOKE_TRACKS_DIRECTORY = "/lrc/";

    // =========================================================================
    // 🗃️ SECTION : CHEMINS DES FICHIERS SQL (DAO)
    // =========================================================================

    /** Dossier contenant l'ensemble des fichiers SQL. */
    // Ce chemin est relatif au dossier resources quand on utilise getResourceAsStream()
    public static final String SQL_FILES_DIRECTORY = "sql/";  // Chemin relatif au dossier resources

    // ➤ Fichiers SQL pour la création des tables et triggers
    public static final String CREATE_TABLES_SQL_FILE = SQL_FILES_DIRECTORY + "create_tables.sql";

    // ➤ Fichiers SQL pour les requêtes d'insertion
    public static final String INSERT_QUERIES_SQL_FILE = SQL_FILES_DIRECTORY + "insert_queries.sql";

    // ➤ Fichiers SQL pour les requêtes de mise à jour
    public static final String UPDATE_QUERIES_SQL_FILE = SQL_FILES_DIRECTORY + "update_queries.sql";

    // ➤ Fichiers SQL pour les requêtes de recherche
    public static final String SEARCH_QUERIES_SQL_FILE = SQL_FILES_DIRECTORY + "search_queries.sql";

    // ➤ Fichier SQL pour les requêtes communes (DbManager : id lookup, etc.)
    public static final String COMMON_QUERIES_SQL_FILE = SQL_FILES_DIRECTORY + "common_queries.sql";

    public static final List<String> NEEDED_DIRECTORIES = Arrays.asList(MUSIC_DIRECTORY, COVER_IMAGES_DIRECTORY, LYRICS_TRACKS_DIRECTORY, KARAOKE_TRACKS_DIRECTORY);
    /**
     * Constructeur privé pour empêcher toute instanciation.
     */
    private Config() {
        // Classe utilitaire statique : pas d'instance
    }

    static public String getFullPathFromRelative(String partial){
        return String.valueOf(Paths.get(getDataDirectoryPath(), partial));
    }

    public static class CouldNotSetUpDataFolder extends Exception{
        public CouldNotSetUpDataFolder(String message) {
            super(message);
        }
    }

    static public void setUpFolders() throws CouldNotSetUpDataFolder {
        Logger logger = Logger.getLogger(Main.class.getName());
        for (String needed_dir : Config.NEEDED_DIRECTORIES) {
            File dir = new File(Config.getFullPathFromRelative(needed_dir));
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    logger.severe("could not create data dir in your home dir");
                    throw new CouldNotSetUpDataFolder("cant create folder " + needed_dir);
                }
            } else {
                logger.info("data dir already exists");
                logger.info(needed_dir);
            }
        }
    }
}
