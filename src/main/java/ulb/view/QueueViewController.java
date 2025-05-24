package ulb.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ulb.model.Queue;
import ulb.model.Track;
import ulb.i18n.LanguageManager;
import ulb.view.utils.AlertManager;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;

public class QueueViewController extends PageViewController implements Initializable {

    @FXML private Label   titleLabel;
    @FXML private Button  playButton;
    @FXML private Button  clearButton;
    @FXML private Label   sectionTitleLabel;
    @FXML private VBox    trackContainer;
    @FXML private Label   emptyQueueLabel;
    @FXML private Text    queueStatus;

    private final LanguageManager lang = LanguageManager.getInstance();
    private static final Logger logger = Logger.getLogger(QueueViewController.class.getName());

    /** Observateur pour déléguer les actions sur la file */
    public interface Observer {
        void addTrack(Track track);
        void removeTrack(Track track);
        void clearTracks();
        void playQueue();
        void playTrack(Track track);
    }
    private Observer observer;

    /** La piste en cours de lecture */
    private Track currentlyPlayingTrack = null;
    /** La queue actuellement affichée (pour re-populer après changement de locale) */
    private Queue currentQueue;
    /** Map to store HBox row to its controller */
    private Map<HBox, QueueTrackViewController> rowToControllerMap = new HashMap<>();

    /** Getter utilisé par QueueController */
    public Track getCurrentlyPlayingTrack() {
        return currentlyPlayingTrack;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Texte initial
        updateTexts(resources);

        // Réagir au changement de langue : relocaliser tous les labels + reformater la queue
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            ResourceBundle b = lang.getResourceBundle();
            updateTexts(b);
            if (currentQueue != null) {
                doPopulateQueue(currentQueue);
            }
        });
    }

    /** Met à jour tous les libellés statiques depuis le bundle */
    private void updateTexts(ResourceBundle b) {
        titleLabel       .setText(b.getString("queue.title"));
        playButton       .setText(b.getString("queue.buttonPlay"));
        clearButton      .setText(b.getString("queue.buttonClear"));
        sectionTitleLabel.setText(b.getString("queue.sectionTitle"));
        emptyQueueLabel  .setText(b.getString("queue.empty"));
        // queueStatus est mis à jour dynamiquement dans doPopulateQueue()
    }

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    @FXML public void handlePlayQueueClick()  { 
        if (observer != null) observer.playQueue(); 
    }
    @FXML public void handleClearQueueClick() { 
        if (observer != null) observer.clearTracks(); 
    }

    /**
     * Initialise la vue avec la queue complète.
     */
    public void setQueue(Queue queue) {
        this.currentQueue = queue;
        doPopulateQueue(queue);
    }

    /**
     * (Re)construit l’affichage de la queue : liste des pistes,
     * label “vide” et statut formaté.
     */
    private void doPopulateQueue(Queue queue) {
        trackContainer.getChildren().clear();
        rowToControllerMap.clear(); // Clear map when repopulating
        List<Track> tracks = queue.getTracks();
        ResourceBundle bundle = lang.getResourceBundle(); // Get current bundle for FXML loading & status

        // Affiche / masque le message “file vide”
        emptyQueueLabel.setVisible(tracks.isEmpty());

        // Statut “N pistes dans la file d’attente”
        String fmt = bundle.getString("queue.statusFormat");
        queueStatus.setText(String.format(fmt, tracks.size()));

        // Construire chaque ligne
        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/QueueTrackView.fxml"),
                    bundle // Pass bundle to QueueTrackView
                );
                HBox trackRow = loader.load();
                QueueTrackViewController ctl = loader.getController();
                rowToControllerMap.put(trackRow, ctl); // Store controller instance

                ctl.setTrack(track);
                if (observer != null) { // Check if observer is set before assigning listeners
                    ctl.setRemoveListener(observer::removeTrack);
                    ctl.setPlayTrackListener(observer::playTrack);
                }

                // Numéro de piste
                Label num = new Label(String.format("%02d", i + 1));
                num.getStyleClass().add("track-number");
                num.setMinWidth(30); // Ensure consistent width
                trackRow.getChildren().add(0, num); // Add number as the first child

                // Marquer la piste en cours via le controller de la ligne
                boolean isPlayingThisTrack = currentlyPlayingTrack != null
                                 && currentlyPlayingTrack.getTrackId() == track.getTrackId();
                ctl.setPlaying(isPlayingThisTrack);

                trackRow.setUserData(track); // For identification (still useful for other purposes if needed)
                trackContainer.getChildren().add(trackRow);

            } catch (IOException e) {
                logger.log(Level.WARNING, "Erreur chargement QueueTrackView.fxml : " + e.getMessage(), e);
                AlertManager.showErrorWithException("Erreur d'affichage", 
                    "Impossible de charger l'interface pour une piste dans la file d'attente", e);
            } catch (Exception e) { // Catch any other unexpected errors during row creation
                 logger.log(Level.SEVERE, "Erreur inattendue lors de la création de la ligne de piste pour: " + track.getTitle(), e);
                 AlertManager.showErrorWithException("Erreur Inattendue", 
                    "Une erreur critique est survenue lors de l'affichage de la file d'attente.", e);
            }
        }
    }

    /**
     * Met à jour uniquement la piste en cours de lecture (visuellement)
     * sans reconstruire toute la liste. Appelé par le QueueController.
     */
    public void setCurrentlyPlayingTrack(Track track) {
        this.currentlyPlayingTrack = track;
        for (Map.Entry<HBox, QueueTrackViewController> entry : rowToControllerMap.entrySet()) {
            QueueTrackViewController ctl = entry.getValue();
            Track rowTrack = ctl.getTrack(); // Assumes getTrack() exists in QueueTrackViewController
            
            boolean isPlayingThisRow = false;
            if (track != null && rowTrack != null && rowTrack.getTrackId() == track.getTrackId()) {
                isPlayingThisRow = true;
            }
            ctl.setPlaying(isPlayingThisRow);
        }
    }
}
