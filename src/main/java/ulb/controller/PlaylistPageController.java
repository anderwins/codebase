package ulb.controller;

import ulb.Main;
import ulb.view.PlaylistPageViewController;

import java.util.List;

/**
 * Contrôleur de la page de gestion des playlists.
 * Permet de créer une playlist à partir d'un titre et de gérer la vue associée.
 */
public class PlaylistPageController extends PageController{

    /** Contrôleur de la vue associée à cette page */
    private PlaylistPageViewController viewController;
    private PlaylistController playlistController;

    /**
     * Constructeur du contrôleur de la page de playlists.
     * @param controller Contrôleur de la vue associée
     */
    public PlaylistPageController(PlaylistPageViewController controller, MainController mainController, PlaylistController playlistController) {
        super(mainController);
        viewController = controller;
        viewController.setObserver(this);
        this.playlistController = playlistController;

    }

    public PlayListFrontController getPlaylistFrontController(){
        return new PlayListFrontController(this.mainController);
    }

    /**
     * Crée une nouvelle playlist à partir d'un titre.
     * @param title Le titre de la nouvelle playlist
     * @return Le titre de la playlist créée
     */
    public String createPlaylistFromTitle(String title) {
        return this.playlistController.createNewPlaylistFromTitle(title);
    }
}
