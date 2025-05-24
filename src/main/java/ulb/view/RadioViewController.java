package ulb.view;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import ulb.i18n.LanguageManager;
import ulb.model.Radio;

public class RadioViewController extends PageViewController {

    /** Ne pas renommer : utilisé par RadioController */
    public interface radioObserver {
        void onRadioSelected(String url);
    }

    private static final Logger logger = Logger.getLogger(RadioViewController.class.getName());

    @FXML private Label titleLabel;
    @FXML private VBox  radioContainer;

    private radioObserver observer;
    private ResourceBundle bundle;
    private final LanguageManager lang = LanguageManager.getInstance();

    @FXML
    public void initialize() {
        // initialisation du titre
        bundle = lang.getResourceBundle();
        titleLabel.setText(bundle.getString("radio.title"));

        // rafraîchir automatiquement si la locale change
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            bundle = lang.getResourceBundle();
            titleLabel.setText(bundle.getString("radio.title"));
        });
    }

    /** Liaison depuis RadioController */
    public void setObserver(radioObserver observer) {
        this.observer = observer;
    }

    /** Chargé depuis RadioController à l’ouverture */
    public void displayRadios(List<Radio> radios) {
        radioContainer.getChildren().clear();
        for (Radio radio : radios) {
            Button btn = new Button(radio.getTitle());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                if (observer != null) {
                    observer.onRadioSelected(radio.getStreamUrl());
                }
            });
            radioContainer.getChildren().add(btn);
        }
    }
}
