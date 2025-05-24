package ulb.view;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import ulb.Config;
import ulb.controller.TrackController;
import ulb.i18n.LanguageManager;

import ulb.model.Playlist;
import ulb.model.PlaylistManager;
import ulb.model.PlaylistsObserver;
import ulb.model.Track;
import ulb.services.AppServices;

import java.util.List;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TrackViewController implements Track.TrackObserver, PlaylistsObserver {

    // ================================
    // 🔹 ATTRIBUTS FXML
    // ================================
    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label yearLabel;
    @FXML private Label durationLabel;
    @FXML private Menu playlistsMenu;
    @FXML private ImageView coverImageView;

    // 🔥 Les nouveaux MenuItems récupérés du FXML
    @FXML private MenuItem menuItemChangeMetadata;
    @FXML private MenuItem menuItemAddToQueue;
    @FXML private MenuItem menuItemImportLyrics;

    // ================================
    // 🔹 LOGIQUE ET CONTRÔLEUR
    // ================================
    public static final Logger logger = Logger.getLogger(TrackViewController.class.getName());
    private Track track;
    private TrackController controller;

    // ================================
    // 🔹 INTERNATIONALISATION (I18n)
    // ================================
    private final LanguageManager lang = LanguageManager.getInstance();
    private ResourceBundle bundle;

    // ================================
    // 🔄 INITIALISATION
    // ================================
    @FXML
    public void initialize() {
        // Charger le bundle de langue
        bundle = lang.getResourceBundle();
        updateStaticTexts();
        refreshContextMenuItems();

        // Écouter le changement de langue
        lang.localeProperty().addListener((obs, oldLoc, newLoc) -> {
            bundle = lang.getResourceBundle();
            updateStaticTexts();
            refreshContextMenuItems();
        });

        // S'abonner au PlaylistManager
        PlaylistManager.getInstance().addObserver(this);

        // Charger les playlists dans le menu
        updatePlaylistsMenu();
    }

    // ================================
    // 🔄 MISE À JOUR DES TEXTES STATIQUES
    // ================================
    private void updateStaticTexts() {
        titleLabel.setText(bundle.getString("track.titlePlaceholder"));
        artistLabel.setText(bundle.getString("track.artistPlaceholder"));
        yearLabel.setText(bundle.getString("track.yearLabel"));
        playlistsMenu.setText(bundle.getString("track.menu.addToPlaylist"));
    }

    // ================================
    // 🔄 MISE À JOUR DES INFORMATIONS DE LA PISTE
    // ================================
    private void updateFields() {
        titleLabel.setText(track.getTitle());
        artistLabel.setText(track.getArtist());
        yearLabel.setText(track.getYear());
        durationLabel.setText(track.getDuration().toString());

        // Chargement de l'image de couverture
        try {
            if (track.getCoverPath() != null && !track.getCoverPath().isEmpty()) {
                Image coverImage = new Image(new java.io.FileInputStream(track.getCoverPath()));
                coverImageView.setImage(coverImage);
            } else {
                Image defaultCover = new Image(getClass().getResourceAsStream("/default_cover_image/default_cover_image.jpg"));
                coverImageView.setImage(defaultCover);
            }
        } catch (Exception e) {
            logger.warning("Error loading cover image: " + e.getMessage());
            try {
                Image defaultCover = new Image(getClass().getResourceAsStream("/default_cover_image/default_cover_image.jpg"));
                coverImageView.setImage(defaultCover);
            } catch (Exception ex) {
                logger.severe("Could not load default cover image: " + ex.getMessage());
            }
        }
    }

    // ================================
    // 🔄 MISE À JOUR DES MENUS
    // ================================
    public void updatePlaylistsMenu() {
        setUpPlaylistsMenu(PlaylistManager.getInstance().getPlaylists());
    }

    public void handleMetaData() {
        controller.onMetadataClick();
    }

    public void handleAddToQueue() {
        controller.onAddToQueue();
    }

    private void setUpPlaylistsMenu(List<Playlist> playlists) {
        playlistsMenu.getItems().clear();

        for (Playlist playlist : playlists) {
            CheckBox item = getAddToPlaylistCheckBox(playlist);
            CustomMenuItem itemWrapper = new CustomMenuItem(item);
            itemWrapper.setHideOnClick(false);
            playlistsMenu.getItems().add(itemWrapper);
        }
    }

    private CheckBox getAddToPlaylistCheckBox(Playlist playlist) {
        CheckBox item = new CheckBox(playlist.getTitle());
        item.setOnAction(event -> {
            if (item.isSelected()) {
                this.handleAddToPlaylist(playlist);
            } else {
                this.handleRemoveOfPlaylist(playlist);
            }
        });
        return item;
    }

    // ================================
    // 🔄 RAFRAÎCHISSEMENT DES CONTEXT MENUS
    // ================================
    private void refreshContextMenuItems() {
        // 🔄 Mise à jour des textes des MenuItems
        menuItemChangeMetadata.setText(bundle.getString("track.menu.changeMetadata"));
        menuItemAddToQueue.setText(bundle.getString("track.menu.addToQueue"));
        menuItemImportLyrics.setText(bundle.getString("track.menu.importLyrics"));
    }

    // ================================
    // 🔄 MÉTHODES DE CONTRÔLE
    // ================================
    public void setController(TrackController c) {
        this.controller = c;
    }

    public void setTrack(Track track) {
        logger.info("Track ajouté : " + track.getTitle());
        this.track = track;
        this.track.addObserver(this);
        this.updateFields();
        updatePlaylistsMenu();
    }

    public void handleTrackClick() {
        controller.onTrackClick();
    }

    public void handleAddToPlaylist(Playlist playlist) {
        playlist.addTrack(track);
        AppServices.getDbInsert().addTrackToPlaylist(playlist.getTitle(), track.getTitle());
    }

    public void handleRemoveOfPlaylist(Playlist playlist) {
        logger.info("remove from playlist " + playlist.getTitle());
    }

    // ================================
    // 🔄 GESTION DES LYRICS
    // ================================
    public void handleLyrics() {
        FileChooser fileChoose = new FileChooser();
        fileChoose.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(bundle.getString("track.fileChooser.lrcFilter"), "*.lrc"),
            new FileChooser.ExtensionFilter(bundle.getString("track.fileChooser.txtFilter"), "*.txt")
        );
        fileChoose.setTitle(bundle.getString("track.fileChooser.title"));
        File file = fileChoose.showOpenDialog(null);
        if (file != null) {
            saveLRCFile(file);
        }
    }

    private void saveLRCFile(File file) {
        logger.info("Reading LRC file");
        String targetDirectory = Config.getFullPathFromRelative(Config.LYRICS_TRACKS_DIRECTORY);

        String sanitizedTitle = track.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_");
        String extension = file.getName().endsWith(".txt") ? ".txt" : ".lrc";
        String newFileName = sanitizedTitle.endsWith(extension) ? sanitizedTitle : sanitizedTitle + extension;

        Path targetPath = Path.of(targetDirectory, newFileName);
        logger.info("saving to " + targetPath);
        try {
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            track.setKaraokePath(targetPath.toString());
        } catch (IOException e) {
            logger.warning("Erreur lors de l'enregistrement du fichier.");
        }
    }

    @Override
    public void onChangeData(Track t) {
        updateFields();
    }

    @Override
    public void onPlaylistAdded(Playlist playlist) {
        updatePlaylistsMenu();
    }

    @Override
    public void onPlaylistRemoved(Playlist playlist) {
        updatePlaylistsMenu();
    }

    public void removeCheckFromCheckBox(Playlist playlist) {
        for (MenuItem item : playlistsMenu.getItems()) {
            if (item instanceof CustomMenuItem) {
                CheckBox checkBox = (CheckBox) ((CustomMenuItem) item).getContent();
                if (checkBox.getText().equals(playlist.getTitle())) {
                    checkBox.setSelected(false);
                }
            }
        }
    }


    @Override
    public void onPlaylistAltered(Playlist playlist) {
        removeCheckFromCheckBox(playlist);
    }
}
