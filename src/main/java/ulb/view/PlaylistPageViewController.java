package ulb.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import ulb.controller.PlaylistPageController;
import ulb.i18n.LanguageManager;
import ulb.model.Playlist;
import ulb.model.PlaylistManager;
import ulb.model.PlaylistsObserver;
import ulb.view.utils.AlertManager;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class PlaylistPageViewController extends PageViewController
        implements PlaylistsObserver, Initializable {

    private static final Logger logger = Logger.getLogger(PlaylistPageViewController.class.getName());

    @FXML private Label titleLabel;
    @FXML private TextField inputField;
    @FXML private Button createButton;
    @FXML private Label sectionTitleLabel;
    @FXML private FlowPane playlistsContainer;

    private PlaylistPageController controller;
    private final LanguageManager lang = LanguageManager.getInstance();

    /** Appelé par FXMLLoader */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1️⃣ Texte initial
        updateTexts(resources);

        // 2️⃣ Réagir aux changements de langue
        lang.localeProperty().addListener((obs, oldLoc, newLoc) ->
            updateTexts(lang.getResourceBundle())
        );

        // 3️⃣ Observer le manager seulement
        PlaylistManager.getInstance().addObserver(this);
        // Ne pas charger les playlists ici, cela sera fait après l'injection du controller
    }

    /** Liaison du contrôleur métier */
    public void setObserver(PlaylistPageController controller) {
        this.controller = controller;
        // Maintenant que le controller est défini, on peut charger les playlists existantes
        loadExistingPlaylists();
    }

    /** Met à jour tous les textes depuis le bundle */
    private void updateTexts(ResourceBundle b) {
        titleLabel.setText(b.getString("playlistPage.title"));
        inputField.setPromptText(b.getString("playlistPage.newPrompt"));
        createButton.setText(b.getString("playlistPage.createButton"));
        sectionTitleLabel.setText(b.getString("playlistPage.sectionTitle"));
    }

    /** Charge les playlists existantes au démarrage */
    private void loadExistingPlaylists() {
        for (Playlist playlist : PlaylistManager.getInstance().getPlaylists()) {
            addPlaylistToView(playlist.getTitle());
        }
    }

    /** Ajoute une playlist à l’interface, en passant le bundle */
    private void addPlaylistToView(String title) {
        ResourceBundle bundle = lang.getResourceBundle();
        URL fxml = getClass().getResource("/fxml/PlayListFrontView.fxml");
        FXMLLoader loader = new FXMLLoader(fxml, bundle);
        try {
            VBox playlistBox = loader.load();
            PlayListFrontViewController front = loader.getController();
            front.setTitle(title);
            front.setController(controller.getPlaylistFrontController());
            playlistBox.setUserData(front);
            playlistsContainer.getChildren().add(playlistBox);
        } catch (IOException e) {
            logger.warning("❌ Erreur chargement PlayListFrontView.fxml : " + e.getMessage());
        }
    }

    @Override
    public void onPlaylistAdded(Playlist playlist) {
        addPlaylistToView(playlist.getTitle());
    }

    @Override
    public void onPlaylistRemoved(Playlist playlist) {
        playlistsContainer.getChildren().removeIf(node -> {
            if (node instanceof VBox) {
                PlayListFrontViewController ctrl = (PlayListFrontViewController) node.getUserData();
                return ctrl != null && playlist.getTitle().equals(ctrl.getTitle());
            }
            return false;
        });
    }

    @Override
    public void onPlaylistAltered(Playlist playlist) {}

    /** Bouton "Créer nouvelle playlist" */
    @FXML
    public void handleCreateNewPlaylistClick() {
        String title = inputField.getText().trim();
        if (title.isEmpty()) {
            logger.info("⚠️ Le nom de la playlist est vide.");
            AlertManager.showWarning("Nom de playlist invalide", 
                                     "Veuillez entrer un nom pour la nouvelle playlist.");
            return;
        }
        inputField.clear();
        // Création de la playlist via le contrôleur
        // La notification sera automatiquement envoyée par le PlaylistManager
        // et la méthode onPlaylistAdded sera appelée pour mettre à jour l'interface
        controller.createPlaylistFromTitle(title);
        logger.info("Nouvelle playlist créée : " + title);
    }
}
