// src/main/java/ulb/controller/MainController.java
package ulb.controller;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ulb.controller.handleError.PageExistsException;
import ulb.model.PlaylistManager;
import ulb.model.Queue;
import ulb.model.Track;
import ulb.model.TrackLibrary;
import ulb.services.AppServices;
import ulb.view.*;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import static ulb.GuiMain.audioPlayerController;

/**
 * Main controller of the application.
 * This class manages the entire application, including the main view,
 * page navigation, and the graphical startup of the application.
 */
public class MainController {

    private MainViewController mainViewController;
    private Parent mainParent;
    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    private HomeController home;
    private LibraryController library;
    private PlaylistPageController playlistList;
    private PlaylistController playlist;
    private RadioController radio;
    private QueueController queue;
    private SearchController search;
    private MetaDataController metadata;

    /**
     * Constructor of the MainController class.
     *
     * @param mainParent The parent node of the main view.
     * @param mainViewController The controller of the main view.
     */
    public MainController(Parent mainParent,
                          MainViewController mainViewController,
                          TrackLibrary trackLibrary,
                          Queue modelQueue) throws IOException {
        this.mainViewController = mainViewController;
        this.mainViewController.setMainController(this);
        this.mainParent = mainParent;

        try {
            home = new HomeController(
                (HomeViewController) mainViewController.addPage("/fxml/HomeView.fxml", EPages.HOME),
                this
            );
            playlist = new PlaylistController(
                (PlaylistViewController) mainViewController.addPage("/fxml/PlaylistView.fxml", EPages.PLAYLIST),
                this,
                AppServices.getDbInsert(),
                audioPlayerController,
                PlaylistManager.getInstance()
            );
            playlistList = new PlaylistPageController(
                (PlaylistPageViewController) mainViewController.addPage("/fxml/PlaylistPageView.fxml", EPages.PLAYLIST_LIST),
                this,
                playlist
            );
            radio = new RadioController(
                (RadioViewController) mainViewController.addPage("/fxml/RadioView.fxml", EPages.RADIO),
                this
            );
            this.queue = new QueueController(
                modelQueue,
                (QueueViewController) mainViewController.addPage("/fxml/QueueView.fxml", EPages.QUEUE),
                this
            );
            search = new SearchController(
                (SearchViewController) mainViewController.addPage("/fxml/SearchView.fxml", EPages.SEARCH),
                this
            );
            metadata = new MetaDataController(
                trackLibrary,
                (MetaDataViewController) mainViewController.addPage("/fxml/MetaDataView.fxml", EPages.METADATA),
                this
            );
            library = new LibraryController(
                trackLibrary,
                (LibraryViewController) mainViewController.addPage("/fxml/LibraryView.fxml", EPages.LIBRARY),
                this
            );

        } catch (PageExistsException e) {
            logger.severe("Multiple pages added with the same ID; some pages will be missing.");
        }
    }

    public void gotToQueue() {
        mainViewController.showPage(EPages.QUEUE);
    }

    public void goToChangeMetaData(Track track) {
        metadata.setTrack(track);
        mainViewController.showPage(EPages.METADATA);
    }

    public void goToHome() {
        mainViewController.showPage(EPages.HOME);
    }

    public void goToSearch() {
        mainViewController.showPage(EPages.SEARCH);
    }

    public void goToLibrary() {
        mainViewController.showPage(EPages.LIBRARY);
    }

    public void goToPlaylistList() {
        mainViewController.showPage(EPages.PLAYLIST_LIST);
    }

    public void goToPlaylist() {
        mainViewController.showPage(EPages.PLAYLIST);
    }

    public void goToRadioPage() {
        mainViewController.showPage(EPages.RADIO);
    }

    /**
     * Starts the application by setting up the scene and stage.
     */
    public void startApplication(Stage primaryStage) {
        Scene scene = new Scene(mainParent);
        String[] stylesheets = {
            "/css/styles/base.css",
            "/css/styles/home.css",
            "/css/styles/library.css",
            "/css/styles/player.css",
            "/css/styles/playlist.css",
            "/css/styles/search.css",
            "/css/styles/sidebar.css",
            "/css/styles/track.css",
            "/css/styles/icon-buttons.css",
            "/css/styles/queue.css"
        };

        for (String path : stylesheets) {
            URL url = getClass().getResource(path);
            if (url != null) {
                scene.getStylesheets().add(url.toExternalForm());
            } else {
                logger.warning("⚠️ Stylesheet not found: " + path);
            }
        }

        primaryStage.setTitle("Music Player");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    public QueueController getQueue() {
        return queue;
    }

    /**
     * Retourne le contrôleur de playlists.
     * @return Contrôleur de playlists
     */
    public PlaylistController getPlaylistController() {
        return playlist;
    }
}
