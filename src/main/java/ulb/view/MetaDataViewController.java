package ulb.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ulb.controller.PageController;
import ulb.i18n.LanguageManager;
import ulb.model.Track;

import java.net.URL;
import java.util.ResourceBundle;

public class MetaDataViewController extends PageViewController implements Initializable {
    public interface MetaDataObserver {
        void onMetaDataSave();
        void onMetaDataCancel();
    }

    private MetaDataObserver observer;

    @FXML private Label headerLabel;

    @FXML private Label titleLabel;
    @FXML private TextField titleField;

    @FXML private Label artistLabel;
    @FXML private TextField artistField;

    @FXML private Label albumLabel;
    @FXML private TextField albumField;

    @FXML private Label yearLabel;
    @FXML private TextField yearField;

    @FXML private Label durationLabel;
    @FXML private TextField durationField;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final LanguageManager lang = LanguageManager.getInstance();
    private ResourceBundle bundle;

    private Track track;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bundle = lang.getResourceBundle();
        updateTexts(bundle);

        // mettre à jour à chaud si la locale change
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            bundle = lang.getResourceBundle();
            updateTexts(bundle);
        });
    }

    private void updateTexts(ResourceBundle b) {
        // titre
        headerLabel    .setText(b.getString("metadata.title"));

        // champs et labels
        titleLabel     .setText(b.getString("metadata.label.title"));
        titleField     .setPromptText(b.getString("metadata.prompt.title"));

        artistLabel    .setText(b.getString("metadata.label.artist"));
        artistField    .setPromptText(b.getString("metadata.prompt.artist"));

        albumLabel     .setText(b.getString("metadata.label.album"));
        albumField     .setPromptText(b.getString("metadata.prompt.album"));

        yearLabel      .setText(b.getString("metadata.label.year"));
        yearField      .setPromptText(b.getString("metadata.prompt.year"));

        durationLabel  .setText(b.getString("metadata.label.duration"));
        durationField  .setPromptText(b.getString("metadata.prompt.duration"));

        // boutons
        saveButton     .setText(b.getString("metadata.button.save"));
        cancelButton   .setText(b.getString("metadata.button.cancel"));
    }

    public void setObserver(MetaDataObserver observer) {
        this.observer = observer;
    }

    public void setTrack(Track original) {
        // on clone pour pouvoir annuler sans écraser
        this.track = new Track(original);
        titleField  .setText(original.getTitle());
        artistField .setText(original.getArtist());
        albumField  .setText(original.getAlbum());
        yearField   .setText(original.getYear());
        durationField.setText(String.valueOf(original.getDuration()));
    }

    @FXML
    public void handleSaveAction() {
        if (observer != null) observer.onMetaDataSave();
    }

    @FXML
    public void handleCancelAction() {
        if (observer != null) observer.onMetaDataCancel();
    }

    /** Récupère la version mise à jour depuis les champs */
    public Track getUpdatedTrack() {
        track.setTitle( titleField.getText() );
        track.setArtist( artistField.getText() );
        track.setAlbum( albumField.getText() );
        track.setYear( yearField.getText() );
        track.setDuration( Integer.parseInt(durationField.getText()) );
        return track;
    }
}
