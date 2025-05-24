// src/main/java/ulb/view/MainViewController.java
package ulb.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ulb.controller.MainController;
import ulb.controller.handleError.PageExistsException;
import ulb.i18n.LanguageManager;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {
    @FXML private StackPane contentArea;
    @FXML public VBox playerControlsContainer;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Button buttonHome;
    @FXML private Button buttonSearch;
    @FXML private Button buttonLibrary;
    @FXML private Button buttonPlaylists;
    @FXML private Button buttonRadio;
    @FXML private Button buttonQueue;

    private MainController mainController;
    private final HashMap<String, Parent> pages = new HashMap<>();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML public void showHomeView()          { mainController.goToHome(); }
    @FXML private void showLibraryView()      { mainController.goToLibrary(); }
    @FXML private void showPlaylistPageView() { mainController.goToPlaylistList(); }
    @FXML private void showRadioView()        { mainController.goToRadioPage(); }
    @FXML public void onShowQueue()          { mainController.gotToQueue(); }
    @FXML private void showSearchView()       { mainController.goToSearch(); }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1️⃣ Initialiser le ComboBox avec les codes
        languageComboBox.getItems().addAll("FR", "EN", "NL");
        // Définir le promptText localisé
        languageComboBox.setPromptText(resources.getString("main.selectLanguage"));
        // Positionner la valeur par défaut
        String current = resources.getLocale().getLanguage().toUpperCase();
        if (languageComboBox.getItems().contains(current)) {
            languageComboBox.setValue(current);
        }

        // 2️⃣ Écoute du choix utilisateur pour changer la locale
        languageComboBox.valueProperty().addListener((obs, old, neo) -> {
            if (neo != null) {
                switch (neo) {
                    case "EN" -> LanguageManager.getInstance().setLocale(Locale.ENGLISH);
                    case "NL" -> LanguageManager.getInstance().setLocale(new Locale("nl"));
                    default   -> LanguageManager.getInstance().setLocale(Locale.FRENCH);
                }
            }
        });
        
       // localisation initiale des boutons
        updateTexts(resources);

        // 3️⃣ Rafraîchir le promptText quand la locale change ailleurs
        LanguageManager.getInstance().localeProperty().addListener((obs, oldLoc, newLoc) -> {
            ResourceBundle b = LanguageManager.getInstance().getResourceBundle();
            languageComboBox.setPromptText(b.getString("main.selectLanguage"));
            // ET relocalise tes boutons
            updateTexts(b);
        });

    }

    /**
     * Charge un FXML, l’injecte dans la vue et retourne son controller typé.
     */
    @SuppressWarnings("unchecked")
    public <T, E extends Enum<E>> T addPage(String fxmlFile, E id)
            throws PageExistsException, IOException {
        String nameId = id.toString();
        if (pages.containsKey(nameId)) {
            throw new PageExistsException("Page ID " + nameId + " already exists");
        }
        ResourceBundle bundle = LanguageManager.getInstance().getResourceBundle();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile), bundle);
        Parent page = loader.load();
        AnchorPane wrapper = new AnchorPane(page);
        AnchorPane.setTopAnchor(page, 0.0);
        AnchorPane.setBottomAnchor(page, 0.0);
        AnchorPane.setLeftAnchor(page, 0.0);
        AnchorPane.setRightAnchor(page, 0.0);
        wrapper.getStyleClass().add("page-wrapper");
        pages.put(nameId, wrapper);
        contentArea.getChildren().add(wrapper);
        return (T) loader.getController();
    }

    /** Met la page en avant-plan */
    public <E extends Enum<E>> void showPage(E id) {
        Parent page = pages.get(id.toString());
        if (page != null) page.toFront();
    }

    /** Remplace les contrôles du lecteur audio */
    public void setPlayerControls(Parent playerControls) {
        playerControlsContainer.getChildren().setAll(playerControls);
    }

    private void updateTexts(ResourceBundle b) {
        buttonHome     .setText(b.getString("main.buttonHome"));
        buttonSearch   .setText(b.getString("main.buttonSearch"));
        buttonLibrary  .setText(b.getString("main.buttonLibrary"));
        buttonPlaylists.setText(b.getString("main.buttonPlaylists"));
        buttonRadio    .setText(b.getString("main.buttonRadio"));
        buttonQueue    .setText(b.getString("main.buttonQueue"));
        // tu peux ajouter d’autres boutons si tu en as
    }

}
