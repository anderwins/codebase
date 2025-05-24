package ulb.controller;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ulb.dao.DbManagerInsert;
import ulb.model.Playlist;
import ulb.model.PlaylistManager; 
import ulb.model.PlaylistObserver;
import ulb.model.Track;
import ulb.view.PlaylistViewController;
import ulb.view.utils.AlertManager;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Contrôleur responsable de la gestion des playlists.
 * Permet d'ajouter/supprimer des morceaux d'une playlist et de gérer la vue associée.
 */
public class PlaylistController extends PageController implements PlaylistViewController.Observer, PlaylistObserver {
    /** Playlist actuellement sélectionnée */
    private Playlist playlist;
    /** Contrôleur de la vue de playlist (final car injecté au constructeur) */
    private final PlaylistViewController playlistView;
    /** Gestionnaire d'insertion/édition en base de données (final, injecté) */
    private final DbManagerInsert dbManagerInsert;
    /** Contrôleur du lecteur audio (final, injecté) */
    private final AudioPlayerController playerController;
    /** Gestionnaire des playlists (injecté) */
    private final PlaylistManager playlistManager;

    private boolean shuffleEnabled = false;
    private List<Track> originalOrder;
    private List<Track> shuffledOrder;

    /** Runnable courant pour l'événement de fin de piste (permet de le supprimer si besoin) */
    private Runnable currentOnEndRunnable = null;

    /** Indique si une playlist est en cours de lecture */
    boolean isPlaying = false;

    private static final Logger logger = Logger.getLogger(PlaylistController.class.getName());

    /**
     * Constructeur principal du contrôleur de playlists.
     * Les dépendances sont injectées.
     *
     * @param viewController Contrôleur de la vue associée à la playlist.
     * @param dbManagerInsert Gestionnaire des insertions/updates en base de données.
     * @param playerController Contrôleur du lecteur audio.
     * @param playlistManager Gestionnaire des playlists.
     * @throws IllegalArgumentException si une dépendance est nulle.
     */
    public PlaylistController(PlaylistViewController viewController,
                              MainController mainController,
                              DbManagerInsert dbManagerInsert,
                              AudioPlayerController playerController,
                              PlaylistManager playlistManager) {
        super(mainController);

        if (viewController == null || dbManagerInsert == null || playerController == null || playlistManager == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }

        this.playlistView = viewController;
        this.dbManagerInsert = dbManagerInsert;
        this.playerController = playerController;
        this.playlistManager = playlistManager;
        this.playlistView.setObserver(this);
    }

    /**
     * Crée une nouvelle playlist à partir d'un titre.
     * @param title Titre de la nouvelle playlist
     * @return Le titre de la playlist créée
     */
    public String createNewPlaylistFromTitle(String title) {
        logger.info("create new playlist from controller");
        Playlist newPlaylist = new Playlist(title);
        // Add to manager first, so it's available in memory
        playlistManager.addPlaylist(newPlaylist);
        
        // Then attempt to save to DB
        if (!dbManagerInsert.insertPlaylist(title, "Admin")) { // "Admin" utilisé comme utilisateur par défaut
            logger.log(Level.SEVERE, "Échec de l\'insertion de la playlist '" + title + "' dans la base de données.");
            AlertManager.showError("Erreur Base de Données", "Impossible de sauvegarder la nouvelle playlist '" + title + "' en base de données.");
            
        } else {
            logger.log(Level.INFO, "Playlist '" + title + "' créée et sauvegardée en base de données.");
        }
        setPlaylist(newPlaylist); // Set as current playlist regardless of DB save for now
        return newPlaylist.getTitle();
    }

    /**
     * Définit la playlist courante à partir de son titre.
     * Si aucune playlist ne correspond, la playlist courante est mise à null.
     * @param title Titre de la playlist à sélectionner
     */
    public void setPlaylistWithTitle(String title) {
        logger.info("set playlist with title controller");
        Playlist foundPlaylist = playlistManager.findPlaylistWithTitle(title);
        if (foundPlaylist != null) {
            setPlaylist(foundPlaylist);
        } else {
            logger.log(Level.SEVERE, "Playlist with title '" + title + "' not found.");
            setPlaylist(null);
        }
    }

    /**
     * Définit la playlist courante et met à jour la vue.
     * @param playlist Nouvelle playlist à sélectionner (peut être null)
     */
    public void setPlaylist(Playlist playlist) {
        logger.info("set playlist controller");
        if (this.playlist != null) {
            this.playlist.removeObserver(this);
        }
        this.playlist = playlist;
        if (this.playlist != null) {
            this.playlist.addObserver(this);
        }
        playlistView.setPlayList(this.playlist);
    }

    /**
     * Retourne la playlist courante.
     * @return Playlist sélectionnée
     */
    protected Playlist getPlaylist() {
        return this.playlist;
    }

    /**
     * Lance la lecture de la playlist courante.
     * Si aucune playlist n'est sélectionnée ou si elle est vide, arrête le lecteur.
     */
    public void playPlaylist() {
        if (playlist == null || playlist.getTracks().isEmpty()) {
            logger.info("Cannot play: Playlist is null or empty.");
            playerController.stop();
            return;
        }
        logger.info("Playing PLAYLIST: " + playlist.getTitle());
        List<Track> trackList = getCurrentTrackOrder();
        if (trackList.isEmpty()) {
             logger.info("Playlist contains no tracks to play.");
             playerController.stop();
             return;
        }
        final int[] currentIndex = {0};
        clearPlayerEventHandlers();
        playerController.addOnPreviousAction(() -> {
            if (currentIndex[0] > 1) {
                currentIndex[0] -= 2;
                playTrackAtIndex(trackList, currentIndex);
            } else {
                playerController.seek(0);
            }
        });
        playerController.addOnNextAction(() -> {
             if (currentIndex[0] < trackList.size()) {
                 playTrackAtIndex(trackList, currentIndex);
             } else {
                 logger.info("End of playlist reached.");
                 playerController.stop();
                 isPlaying = false;
             }
        });
        playTrackAtIndex(trackList, currentIndex);
    }


    private List<Track> getCurrentTrackOrder() {
        if (shuffleEnabled) {
            // if shuffle is enabled, shuffle the tracks
            if (originalOrder == null || !originalOrder.equals(playlist.getTracks())) {
                originalOrder = new ArrayList<>(playlist.getTracks());
                //Collections.shuffle(playlist.getTracks());
                Collections.shuffle(originalOrder);
            }
            return originalOrder; // Return the shuffled order
        } else {
            originalOrder = null; // Reset original order if shuffle is disabled
            return new ArrayList<>(playlist.getTracks());
        }
    }

    @Override
    public void setShuffleEnabled(boolean enabled) {
        this.shuffleEnabled = enabled;
    }

    public boolean isShuffleEnabled() {
        return this.shuffleEnabled;
    }

    @Override
    public boolean isPlaying() {
        return this.isPlaying;
    }

    /**
     * Joue le morceau à l'index courant et prépare le passage au suivant.
     * @param trackList Liste des morceaux à jouer
     * @param currentIndex Index courant (tableau pour modification en lambda)
     */
    private void playTrackAtIndex(List<Track> trackList, int[] currentIndex) {
        if (currentIndex[0] < trackList.size()) {
            Track track = trackList.get(currentIndex[0]);
            logger.info("Playing track " + (currentIndex[0] + 1) + "/" + trackList.size() + ": " + track.getTitle());
            if (currentOnEndRunnable != null) {
                 playerController.removeOnEndEvent(currentOnEndRunnable);
            }
            currentOnEndRunnable = () -> {
                if (currentIndex[0] < trackList.size()) {
                    playTrackAtIndex(trackList, currentIndex);
                } else {
                    logger.info("Finished last track of playlist.");
                    isPlaying = false;
                    currentOnEndRunnable = null;
                }
            };
            playerController.addOnEndEvent(currentOnEndRunnable);
            playerController.play(track.getTrackId());
            isPlaying = true;
            currentIndex[0]++;
        } else {
             logger.info("Attempted to play beyond playlist bounds.");
             isPlaying = false;
             currentOnEndRunnable = null;
        }
    }

    /**
     * Nettoie tous les gestionnaires d'événements du lecteur audio.
     * Permet d'éviter les conflits d'écouteurs lors de changements de playlist.
     */
    private void clearPlayerEventHandlers() {
        if (currentOnEndRunnable != null) {
            playerController.removeOnEndEvent(currentOnEndRunnable);
            currentOnEndRunnable = null;
        }
        playerController.addOnEndEvent(() -> {});
        playerController.addOnPreviousAction(() -> {});
        playerController.addOnNextAction(() -> {});
        playerController.removeOnEndEvent(() -> {});
    }

    /**
     * Ajoute un morceau à la playlist sélectionnée.
     * @param track Le morceau à ajouter
     */
    @Override
    public void addTrack(Track track) {
        if (playlist != null && track != null) {
            if (!playlist.getTracks().contains(track)) {
                playlist.addTrack(track); // Add to in-memory list first
                // Then attempt to save to DB
                if (!dbManagerInsert.addTrackToPlaylist(this.playlist.getTitle(), track.getTitle())) {
                    logger.log(Level.SEVERE, "Échec de l\'ajout du morceau '" + track.getTitle() + "' à la playlist '" + this.playlist.getTitle() + "' en DB.");
                    AlertManager.showError("Erreur Base de Données", "Impossible d\'ajouter le morceau '" + track.getTitle() + "' à la playlist '" + this.playlist.getTitle() + "' en base de données.");
                    // Optionally, revert in-memory add:
                    // playlist.removeTrack(track);
                    // Or throw new RuntimeException(...)
                } else {
                    logger.log(Level.INFO, "Morceau '" + track.getTitle() + "' ajouté à la playlist '" + this.playlist.getTitle() + "' et sauvegardé.");
                }
            } else {
                logger.log(Level.INFO, "Le morceau '" + track.getTitle() + "' est déjà dans la playlist '" + this.playlist.getTitle() + "'.");
            }
        } else {
            logger.log(Level.WARNING, "Impossible d'ajouter le morceau : playlist ou morceau nul.");
        }
    }

    /**
     * Supprime un morceau de la playlist sélectionnée.
     * @param track Le morceau à supprimer
     */
    @Override
    public void removeTrack(Track track) {
        if (playlist != null && track != null) {
            // Check if the track is present before attempting removal from memory
            boolean wasPresentInMemory = playlist.getTracks().contains(track);

            if (wasPresentInMemory) {
                playlist.removeTrack(track); // Call the void method

                // Now, attempt to remove from the database
                if (!dbManagerInsert.removeTrackFromPlaylist(this.playlist.getTitle(), track.getTitle())) {
                    logger.log(Level.SEVERE, "Échec de la suppression du morceau '" + track.getTitle() + "' de la playlist '" + this.playlist.getTitle() + "' en DB.");
                    AlertManager.showError("Erreur Base de Données", "Impossible de supprimer le morceau '" + track.getTitle() + "' de la playlist '" + this.playlist.getTitle() + "' en base de données.");
                    // Note: Track is removed from memory even if DB removal fails. 
                    // Consider if this discrepancy needs further handling (e.g., add back to memory, though complex).
                } else {
                    logger.log(Level.INFO, "Morceau '" + track.getTitle() + "' supprimé de la playlist '" + this.playlist.getTitle() + "' (DB et mémoire).");
                }
            } else {
                 logger.log(Level.INFO, "Le morceau '" + track.getTitle() + "' n'était pas dans la playlist (mémoire) '" + this.playlist.getTitle() + "'. Pas de suppression de la DB tentée sur cette base.");
                 // If it wasn't in memory, we might not want to attempt a DB removal without further checks,
                 // or we might, if we suspect an inconsistency. For now, only act if it was in memory.
            }
        } else {
             logger.log(Level.WARNING, "Impossible de supprimer le morceau : playlist ou morceau nul.");
        }
    }

    /**
     * Supprime tous les morceaux de la playlist sélectionnée
     * et les retire également de la base de données.
     */
    @Override
    public void clearTracks() {
        if (playlist != null) {
            // Vérifier si la liste est vide avant de la vider
            if (playlist.getTracks().isEmpty()) {
                logger.log(Level.INFO, "La playlist '" + this.playlist.getTitle() + "' est déjà vide.");
                return; // Ne rien faire si la playlist est déjà vide
            }
            
            // Clear in-memory list first
            playlist.clearTracks();
            // Then attempt to clear in DB
            if (!dbManagerInsert.removeAllTracksFromPlaylist(this.playlist.getTitle())) {
                 logger.log(Level.SEVERE, "Échec de la suppression de tous les morceaux de la playlist '" + this.playlist.getTitle() + "' en DB.");
                 AlertManager.showError("Erreur Base de Données", "Impossible de vider la playlist '" + this.playlist.getTitle() + "' en base de données.");
                 // Reverting this (re-adding all tracks to memory) is complex and usually not desired if clear is called.
            } else {
                 logger.log(Level.INFO, "Tous les morceaux supprimés de la playlist '" + this.playlist.getTitle() + "' et de la DB.");
            }
        } else {
            logger.log(Level.WARNING, "Impossible de vider les morceaux : playlist nulle.");
        }
    }

    // --- Méthodes PlaylistObserver ---

    /**
     * Notifié lorsqu'un morceau est ajouté à la playlist.
     * @param trackId L'identifiant du morceau ajouté
     */
    @Override
    public void onAddTrack(Integer trackId) {
        logger.info("PlaylistObserver: onAddTrack notified.");
        // La vue est généralement notifiée par le modèle Playlist
    }

    /**
     * Notifié lorsqu'un morceau est retiré de la playlist.
     * @param trackId L'identifiant du morceau retiré
     */
    @Override
    public void onRemoveTrack(Integer trackId) {
        logger.info("PlaylistObserver: onRemoveTrack notified.");
    }

    /**
     * Notifié lorsque la playlist est vidée de tous ses morceaux.
     */
    @Override
    public void onClear() {
        logger.info("PlaylistObserver: onClear notified.");
    }

    // --- Méthodes PlaylistViewController.Observer ---

    /**
     * Réordonne un morceau dans la playlist (drag & drop dans la vue).
     * @param track     Le morceau à déplacer (optionnel)
     * @param fromIndex Index de départ
     * @param toIndex   Index d'arrivée
     */
    @Override
    public void reorderTracks(Track track, int fromIndex, int toIndex) {
        logger.info("View Observer: Reordering track from index " + fromIndex + " to " + toIndex);
        if (playlist != null) {
            playlist.reorderTrack(fromIndex, toIndex);
            playlistView.setPlayList(playlist);
        } else {
            logger.log(Level.SEVERE, "Cannot reorder tracks: Playlist is null.");
        }
    }
}