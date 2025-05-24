package ulb.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ulb.i18n.LanguageManager;
import ulb.model.Track;

import java.util.ResourceBundle;

public class QueueTrackViewController {
    
    @FXML
    private Label titleLabel;
    
    @FXML
    private Label artistLabel;
    
    @FXML
    private Label yearLabel;
    
    @FXML
    private Label durationLabel;
    
    @FXML
    private Button removeButton;
    
    @FXML
    private HBox root;
    
    private Track track;
    private RemoveTrackListener removeListener;
    private PlayTrackListener playTrackListener;

    // 🔄 Internationalisation
    private final LanguageManager lang = LanguageManager.getInstance();
    private ResourceBundle bundle;

    // ================================
    // 🔹 LISTENERS
    // ================================
    public interface RemoveTrackListener {
        void onRemoveTrack(Track track);
    }
    
    public interface PlayTrackListener {
        void onPlayTrack(Track track);
    }

    // ================================
    // 🔄 INITIALISATION
    // ================================
    @FXML
    public void initialize() {
        // Chargement initial des textes
        bundle = lang.getResourceBundle();
        updateStaticTexts();

        // 🔄 Mise à jour lors du changement de langue
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            bundle = lang.getResourceBundle();
            updateStaticTexts();
            // updateFields(); // Track specific fields should re-bind if track is set again or view rebuilt
        });

        // 🔥 Bouton de suppression
        removeButton.setOnAction(event -> {
            if (removeListener != null && track != null) {
                removeListener.onRemoveTrack(track);
            }
        });
        
        // 🔥 Clique sur le VBox contenant les informations du morceau pour le jouer.
        // This assumes the VBox is a direct child of root and is intended for playing the track.
        // A more robust way would be to assign an fx:id to the VBox in FXML and inject it here.
        for (javafx.scene.Node node : root.getChildren()) {
            if (node instanceof VBox) { // Assuming this VBox is the main clickable area for track info
                node.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 1 && playTrackListener != null && track != null) { // Single click to play
                        playTrackListener.onPlayTrack(track);
                    }
                });
                break; // Found the VBox, no need to check further children of root.
            }
        }
    }

    // ================================
    // 🔄 MISE À JOUR DES TEXTES
    // ================================
    private void updateStaticTexts() {
        if (bundle == null) bundle = lang.getResourceBundle(); // Ensure bundle is loaded
        removeButton.setText(bundle.getString("queue.track.remove"));
        // Other static texts in QueueTrackView.fxml (if any) would be updated here.
        // Track-specific labels like title, artist, year, duration are updated in updateFields().
    }

    private void updateFields() {
        if (track != null) {
            titleLabel.setText(track.getTitle());
            artistLabel.setText(track.getArtist());
            yearLabel.setText(String.valueOf(track.getYear())); // Year is int
            durationLabel.setText(formatDuration(track.getDuration())); // Duration is int (seconds)
        }
    }

    private String formatDuration(int totalSecs) {
        long minutes = totalSecs / 60;
        long seconds = totalSecs % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ================================
    // 🔄 MÉTHODES PUBLIQUES
    // ================================
    public void setTrack(Track track) {
        this.track = track;
        updateFields();
    }
    
    public Track getTrack() {
        return track;
    }
    
    public void setRemoveListener(RemoveTrackListener listener) {
        this.removeListener = listener;
    }
    
    public void setPlayTrackListener(PlayTrackListener listener) {
        this.playTrackListener = listener;
    }
    
    public void setPlaying(boolean isPlaying) {
        if (isPlaying) {
            if (!root.getStyleClass().contains("currently-playing")) {
                root.getStyleClass().add("currently-playing");
            }
            // Style the number label (it's the first child of root, added by QueueViewController)
            if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof Label numLabel) {
                if (!numLabel.getStyleClass().contains("currently-playing-number")) {
                     numLabel.getStyleClass().add("currently-playing-number");
                }
            }
        } else {
            root.getStyleClass().removeAll("currently-playing");
            // Style the number label
            if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof Label numLabel) {
                numLabel.getStyleClass().removeAll("currently-playing-number");
            }
        }
    }
}