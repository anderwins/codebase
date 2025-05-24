package ulb.view;

import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import ulb.i18n.LanguageManager;
import ulb.model.LrcLibService;
import ulb.model.KaraokeSynchronizer;

public class LyricsViewController extends PageViewController {
    private static final Logger logger = Logger.getLogger(LyricsViewController.class.getName());

    @FXML private ScrollPane lyricsScrollPane;
    @FXML private VBox     lyricsContainer;
    @FXML private Label    titleLabel;
    @FXML private Label    lyricsLabel;

    // Pour surligner la ligne en cours
    private Label currentLyricLabel;

    private LrcLibService lrcLibService;
    private List<KaraokeSynchronizer.LyricsLine> lyricsLines;
    private Timer timer;
    private int currentLineIndex = 0;

    private final LanguageManager lang   = LanguageManager.getInstance();
    private ResourceBundle      bundle;

    @FXML
    public void initialize() {
        bundle = lang.getResourceBundle();
        titleLabel.setText(bundle.getString("lyrics.title"));

        // Masqué au démarrage
        lyricsContainer.setVisible(false);
        lyricsContainer.setManaged(false);

        // i18n dynamique
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            bundle = lang.getResourceBundle();
            titleLabel.setText(bundle.getString("lyrics.title"));
        });
    }

    public void setLrcLibService(LrcLibService service) {
        this.lrcLibService = service;
    }
    
    @FXML
    public void toggleLyrics(List<KaraokeSynchronizer.LyricsLine> newLyricsLines) {
        logger.info("toggleLyrics called in LyricsViewController.");
        this.lyricsLines = newLyricsLines; // Update the internal list of lines

        stopKaraoke(); // Stop any active karaoke before changing displayed lyrics

        lyricsContainer.getChildren().clear(); // Always clear the container first

        if (this.lyricsLines == null || this.lyricsLines.isEmpty()) {
            logger.info("Lyrics lines are empty or null. Displaying 'no lyrics' message.");
            Label noLyricsMsg = new Label(bundle.getString("lyrics.notFound"));
            noLyricsMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
            lyricsContainer.getChildren().add(noLyricsMsg);
            // Ensure the VBox container for lyrics is visible and managed
            lyricsContainer.setVisible(true);
            lyricsContainer.setManaged(true);
        } else {
            logger.info("Populating lyrics container with " + this.lyricsLines.size() + " lines.");
            for (KaraokeSynchronizer.LyricsLine line : this.lyricsLines) {
                Label lineLabel = new Label(line.getLine());
                lineLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
                lyricsContainer.getChildren().add(lineLabel);
            }
            // Ensure the VBox container for lyrics is visible and managed
            lyricsContainer.setVisible(true);
            lyricsContainer.setManaged(true);

            Platform.runLater(() -> {
                if (lyricsScrollPane != null) {
                    lyricsScrollPane.setVvalue(0); // Scroll to top
                }
            });
        }
        // The PlayerViewController controls the actual visibility of lyricsContainerPlaceholder (which holds this view).
        // The internal isLyricsVisible flag (if kept) should ideally reflect if lyricsContainer has content vs. a placeholder message.
    }

    private boolean isLyricsEmpty() {
        return lyricsLines == null || lyricsLines.isEmpty();
    }

    /** Lance le karaoké (surbrillance progressive). */
    public void startKaraoke(long currentTimeMs) {
        if (lyricsLines == null || lyricsLines.isEmpty()) {
            logger.warning("⚠️ lyricsLines est vide ou non initialisé");
            return;
        }
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        currentLineIndex = 0;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updateLyrics(currentTimeMs));
            }
        }, 0, 500);
    }

    /** Met en surbrillance la ligne courante selon currentTime. */
    @FXML
    public void updateLyrics(long currentTime) {
        if (lyricsContainer.getChildren().isEmpty() || lyricsLines == null) return;
        // Avance l'index au bon endroit
        while (currentLineIndex < lyricsLines.size() - 1
            && lyricsLines.get(currentLineIndex + 1).getTimestamp() <= currentTime) {
            currentLineIndex++;
        }
        // Dé-surligne l'ancienne ligne
        if (currentLyricLabel != null) {
            currentLyricLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
        }
        // Surligne la nouvelle
        currentLyricLabel = (Label) lyricsContainer.getChildren().get(currentLineIndex);
        currentLyricLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: yellow;");
        // Scroll automatique
        double scrollValue = (double) currentLineIndex / lyricsContainer.getChildren().size();
        lyricsScrollPane.setVvalue(scrollValue);
    }

    /** Affiche un message « recherche en cours ». */
    private void showSearching() {
        Platform.runLater(() -> {
            lyricsContainer.getChildren().clear();
            Label msg = new Label(bundle.getString("lyrics.searching"));
            msg.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
            lyricsContainer.getChildren().add(msg);
            lyricsContainer.setVisible(true);
            lyricsContainer.setManaged(true);
        });
    }

    /** Affiche un message « pas de paroles ». */
    private void showNoLyrics() {
        Platform.runLater(() -> {
            lyricsContainer.getChildren().clear();
            Label msg = new Label(bundle.getString("lyrics.notFound"));
            msg.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
            lyricsContainer.getChildren().add(msg);
            lyricsContainer.setVisible(true);
            lyricsContainer.setManaged(true);
        });
    }

    /** Affiche un message « pas de karaoké disponible ». */
    public void messageNoKaraoke() {
        Platform.runLater(() -> {
            lyricsContainer.getChildren().clear();
            Label msg = new Label(bundle.getString("lyrics.noKaraoke"));
            msg.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
            lyricsContainer.getChildren().add(msg);
            lyricsContainer.setVisible(true);
            lyricsContainer.setManaged(true);
        });
    }

    /** Arrête le karaoké. */
    public void stopKaraoke() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        currentLineIndex = 0;
    }

    /** 
     * Returns the lyrics container VBox for external management.
     * @return The VBox containing lyrics content
     */
    public VBox getActualLyricsNode() {
        return lyricsContainer;
    }
}
