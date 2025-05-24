package ulb.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import ulb.controller.LibraryController;
import ulb.i18n.LanguageManager;
import ulb.model.Track;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class LibraryViewController extends PageViewController implements Initializable {

    @FXML private Label libraryTitleLabel;
    @FXML private Label mySongsLabel;  
    @FXML public VBox tracksContainer;

    private LibraryController controller;
    private static final Logger logger = Logger.getLogger(LibraryViewController.class.getName());
    private final LanguageManager lang = LanguageManager.getInstance();

    /** Appelé par MainController pour lier la logique-metier */
    public void setController(LibraryController libraryController) {
        this.controller = libraryController;
    }

    /** Chargé automatiquement par FXMLLoader avec le ResourceBundle injecté */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1) Texte initial
        updateTexts(resources);

        // 2) Rafraîchir à chaud quand la locale change
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            updateTexts(lang.getResourceBundle());
        });
    }

    /** Met à jour tous les labels et le bouton depuis le bundle */
    private void updateTexts(ResourceBundle bundle) {
        libraryTitleLabel.setText(bundle.getString("library.title"));
        mySongsLabel.setText(bundle.getString("library.mySongs"));
    }

    /** Affiche la liste des pistes */
    public void setTracks(List<Track> tracks) {
        tracksContainer.getChildren().clear();
        for (Track track : tracks) {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/TrackView.fxml"),
                lang.getResourceBundle()  // si tu veux aussi localiser TrackView.fxml
            );
            try {
                VBox trackBox = loader.load();
                TrackViewController viewController = loader.getController();
                viewController.setTrack(track);
                viewController.setController(controller.getTrackFrontController(track));
                tracksContainer.getChildren().add(trackBox);
            } catch (IOException e) {
                logger.severe("❌ Erreur chargement TrackView.fxml : " + e.getMessage());
            } catch (Exception e) {
                logger.severe("❌ Erreur inattendue lors de l’ajout d’un track : " + e.getMessage());
            }
        }
    }
}
