package ulb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import ulb.controller.MainController;
import ulb.controller.AudioPlayerController;
import ulb.controller.PlaylistController;
import ulb.i18n.LanguageManager;
import ulb.model.ChangeTracker;
import ulb.model.Playlist;
import ulb.model.PlaylistManager;
import ulb.model.Queue;
import ulb.model.Track;
import ulb.model.TrackLibrary;
import ulb.services.AppServices;
import ulb.view.MainViewController;
import ulb.view.utils.AlertManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ulb.i18n.LanguageManager;

public class GuiMain extends Application {

    public static AudioPlayerController audioPlayerController; // turn into a singleton
    public static PlaylistController playlistController; // controller for playlists
    public static MainController mainController; // Main controller
    private static final Logger logger = Logger.getLogger(GuiMain.class.getName());
    private MainViewController mainViewController;
    private Parent main;
    private TrackLibrary library; // On stocke l'instance ici pour le réutiliser

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Initialize LanguageManager first
        LanguageManager languageManager = LanguageManager.getInstance();
        try{
            AppServices.init();
            AppServices.getDbSeeder().seedDatabase();

            // === 🔄 Initialisation de la bibliothèque de pistes
            initializeLibrary();

            // === 🔄 Chargement des playlists depuis la BDD
            initializePlaylists();

            // === 🔄 Création de la vue principale avec i18n
            loadMainView();

            // === 🔄 Création de la vue du lecteur avec i18n
            initializePlayerView();


            // Lancement du contrôleur principal
            Queue queue = new Queue("queue");
            mainController = new MainController(main, mainViewController, library, queue);
            playlistController = mainController.getPlaylistController();
            mainController.goToLibrary();
            mainController.startApplication(primaryStage);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur critique lors du démarrage de l'interface utilisateur", e);
            AlertManager.showCriticalErrorWithException("Démarrage impossible",
                "Une erreur critique est survenue lors du démarrage de l'application. L'application va se fermer.", e, true);
        } catch (Exception e) { // Catch any other unexpected runtime exceptions
            logger.log(Level.SEVERE, "Erreur inattendue et critique lors du démarrage de l'interface utilisateur", e);
            AlertManager.showCriticalErrorWithException("Démarrage impossible - Erreur Inattendue",
                "Une erreur inattendue et critique est survenue. L'application va se fermer.", e, true);
        }
    }

    /**
     * Initialise la bibliothèque de pistes musicales.
     */
    private void initializeLibrary() {
        ChangeTracker changeTracker = new ChangeTracker();
        library = new TrackLibrary();
        library.addObserver(changeTracker);
        library.setTracks(AppServices.getDbSearch().getAllTracks());
        AppServices.getDbUpdate().setChanges(changeTracker);
        AppServices.getDbUpdate().setTrackLibrary(library);
    }

    /**
     * Charge les playlists depuis la base de données.
     */
    private void initializePlaylists() {
        PlaylistManager playlistManager = PlaylistManager.getInstance();
        Map<String, List<Track>> playlistsWithTracks = AppServices.getDbSearch().getAllPlaylistsWithTracks();
        logger.info("playlist = " + playlistsWithTracks);

        for (Map.Entry<String, List<Track>> entry : playlistsWithTracks.entrySet()) {
            String playlistTitle = entry.getKey();
            List<Track> playlistTracks = entry.getValue();
            logger.info("[DEBUG] Chargement playlist: " + playlistTitle + " - " + playlistTracks.size() + " tracks");

            Playlist playlist = new Playlist(playlistTitle);
            for (Track track : playlistTracks) {
                playlist.addTrack(track);
            }
            playlistManager.addPlaylist(playlist);
        }
    }

    /**
     * Charge la vue principale et l'associe au contrôleur.
     */
    private void loadMainView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        loader.setResources(LanguageManager.getInstance().getResourceBundle());
        main = loader.load();
        mainViewController = loader.getController();
    }

    /**
     * Initialise la vue du lecteur.
     */
    private void initializePlayerView() throws IOException {
        FXMLLoader playerLoader = new FXMLLoader(getClass().getResource("/fxml/PlayerView.fxml"));
        playerLoader.setResources(LanguageManager.getInstance().getResourceBundle());
        Parent playerControls = playerLoader.load();
        mainViewController.setPlayerControls(playerControls);
        audioPlayerController = new AudioPlayerController(playerLoader.getController(), library);
    }

    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out, true));
        System.setErr(new java.io.PrintStream(System.err, true));
        logger.info("[DEBUG] Rebranché println standard");
        launch(args);
    }
}
