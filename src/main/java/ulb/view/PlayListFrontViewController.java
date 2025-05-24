package ulb.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import ulb.GuiMain;
import ulb.controller.PlayListFrontController;
import ulb.i18n.LanguageManager;
import ulb.model.Playlist;
import ulb.model.PlaylistManager;
import ulb.view.utils.AlertManager;

public class PlayListFrontViewController {
    private static final Logger logger = Logger.getLogger(PlayListFrontViewController.class.getName());

    @FXML private Label title;
    @FXML private ImageView albumCover;
    @FXML private Rectangle albumBackground;
    @FXML private StackPane playIconContainer;

    
    @FXML private MenuItem addCoverItem;
    @FXML private MenuItem deletePlaylistItem;


    private PlayListFrontController controller;

    /** Appelé par FXMLLoader */
    @FXML
    public void initialize() {
        // Survol de l'album
        StackPane container = (StackPane) albumCover.getParent();
        container.setOnMouseEntered(e -> {
            playIconContainer.setVisible(true);
            albumBackground.setOpacity(0.7);
        });
        container.setOnMouseExited(e -> {
            playIconContainer.setVisible(false);
            albumBackground.setOpacity(1.0);
        });

        // i18n des menuitems
        updateMenuItems();

        // 3️⃣ Réagir au changement de langue à l'exécution
        LanguageManager.getInstance()
                    .localeProperty()
                    .addListener((obs, oldLoc, newLoc) -> updateMenuItems());

        // Cover éventuelle
        loadCoverImage();
    }

    /** Injecté par PlaylistPageViewController */
    public void setController(PlayListFrontController controller) {
        this.controller = controller;
    }

    /** Met à jour les libellés des MenuItem */
    private void updateMenuItems() {
        ResourceBundle b = LanguageManager.getInstance().getResourceBundle();
        addCoverItem.setText(b.getString("playlist.menu.add_cover"));
        deletePlaylistItem.setText(b.getString("playlist.menu.delete_playlist"));
    }

    @FXML
    public void handlePlaylistClick(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlaylistView.fxml"));
            loader.setResources(LanguageManager.getInstance().getResourceBundle());
            Parent playlistView = loader.load();
            GuiMain.playlistController.setPlaylistWithTitle(title.getText());
            GuiMain.mainController.goToPlaylist();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'ouverture de la playlist", e);
            AlertManager.showErrorWithException("Erreur d'ouverture de playlist", 
                "Impossible d'ouvrir la playlist " + title.getText(), e);
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE, "Fichier FXML de la playlist introuvable", e);
            AlertManager.showErrorWithException("Ressource manquante", 
                "Fichier de l'interface graphique de la playlist introuvable", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur inattendue lors de l'ouverture de la playlist", e);
            AlertManager.showErrorWithException("Erreur inattendue", 
                "Une erreur est survenue lors de l'ouverture de la playlist", e);
        }
    }


    public void setTitle(String t) {
        title.setText(t);
        loadCoverImage();
    }

    public String getTitle() {
        return title.getText();
    }

    private void saveCover(File file) {
        try {
            String dir = "src/main/resources/images/";
            Files.createDirectories(Path.of(dir));
            String clean = title.getText().replaceAll("[^\\w.-]", "_");
            Path dst = Path.of(dir, clean + ".jpg");
            Files.copy(file.toPath(), dst, StandardCopyOption.REPLACE_EXISTING);
            albumCover.setImage(new Image(dst.toUri().toString()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde de l'image", e);
            AlertManager.showErrorWithException("Erreur de sauvegarde", 
                "Impossible d'enregistrer l'image de la pochette", e);
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "Permission insuffisante pour sauvegarder l'image", e);
            AlertManager.showErrorWithException("Erreur de permission", 
                "Droits insuffisants pour enregistrer l'image", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur inattendue lors de la sauvegarde de l'image", e);
            AlertManager.showErrorWithException("Erreur inattendue", 
                "Une erreur est survenue lors de l'enregistrement de l'image", e);
        }
    }

    private void setDefaultCoverImage() {
        albumCover.setImage(null);
    }

    private void loadCoverImage() {
        String clean = title.getText().replaceAll("[^\\w.-]", "_");
        File img = new File("src/main/resources/images/" + clean + ".jpg");
        if (img.exists()) {
            albumCover.setImage(new Image(img.toURI().toString()));
            updateTextColor(Color.WHITE);
        } else {
            setDefaultCoverImage();
        }
    }
    
    /**
     * Met à jour la couleur du texte en fonction de la couleur de fond.
     */
    private void updateTextColor(Color color) {
        title.setTextFill(color);
    }

    /**
     * Gère la suppression de la playlist après confirmation.
     */
    public void handleDeletePlaylist() {
        String playlistTitle = title.getText();
        boolean confirmed = AlertManager.showConfirmation(
            "Suppression de playlist",
            "Êtes-vous sûr de vouloir supprimer la playlist '" + playlistTitle + "' ?",
            "Supprimer", "Annuler");
            
        if (confirmed) {
            Playlist playlist = PlaylistManager.getInstance().findPlaylistWithTitle(playlistTitle);
            if (playlist != null) {
                PlaylistManager.getInstance().removePlaylist(playlist);
                ulb.services.AppServices.getDbUpdate().deletePlaylist(title.getText());
            }
        }
    }
    
    /**
     * Gère l'ajout d'une image de couverture à la playlist.
     */
    @FXML
    public void handleCover() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de couverture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(title.getScene().getWindow());
        if (selectedFile != null) {
            saveCover(selectedFile);
            // Mettre à jour la couleur du texte pour la visibilité
            updateTextColor(Color.WHITE);
        }
    }
}
