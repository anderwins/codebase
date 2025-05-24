package ulb.view;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import ulb.controller.PlaylistController;
import ulb.i18n.LanguageManager;
import ulb.model.Playlist;
import ulb.model.Track;
import ulb.utils.ColorExtractor;
import ulb.view.utils.AlertManager;

public class PlaylistViewController extends PageViewController {

    // === Les champs injectés depuis le FXML ===
    @FXML private Label title;
    @FXML private Label trackCount;
    @FXML private Label totalDuration;
    @FXML private ImageView playlistCover;
    @FXML private StackPane headerBackground;
    @FXML private VBox trackContainer;

    @FXML private Button playButton;
    @FXML private Button clearButton;
    @FXML private ToggleButton shuffleButton;

    @FXML private Label headerTitle;
    @FXML private Label headerAlbum;
    @FXML private Label headerDuration;

    // === I18n & état ===
    private final LanguageManager lang = LanguageManager.getInstance();
    private ResourceBundle bundle;

    // === Contrôleurs et observateur ===
    public interface Observer {
        void addTrack(Track track);
        void removeTrack(Track track);
        void clearTracks();
        void playPlaylist();
        void reorderTracks(Track track, int fromIndex, int toIndex);
        void setShuffleEnabled(boolean shuffleEnabled);
        boolean isPlaying();
    }
    private Observer observer;
    private PlaylistController playlistController;

    private static final Logger logger = Logger.getLogger(PlaylistViewController.class.getName());

    /** Appelé par FXMLLoader */
    @FXML
    public void initialize() {
        // 1️⃣ Texte initial
        bundle = lang.getResourceBundle();
        updateStaticTexts();

        // 2️⃣ Réagir aux changements de langue
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            bundle = lang.getResourceBundle();
            updateStaticTexts();
        });
    }

    /** Met à jour tous les textes statiques (boutons, en-têtes) */
    private void updateStaticTexts() {
        playButton    .setText(bundle.getString("playlist.buttonPlay"));
        clearButton   .setText(bundle.getString("playlist.buttonClear"));

        boolean on = shuffleButton.isSelected();
        shuffleButton .setText(bundle.getString(on
            ? "playlist.shuffleOn"
            : "playlist.shuffleOff"));

        headerTitle   .setText(bundle.getString("playlist.headerTitle"));
        headerAlbum   .setText(bundle.getString("playlist.headerAlbum"));
        headerDuration.setText(bundle.getString("playlist.headerDuration"));
    }

    /** Injecte l'observateur métier */
    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    /** Injecte le contrôleur pour l'état initial du shuffle */
    public void setPlaylistController(PlaylistController controller) {
        this.playlistController = controller;
        if (controller != null) {
            boolean enabled = controller.isShuffleEnabled();
            shuffleButton.setSelected(enabled);
            shuffleButton.setText(bundle.getString(enabled
                ? "playlist.shuffleOn"
                : "playlist.shuffleOff"));
        }
    }

    /** Clic sur PLAY */
    @FXML
    public void handlePlayPlaylistClick() {
        if (observer != null) observer.playPlaylist();
    }

    /** Clic sur CLEAR */
    @FXML
    public void handleClearPlaylistClick() {
        if (observer != null) observer.clearTracks();
    }

    /** Bascule SHUFFLE ON/OFF */
    @FXML
    public void handleShuffle() {
        boolean enabled = shuffleButton.isSelected();
        if (observer != null) {
            observer.setShuffleEnabled(enabled);
            shuffleButton.setText(bundle.getString(enabled
                ? "playlist.shuffleOn"
                : "playlist.shuffleOff"));
            if (observer.isPlaying()) observer.playPlaylist();
        }
    }

    /** Initialise l'affichage de la playlist chargée */
    public void setPlayList(Playlist playlist) {
        title.setText(playlist.getTitle());
        loadCoverImage();
        trackContainer.getChildren().clear();
        List<Track> tracks = playlist.getTracks();
        updatePlaylistInfo(tracks);
        trackContainer.setUserData(playlist);
        populateTrackList(tracks);
    }

    /** Met à jour compteur de pistes + durée totale */
    private void updatePlaylistInfo(List<Track> tracks) {
        trackCount.setText(tracks.size()
            + " "
            + bundle.getString("playlist.tracksSuffix"));

        int tot = calculateTotalDuration(tracks);
        long hrs = TimeUnit.SECONDS.toHours(tot);
        long mins = TimeUnit.SECONDS.toMinutes(tot) % 60;

        String fmtKey = hrs > 0
            ? "playlist.duration.format.hrs"
            : "playlist.duration.format.min";
        String fmt = bundle.getString(fmtKey);

        totalDuration.setText(
            hrs > 0
            ? String.format(fmt, hrs, mins)
            : String.format(fmt, mins)
        );
    }

    private int calculateTotalDuration(List<Track> tracks) {
        return tracks.stream()
                     .mapToInt(Track::getDuration)
                     .sum();
    }

    /** Construit la liste des pistes */
    private void populateTrackList(List<Track> tracks) {
        int trackNumber = 1;
        for (Track track : tracks) {
            try {
                HBox trackRow = createTrackRow(track, trackNumber);
                setupDragHandling(trackRow);
                setupDragDropHandling(trackRow);
                setupEventHandlers(trackRow, track);
                trackContainer.getChildren().add(trackRow);
                trackNumber++;
            } catch (NullPointerException e) {
                logger.log(Level.WARNING, "Erreur lors de la création de la ligne (objet null)", e);
                AlertManager.showErrorWithException("Erreur d'affichage", 
                    "Impossible d'afficher la piste dans la playlist", e);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Erreur lors du traitement des données de la piste (argument invalide)", e);
                AlertManager.showErrorWithException("Erreur de données", 
                    "Les données de la piste sont invalides", e);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erreur inattendue lors de l'ajout de la piste à la liste", e);
                AlertManager.showErrorWithException("Erreur inattendue", 
                    "Une erreur est survenue lors de l'affichage de la piste", e);
            }
        }
    }

    private HBox createTrackRow(Track track, int number) {
        HBox row = new HBox(10);
        row.getStyleClass().add("track-row");

        Label num = new Label(String.valueOf(number));
        num.getStyleClass().add("track-number");
        num.setPrefWidth(30);

        VBox info = new VBox(5);
        info.getStyleClass().add("track-info");
        Label t1 = new Label(track.getTitle());
        t1.getStyleClass().add("track-title");
        Label t2 = new Label(track.getArtist());
        t2.getStyleClass().add("track-artist");
        info.getChildren().addAll(t1, t2);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label album = new Label(track.getAlbum());
        album.getStyleClass().add("track-album");
        album.setPrefWidth(150);

        Label dur = new Label(formatTime(track.getDuration()));
        dur.getStyleClass().add("track-duration");
        dur.setPrefWidth(80);

        Button drag = new Button("≡");
        drag.getStyleClass().add("drag-handle");

        row.getChildren().addAll(num, info, album, dur, drag);
        row.setUserData(track);
        return row;
    }

    private String formatTime(int sec) {
        long m = TimeUnit.SECONDS.toMinutes(sec);
        long s = sec % 60;
        return String.format("%02d:%02d", m, s);
    }

    /** Gestion du glisser-déposer */
    private void setupDragHandling(HBox row) {
        Button handle = (Button)row.getChildren().get(4);
        handle.setOnDragDetected(e -> {
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent c = new ClipboardContent();
            c.putString(String.valueOf(trackContainer.getChildren().indexOf(row)));
            db.setContent(c);
            db.setDragView(row.snapshot(null,null));
            e.consume();
        });
    }

    /** More complete drag-drop event handling */
    private void setupDragDropHandling(HBox targetRow) {
        targetRow.setOnDragOver(event -> {
            if (event.getGestureSource() != targetRow && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        targetRow.setOnDragEntered(event -> {
            if (event.getGestureSource() != targetRow) {
                targetRow.setStyle("-fx-background-color:#e0e0e0;");
            }
            event.consume();
        });

        targetRow.setOnDragExited(event -> {
            targetRow.setStyle("");
            event.consume();
        });

        targetRow.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int draggedIndex = Integer.parseInt(db.getString());
                int targetIndex = trackContainer.getChildren().indexOf(targetRow);
                
                if (draggedIndex != -1 && targetIndex != -1 && draggedIndex != targetIndex) {
                    processDragDrop(draggedIndex, targetIndex);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        setupDragDoneHandling(targetRow);
    }

    private void processDragDrop(int draggedIndex, int targetIndex) {
        if (observer != null) {
            Track draggedTrack = (Track)((HBox)trackContainer.getChildren().get(draggedIndex)).getUserData();
            observer.reorderTracks(draggedTrack, draggedIndex, targetIndex);
        }
    }

    private void setupDragDoneHandling(HBox trackRow) {
        trackRow.setOnDragDone(event -> {
            event.consume();
        });
    }

    /** Event handlers */
    private void setupEventHandlers(HBox trackRow, Track track) {
        setupClickHandler(trackRow);
        setupContextMenu(trackRow, track);
    }

    private void setupClickHandler(HBox trackRow) {
        trackRow.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (observer != null) {
                    observer.playPlaylist();
                }
            }
        });
    }

    private void setupContextMenu(HBox trackRow, Track track) {
        trackRow.setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem(bundle.getString("playlist.context.remove"));
            deleteItem.setOnAction(e -> {
                if (observer != null) {
                    observer.removeTrack(track);
                }
            });
            contextMenu.getItems().add(deleteItem);
            contextMenu.show(trackRow, event.getScreenX(), event.getScreenY());
        });
    }

    /** Chargement / affichage de la pochette */
    private void loadCoverImage() {
        String safe = title.getText().replaceAll("[^a-zA-Z0-9_.-]", "_");
        File f = new File("src/main/resources/images/" + safe + ".jpg");
        if (f.exists()) {
            Image img = new Image(f.toURI().toString());
            playlistCover.setImage(img);
            try {
                Color dom = ColorExtractor.extractDominantColor(img);
                setHeaderGradient(dom, Color.web("#fafafa"));
                updateTextColor(dom);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to extract dominant color, using default.", e);
                setDefaultCover();
            }
        } else {
            setDefaultCover();
        }
    }

    private void setHeaderGradient(Color a, Color b) {
        String css = String.format(
            "-fx-background-color: linear-gradient(to bottom, %s, %s);",
            toRGB(a), toRGB(b)
        );
        headerBackground.setStyle(css);
    }

    private String toRGB(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed()*255),
            (int)(c.getGreen()*255),
            (int)(c.getBlue()*255)
        );
    }

    private void updateTextColor(Color bg) {
        double lum = 0.299*bg.getRed() + 0.587*bg.getGreen() + 0.114*bg.getBlue();
        String col = lum>0.5 ? "black" : "white";
        title       .setStyle("-fx-text-fill:" + col + ";");
        trackCount  .setStyle("-fx-text-fill:" + col + ";");
        totalDuration.setStyle("-fx-text-fill:" + col + ";");
    }

    private void setDefaultCover() {
        Color a = Color.web("#e0e6f0"), b = Color.web("#d8e2ff");
        playlistCover.setImage(createGradientImage(a,b));
        setHeaderGradient(a,b);
        title        .setStyle("-fx-text-fill:black;");
        trackCount   .setStyle("-fx-text-fill:#333333;");
        totalDuration.setStyle("-fx-text-fill:#333333;");
    }

    private Image createGradientImage(Color a, Color b) {
        Rectangle r = new Rectangle(300,300);
        r.setFill(new LinearGradient(0,0,1,1,true,
            CycleMethod.NO_CYCLE,new Stop(0,a),new Stop(1,b)));
        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        return r.snapshot(p,null);
    }
}
