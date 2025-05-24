// src/main/java/ulb/view/HomeViewController.java
package ulb.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import ulb.i18n.LanguageManager;
import javafx.scene.layout.VBox; 
import java.net.URL;
import java.util.ResourceBundle;

public class HomeViewController implements Initializable {

    public interface HomeObserver {
        void handleSongClick(String fileName);
        void searchByTag(String tag);
        void handlePlaylistClick(String playlistName);
    }

    private HomeObserver observer;
    private final LanguageManager lang = LanguageManager.getInstance();

    @FXML private Label welcomeLabel;
    @FXML private Label topGenresLabel;
    @FXML private Button tagPopButton;
    @FXML private Button tagRockButton;
    @FXML private Button tagHipHopButton;
    @FXML private Button tagElectronicButton;
    @FXML private Button tagJazzButton;
    @FXML private Button tagClassicalButton;

    @FXML private Label popularAlbumsLabel;
    @FXML private Label playlistOverlay1;
    @FXML private Label playlistTitleLabel1;
    @FXML private Label artistNameLabel1;
    @FXML private Label playlistOverlay2;
    @FXML private Label playlistTitleLabel2;
    @FXML private Label artistNameLabel2;
    @FXML private Label playlistOverlay3;
    @FXML private Label playlistTitleLabel3;
    @FXML private Label artistNameLabel3;

    @FXML private Label popularSongsLabel;
    @FXML private Label notImplementedLabel;

    public void setObserver(HomeObserver observer) {
        this.observer = observer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Texte initial
        updateTexts(resources);
        // Rafraîchir à chaud
        lang.localeProperty().addListener((o, oldLoc, newLoc) -> {
            updateTexts(lang.getResourceBundle());
        });
    }

    private void updateTexts(ResourceBundle b) {
        welcomeLabel.setText(b.getString("home.title"));
        topGenresLabel.setText(b.getString("home.topGenres"));
        tagPopButton.setText(b.getString("home.buttonPop"));
        tagRockButton.setText(b.getString("home.buttonRock"));
        tagHipHopButton.setText(b.getString("home.buttonHipHop"));
        tagElectronicButton.setText(b.getString("home.buttonElectronic"));
        tagJazzButton.setText(b.getString("home.buttonJazz"));
        tagClassicalButton.setText(b.getString("home.buttonClassical"));

        popularAlbumsLabel.setText(b.getString("home.popularAlbums"));
        playlistOverlay1.setText(b.getString("home.playlist1"));
        playlistTitleLabel1.setText(b.getString("home.playlistTitle1"));
        artistNameLabel1.setText(b.getString("home.artistName1"));
        playlistOverlay2.setText(b.getString("home.playlist2"));
        playlistTitleLabel2.setText(b.getString("home.playlistTitle2"));
        artistNameLabel2.setText(b.getString("home.artistName2"));
        playlistOverlay3.setText(b.getString("home.playlist3"));
        playlistTitleLabel3.setText(b.getString("home.playlistTitle3"));
        artistNameLabel3.setText(b.getString("home.artistName3"));

        popularSongsLabel.setText(b.getString("home.popularSongs"));
        notImplementedLabel.setText(b.getString("home.notImplemented"));
    }

    @FXML
    private void handleTagClick(MouseEvent event) {
        if (observer != null) {
            Button btn = (Button) event.getSource();
            observer.searchByTag(btn.getText());
        }
    }

    @FXML
    private void handlePlaylistClick(MouseEvent event) {
        if (observer != null) {
            VBox card = (VBox) event.getSource();
            // prend le titre sous le 2ᵉ enfant Label
            Label lbl = (Label) card.getChildren().get(1);
            observer.handlePlaylistClick(lbl.getText());
        }
    }
}
