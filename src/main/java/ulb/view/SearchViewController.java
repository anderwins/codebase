package ulb.view;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ulb.controller.SearchType;
import ulb.i18n.LanguageManager;
import ulb.model.Track;
import ulb.view.utils.AlertManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SearchViewController extends PageViewController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<SearchType> searchTypeComboBox;
    @FXML private Label resultsLabel;
    @FXML private VBox tracksContainer;

    private static final Logger logger = Logger.getLogger(SearchViewController.class.getName());
    private final LanguageManager lang = LanguageManager.getInstance();
    private SearchObserver observer;

    public interface SearchObserver {
        void onSearch(String search, SearchType type);
    }

    public void setObserver(SearchObserver observer) {
        this.observer = observer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1️⃣ Initialise tous les textes
        updateTexts(resources);

        // 2️⃣ Configure le ComboBox pour contenir les enums et leurs libellés localisés
        configureCombo(resources);

        // 3️⃣ Réagit au changement de locale pour re-localiser à chaud
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            ResourceBundle bundle = lang.getResourceBundle();
            updateTexts(bundle);
            configureCombo(bundle);
        });

        // 4️⃣ Suggestions dynamiques à la frappe
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (observer != null && newText != null && !newText.isEmpty()) {
                observer.onSearch(newText, SearchType.ALL);
            } else {
                setResult(List.of());
            }
        });
    }

    /** Met à jour prompt et labels depuis le ResourceBundle */
    private void updateTexts(ResourceBundle bundle) {
        searchField.setPromptText(bundle.getString("search.promptText"));
        searchTypeComboBox.setPromptText(bundle.getString("search.selectType"));
        resultsLabel.setText(bundle.getString("search.results"));
    }

    /** Configure les items, le converter et la valeur par défaut du ComboBox */
    private void configureCombo(ResourceBundle bundle) {
        // mémorise l'ancienne sélection si possible
        SearchType previous = searchTypeComboBox.getValue();

        // items = tous les enums
        searchTypeComboBox.setItems(FXCollections.observableArrayList(SearchType.values()));

        // converter pour afficher les labels localisés
        searchTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SearchType type) {
                return bundle.getString("search.type." + type.name().toLowerCase());
            }
            @Override
            public SearchType fromString(String string) {
                for (SearchType t : SearchType.values()) {
                    if (bundle.getString("search.type." + t.name().toLowerCase()).equals(string)) {
                        return t;
                    }
                }
                return SearchType.ALL;
            }
        });

        // restaure la sélection précédente ou met ALL par défaut
        searchTypeComboBox.setValue(previous != null ? previous : SearchType.ALL);
    }

    /** Déclenché quand on valide le champ de recherche */
    @FXML
    public void handleSearchAction() {
        if (observer != null) {
            SearchType type = searchTypeComboBox.getValue() != null
                ? searchTypeComboBox.getValue()
                : SearchType.ALL;
            observer.onSearch(searchField.getText(), type);
        }
    }

    /** Affiche la liste des pistes dans le conteneur */
    public void setResult(List<Track> tracks) {
        try {
            tracksContainer.getChildren().clear();
            for (Track track : tracks) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/TrackView.fxml"),
                        lang.getResourceBundle()
                    );
                    VBox trackBox = loader.load();
                    TrackViewController ctrl = loader.getController();
                    ctrl.setTrack(track);
                    tracksContainer.getChildren().add(trackBox);
                } catch (IOException e) {
                    logger.warning("Erreur chargement TrackView.fxml : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur inattendue lors de l'affichage des résultats", e);
            AlertManager.showErrorWithException("Erreur inattendue",
                "Une erreur est survenue lors de l'affichage des résultats", e);
        }
    }
}
