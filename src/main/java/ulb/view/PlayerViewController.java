package ulb.view;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import ulb.Config;
import ulb.i18n.LanguageManager;
import ulb.model.KaraokeSynchronizer;
import ulb.model.Playlist;
import ulb.model.Track;
import ulb.view.utils.AlertManager;

/**
 * Controller class responsible for handling audio player UI.
 * It reports user event to a @code {PlayerViewObserver}
 */
public class PlayerViewController {

    /**
     * Interface for the observer pattern
     */
    public interface PlayerViewObserver {
        void onPlayPause();
        void onPrevious();
        void onNext();
        void onVolumeChange(double value);
        void onSeek(double progress);
        void onSeekStart(double progress);
        void onSeekEnd(double progress);
        List<KaraokeSynchronizer.LyricsLine> showLyrics(Track track);
        long getCurrentTime();
        void playFile(String audioPath);
        void onBalanceAudio(double balance);
        void setPlaySpeed(double speed);
        void setFade();
    }

    // ================================
    // 🔄 ATTRIBUTS FXML
    // ================================
    @FXML private Button playPauseButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Slider volumeSlider;
    @FXML private Slider progressSlider;
    @FXML private Label currentTime;
    @FXML private Label totalTime;
    @FXML private Label trackTitle;
    @FXML private Label artistName;
    @FXML private ImageView playIconView;
    @FXML private ImageView pauseIconView;
    @FXML private Button karaokeButton;
    @FXML private Button lyricsButton;
    @FXML private Slider balanceSlider;
    @FXML private ImageView albumArt;
    @FXML private ScrollPane lyricsContainerPlaceholder;
    @FXML private VBox mainContainer;
    @FXML private ComboBox<String> speedComboBox;


    @FXML private Button visualizerButton;
    @FXML private StackPane visualizerPane;

    @FXML private Label volumeMinLabel;
    @FXML private Label volumeMaxLabel;
    @FXML private Label balanceLeftLabel;
    @FXML private Label balanceRightLabel;


    // ================================
    // 🔄 ATTRIBUTS DE CLASSE
    // ================================
    private PlayerViewObserver observer;
    private LyricsViewController lyricsController;
    private Track currentTrack;
    private String default_cover_image = Config.DEFAULT_COVER_IMAGE;
    private boolean iskaraokeVisible = false;
    private final double NORMAL_HEIGHT = 180;
    private final double EXPANDED_HEIGHT = 400;
    private static final Logger logger = Logger.getLogger(PlayerViewController.class.getName());
    private Playlist playlist;
    private final double[] speedValues = {0.5, 0.7, 1.0, 1.5, 2.0};
    private final int numBands = 65;
    private Rectangle[] bars;

    // 🔄 Internationalisation
    private final LanguageManager lang = LanguageManager.getInstance();
    private ResourceBundle bundle;

    // Forward declarations of methods to fix linter errors
    /**
     * Définit l'image par défaut.
     */
    private void setDefaultCover() {
        File f = new File(default_cover_image);
        albumArt.setImage(new Image(f.toURI().toString()));
    }

    /**
     * Met à jour la pochette depuis l'objet Track.
     */
    public void updateTrackInfoPicture(Track track) {
        if (track == null) return;
        if (track.getCoverPath() != null && !track.getCoverPath().isEmpty()) {
            File f = new File(track.getCoverPath());
            if (f.exists()) {
                albumArt.setImage(new Image(f.toURI().toString()));
            } else {
                setDefaultCover();
            }
        } else {
            setDefaultCover();
        }
    }

    private void animateLyricsContainer(boolean show) {
        if (show) {
            if (!lyricsContainerPlaceholder.isVisible()) {
                lyricsContainerPlaceholder.setVisible(true);
                lyricsContainerPlaceholder.setManaged(true);
                Timeline timeline = new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(mainContainer.prefHeightProperty(), EXPANDED_HEIGHT)));
                timeline.play();
            }
        } else {
            if (lyricsContainerPlaceholder.isVisible()) {
                lyricsContainerPlaceholder.setVisible(false);
                lyricsContainerPlaceholder.setManaged(false);
                Timeline timeline = new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(mainContainer.prefHeightProperty(), NORMAL_HEIGHT)));
                timeline.play();
            }
        }
    }

    @FXML
    private void handleLyrics() {
        if (currentTrack == null) {
            logger.warning("Tentative d'affichage des paroles sans morceau sélectionné.");
            AlertManager.showWarning("Paroles non disponibles", "Aucune chanson n'est en cours de lecture.");
            if (visualizerPane.isVisible()) { // Ensure visualizer is hidden
                visualizerPane.setVisible(false);
                visualizerPane.setManaged(false);
                // Height will be handled by animateLyricsContainer(false)
            }
            animateLyricsContainer(false); // Ensure lyrics panel is hidden & height is normal
            if (lyricsController != null) {
                lyricsController.toggleLyrics(java.util.Collections.emptyList()); // Clear lyrics
            }
            return;
        }

        List<KaraokeSynchronizer.LyricsLine> lyricsLines = observer.showLyrics(currentTrack);

        if (lyricsLines == null || lyricsLines.isEmpty()) {
            logger.info("Aucunes paroles trouvées pour '" + currentTrack.getTitle() + "'. La vue des paroles ne sera pas ouverte/affichée.");
            if (visualizerPane.isVisible()) { // Ensure visualizer is hidden
                visualizerPane.setVisible(false);
                visualizerPane.setManaged(false);
            }
            animateLyricsContainer(false); // Ensure lyrics panel is hidden & height is normal
            if (lyricsController != null) {
                lyricsController.toggleLyrics(java.util.Collections.emptyList()); // Clear lyrics
            }
            return;
        }

        // Lyrics ARE available. Now, toggle the view and update lyrics controller.
        if (lyricsContainerPlaceholder.isVisible()) {
            // It's visible, so user wants to hide it
            animateLyricsContainer(false);
            if (lyricsController != null) {
                lyricsController.toggleLyrics(java.util.Collections.emptyList());
            }
            // Visualizer state remains unchanged when only hiding lyrics.
        } else {
            // It's hidden, so user wants to show it.
            // FIRST, ensure visualizer is hidden if it's active.
            if (visualizerPane.isVisible()) {
                visualizerPane.setVisible(false);
                visualizerPane.setManaged(false);
                // Height will be handled by animateLyricsContainer(true) for lyrics panel
            }

            if (lyricsController != null) {
                lyricsController.toggleLyrics(lyricsLines); // Load and show new lyrics
                animateLyricsContainer(true); // Show lyrics container (and expand mainContainer)
            } else {
                logger.warning("LyricsController est null. Impossible d'afficher les paroles pour " + currentTrack.getTitle());
                AlertManager.showError("Erreur d'affichage", "Le composant d'affichage des paroles n'a pas pu être initialisé.");
                if (visualizerPane.isVisible()) { // Ensure visualizer is hidden
                    visualizerPane.setVisible(false);
                    visualizerPane.setManaged(false);
                }
                animateLyricsContainer(false); // Ensure lyrics panel is hidden & height is normal
            }
        }
    }

    /**
     * Affiche ou cache le visualizer.
     */
    @FXML
    public void showVizualizer() {
        boolean showVis = !visualizerPane.isVisible(); // Target state for visualizer

        if (showVis) { // If we are about to show the visualizer (turn ON)
            if (lyricsContainerPlaceholder.isVisible()) {
                // Hide lyrics panel components directly to avoid conflicting animations on mainContainer height
                lyricsContainerPlaceholder.setVisible(false);
                lyricsContainerPlaceholder.setManaged(false);
                
                VBox actualLyricsVBox = (lyricsController != null) ? lyricsController.getActualLyricsNode() : null;
                if (actualLyricsVBox != null) {
                   actualLyricsVBox.setVisible(false);
                   actualLyricsVBox.setManaged(false);
                }

                if (lyricsController != null) {
                    // Explicitly clear lyrics content from lyricsController as its panel is being hidden
                    lyricsController.toggleLyrics(java.util.Collections.emptyList());
                }
            }
        }
        // These actions are common for both turning ON and OFF the visualizer.
        // Visibility of the visualizer pane itself:
        visualizerPane.setVisible(showVis);
        visualizerPane.setManaged(showVis);

        // Animate main container height based on visualizer's new state.
        // If visualizer is shown (showVis = true), height becomes EXPANDED_HEIGHT.
        // If visualizer is hidden (showVis = false), height becomes NORMAL_HEIGHT.
        Timeline t = new Timeline(new KeyFrame(Duration.millis(200),
            new KeyValue(mainContainer.prefHeightProperty(),
                showVis ? EXPANDED_HEIGHT : NORMAL_HEIGHT)));
        t.play();
    }

    // ================================
    // 🔄 INITIALISATION
    // ================================
    @FXML
    private void initialize() {
        mainContainer.setPrefHeight(NORMAL_HEIGHT);

        // Charger le bundle et mettre à jour les textes statiques
        bundle = lang.getResourceBundle();
        updateStaticTexts();

        // Lister les changements de langue
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            bundle = lang.getResourceBundle();
            updateStaticTexts();
        });

        setupEventHandlers();

        // Icônes pause/play
        playIconView.setVisible(true);
        playIconView.setManaged(true);
        pauseIconView.setVisible(false);
        pauseIconView.setManaged(false);

        // Chargement du LyricsView
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LyricsView.fxml"), bundle);
            Parent lyricsContent = loader.load();
            lyricsController = loader.getController();
            if (lyricsController == null) {
                logger.warning("Error: pas de LyricsViewController");
            } else {
                lyricsContainerPlaceholder.setContent(lyricsContent);
                lyricsContainerPlaceholder.setVisible(false);
                lyricsContainerPlaceholder.setManaged(false);
            }

            lyricsContainerPlaceholder.setContent(lyricsContent);
            lyricsContainerPlaceholder.setVisible(false);
            lyricsContainerPlaceholder.setManaged(false);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur de chargement de LyricsView.fxml", e);
            AlertManager.showErrorWithException("Erreur d'interface", 
                "Impossible de charger la vue des paroles", e);
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE, "Erreur de référence nulle", e);
            AlertManager.showErrorWithException("Erreur d'interface", 
                "Une référence à un élément manquant a été détectée", e);
        }

        // Initialisation du ComboBox de vitesse
        for (double speed : speedValues) {
            speedComboBox.getItems().add("x" + speed);
        }
        speedComboBox.setValue("x1.0");

        // Initialisation du visualizer
        bars = new Rectangle[numBands];
        visualizerPane.getChildren().clear();
        HBox visualizerBars = new HBox(2);
        visualizerBars.setAlignment(Pos.CENTER);
        visualizerBars.setTranslateY(-20);
        visualizerBars.setTranslateX(-10);
        for (int i = 0; i < numBands; i++) {
            Rectangle bar = new Rectangle(5, 100);
            bar.setFill(Color.ROYALBLUE);
            bars[i] = bar;
            visualizerBars.getChildren().add(bar);
        }
        visualizerPane.getChildren().add(visualizerBars);
    }

    /**
     * Met à jour tous les textes statiques (labels et boutons) depuis le ResourceBundle.
     */
    private void updateStaticTexts() {
        // Texte par défaut quand aucune piste sélectionnée
        trackTitle.setText(bundle.getString("player.noTrackPlaying"));
        artistName.setText(bundle.getString("player.artistPlaceholder"));
        currentTime.setText(bundle.getString("player.time.zero"));
        totalTime.setText(bundle.getString("player.time.zero"));

        // Boutons
        playPauseButton.setText(bundle.getString("player.button.playPause"));
        prevButton.setText(bundle.getString("player.button.previous"));
        nextButton.setText(bundle.getString("player.button.next"));
        karaokeButton.setText(bundle.getString("player.button.karaoke"));
        lyricsButton.setText(bundle.getString("player.button.lyrics"));
        visualizerButton.setText(bundle.getString("player.button.visualizer"));

        // Volume et balance
        volumeMinLabel.setText(bundle.getString("player.volume.min"));
        volumeMaxLabel.setText(bundle.getString("player.volume.max"));
        balanceLeftLabel.setText(bundle.getString("player.balance.left"));
        balanceRightLabel.setText(bundle.getString("player.balance.right"));
    }

    /**
     * Définit l'observateur qui recevra les événements utilisateur.
     */
    public void setObserver(PlayerViewObserver audioPlayerObserver) {
        observer = audioPlayerObserver;
    }

    /**
     * Met en place tous les gestionnaires d'événements sur les contrôles UI.
     */
    private void setupEventHandlers() {
        playPauseButton.setOnAction(e -> handlePlayPause());
        prevButton.setOnAction(e -> handlePrevious());
        nextButton.setOnAction(e -> handleNext());
        volumeSlider.valueProperty().addListener((obs, o, n) -> handleVolumeChange(n.doubleValue()));
        progressSlider.setOnMousePressed(this::handleSliderPress);
        progressSlider.setOnMouseReleased(this::handleSeek);
        progressSlider.setOnMouseDragged(this::handleSliderDrag);
        balanceSlider.valueProperty().addListener((obs, o, n) -> observer.onBalanceAudio(n.doubleValue()));
        visualizerButton.setOnAction(e -> showVizualizer());
        karaokeButton.setOnAction(e -> {
            handleLyrics();
            iskaraokeVisible = lyricsContainerPlaceholder.isVisible();
        });
        lyricsButton.setOnAction(e -> {
            handleLyrics();
            iskaraokeVisible = false;
        });
    }

    /**
     * Gère le clic sur le slider de progression (début du drag).
     */
    public void handleSliderPress(MouseEvent event) {
        observer.onSeekStart(progressSlider.getValue() / 100);
    }

    /**
     * Gère le drag sur le slider de progression.
     */
    private void handleSliderDrag(MouseEvent event) {
        observer.onSeek(progressSlider.getValue() / 100);
    }

    /**
     * Gère la fin du drag (relâchement) sur le slider de progression.
     */
    @FXML
    public void handleSeek(MouseEvent event) {
        observer.onSeekEnd(progressSlider.getValue() / 100);
    }

    /**
     * Gère le clic sur le bouton Play/Pause.
     */
    @FXML
    public void handlePlayPause() {
        observer.onPlayPause();
    }

    /**
     * Gère le clic sur le bouton Précédent.
     */
    @FXML
    public void handlePrevious() {
        observer.onPrevious();
        resetBalance();
        resetPlaybackSpeed();
    }

    /**
     * Gère le clic sur le bouton Suivant.
     */
    @FXML
    public void handleNext() {
        observer.onNext();
        resetBalance();
        resetPlaybackSpeed();
    }

    /**
     * Gère le changement de valeur du slider de volume.
     */
    public void handleVolumeChange(double value) {
        observer.onVolumeChange(value / 100);
    }

    /**
     * Met à jour la position du slider de volume depuis l'extérieur.
     */
    public void updateVolume(double volume) {
        volumeSlider.setValue(volume * 100);
    }

    /**
     * Met à jour le slider de progression et le label currentTime.
     */
    public void updateProgress(double progress, double time) {
        Platform.runLater(() -> {
            currentTime.setText(formatTime((long) time));
            progressSlider.setValue(progress * 100);

            if (iskaraokeVisible && lyricsController != null && observer != null && currentTrack != null) {
                try {
                    String karaokePath = currentTrack.getKaraokePath();
                    // Vérification que karaokePath n'est pas null avant d'appeler endsWith
                    if (karaokePath != null) {
                        if (karaokePath.endsWith(".lrc")) {
                            // Only start karaoke sync, do NOT reload/toggle lyrics on every progress update
                            lyricsController.startKaraoke(observer.getCurrentTime());
                        } else if (karaokePath.endsWith(".txt")) {
                            lyricsController.messageNoKaraoke();
                        }
                    } else {
                        // Si karaokePath est null, afficher un message approprié une seule fois
                        if (iskaraokeVisible) {
                            iskaraokeVisible = false; // Empêcher d'afficher le message à chaque frame
                            logger.warning("Chemin de fichier karaoké non défini pour la piste actuelle");
                            AlertManager.showWarning("Karaoké non disponible", 
                                "Cette piste n'a pas de fichier karaoké associé");
                            lyricsController.messageNoKaraoke();
                        }
                    }
                } catch (Exception e) {
                    // Capturer toute autre exception pour éviter les crashs pendant la lecture
                    logger.log(Level.WARNING, "Erreur lors de la mise à jour des paroles", e);
                    iskaraokeVisible = false;
                }
            }
        });
    }

    /**
     * Change l'icône du bouton Play/Pause selon l'état.
     */
    public void updatePlayPause(boolean isInPause) {
        playIconView.setVisible(isInPause);
        playIconView.setManaged(isInPause);
        pauseIconView.setVisible(!isInPause);
        pauseIconView.setManaged(!isInPause);
    }

    /**
     * Change la piste affichée ou réinitialise si null.
     */
    public void changeTrack(Track track) {
        boolean lyricsWereVisible = lyricsContainerPlaceholder.isVisible(); // Keep this to know original state for track == null case.

        this.currentTrack = track;
        if (track == null) {
            artistName.setText("");
            trackTitle.setText(bundle.getString("player.noTrackPlaying"));
            totalTime.setText(bundle.getString("player.time.zero"));
            progressSlider.setValue(0);
            currentTime.setText(bundle.getString("player.time.zero"));
            setDefaultCover();

            // If lyrics were visible, hide them.
            if (lyricsWereVisible) {
                animateLyricsContainer(false);
            }
            // Always clear lyrics if controller exists.
            if (lyricsController != null) {
                lyricsController.toggleLyrics(java.util.Collections.emptyList());
                lyricsController.stopKaraoke(); // Ensure karaoke is stopped
            }
            iskaraokeVisible = false; // Reset karaoke visibility
        } else {
            artistName.setText(track.getArtist());
            trackTitle.setText(track.getTitle());
            totalTime.setText(formatTime((long) track.getDuration() * 1000));
            progressSlider.setValue(0);
            updateTrackInfoPicture(track);

            // Always hide lyrics panel and clear lyrics on new track load.
            // The handleLyrics() method will be responsible for fetching and showing them on demand.
            if (lyricsContainerPlaceholder.isVisible()) {
                animateLyricsContainer(false); // Hide the panel
            }
            if (lyricsController != null) {
                lyricsController.toggleLyrics(java.util.Collections.emptyList()); // Clear any old lyrics
                lyricsController.stopKaraoke(); // Stop any ongoing karaoke
            }
            iskaraokeVisible = false; // Reset karaoke visibility flag
        }
    }

    /**
     * Formate un temps (ms) en "MM:SS".
     */
    private String formatTime(long time) {
        long m = TimeUnit.MILLISECONDS.toMinutes(time);
        long s = TimeUnit.MILLISECONDS.toSeconds(time) % 60;
        return String.format("%02d:%02d", m, s);
    }

    /**
     * Définit la playlist courante.
     */
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    /**
     * Change la vitesse de lecture depuis le ComboBox.
     */
    @FXML
    private void setSpeed() {
        String sel = speedComboBox.getValue();
        double v = Double.parseDouble(sel.substring(1));
        observer.setPlaySpeed(v);
    }

    /**
     * Réinitialise la vitesse de lecture à 1.0.
     */
    public void resetPlaybackSpeed() {
        speedComboBox.setValue("x1.0");
        observer.setPlaySpeed(1.0);
    }

    /**
     * Réinitialise le balance slider à 0.
     */
    public void resetBalance() {
        balanceSlider.setValue(0);
        observer.onBalanceAudio(0);
    }

    /**
     * Lie le MediaPlayer pour le visualizer.
     */
    public void bindMediaPlayer(javafx.scene.media.MediaPlayer mediaPlayer) {
        mediaPlayer.setAudioSpectrumInterval(0.03);
        mediaPlayer.setAudioSpectrumNumBands(numBands);
        mediaPlayer.setAudioSpectrumListener((ts, dur, mags, phs) ->
            Platform.runLater(() -> updateVisualizer(mags)));
    }

    /**
     * Met à jour les barres du visualizer.
     */
    private void updateVisualizer(float[] magnitudes) {
        int center = numBands / 2;
        for (int i = 0; i < numBands && i < magnitudes.length; i++) {
            double h = Math.max(5, (60 + magnitudes[i]) * 2);
            int l = center - i, r = center + i;
            if (l >= 0) bars[l].setHeight(h);
            if (r < numBands) bars[r].setHeight(h);
        }
    }

    /**
     * Réinitialise le panneau lyrics/visualizer.
     */
    public void resetLyricsVisualizerWindow() {
        if (visualizerPane.isVisible()) {
            visualizerPane.setVisible(false);
            visualizerPane.setManaged(false);
        }
        if (lyricsContainerPlaceholder.isVisible()) {
            lyricsContainerPlaceholder.setVisible(false);
            lyricsContainerPlaceholder.setManaged(false);
        }
        Timeline t = new Timeline(new KeyFrame(Duration.millis(200),
            new KeyValue(mainContainer.prefHeightProperty(), NORMAL_HEIGHT)));
        t.play();
    }
    public void setFade(){
        observer.setFade();
    }

}
