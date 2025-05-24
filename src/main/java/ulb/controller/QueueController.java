package ulb.controller;

import ulb.GuiMain;
import ulb.model.Queue;
import ulb.model.Track;
import ulb.view.QueueViewController;

import java.util.Iterator;
import java.util.List;

/**
 * Contrôleur responsable de la gestion de la file d'attente (queue).
 * Permet d'ajouter, supprimer, lire et réordonner les morceaux dans la file d'attente.
 */
public class QueueController extends PageController implements QueueViewController.Observer {

    /** File d'attente courante */
    private Queue queue;
    /** Contrôleur de la vue associée à la file d'attente */
    private QueueViewController viewController;
    /** Indique si la file d'attente est en cours de lecture */
    private boolean isPlaying = false;

    /**
     * Constructeur du contrôleur de file d'attente.
     * @param viewController Contrôleur de la vue associée
     */
    public QueueController(Queue q, QueueViewController viewController, MainController mainController) {
        super(mainController);
        this.viewController = viewController;
        this.viewController.setObserver(this);
        this.queue = q;
        this.viewController.setQueue(queue);
    }

    /**
     * Vide la file d'attente et arrête la lecture.
     */
    @Override
    public void clearTracks() {
        if (queue != null) {
            boolean wasQueuePlayerActive = isPlaying || (viewController.getCurrentlyPlayingTrack() != null);

            queue.clearTracks();
            viewController.setQueue(queue);

            if (wasQueuePlayerActive) {
                GuiMain.audioPlayerController.reset();
                if (GuiMain.audioPlayerController.getView() != null) { // Ensure view is not null
                    GuiMain.audioPlayerController.getView().changeTrack(null);
                }
            }
            viewController.setCurrentlyPlayingTrack(null);
            isPlaying = false;
        }
    }

    /**
     * Ajoute un morceau à la file d'attente et lance la lecture si nécessaire.
     * @param track Le morceau à ajouter
     */
    @Override
    public void addTrack(Track track) {
        if (queue != null) {
            queue.addTrack(track);
            viewController.setQueue(queue);

            if (!isPlaying) {
                isPlaying = true;
                playQueue();
            }
        }
    }

    /**
     * Supprime un morceau de la file d'attente.
     * @param track Le morceau à supprimer
     */
    @Override
    public void removeTrack(Track track) {
        if (queue != null && track != null) {
            Track currentlyPlayingTrackInQueueView = viewController.getCurrentlyPlayingTrack();
            boolean wasCurrentlyPlayingThisTrack = currentlyPlayingTrackInQueueView != null &&
                                                   currentlyPlayingTrackInQueueView.getTrackId() == track.getTrackId();

            queue.removeTrack(track);        // 1. Remove from the model
            viewController.setQueue(queue); // 2. Update the queue's visual representation

            if (wasCurrentlyPlayingThisTrack) {
                // 3. If the removed track was the one the queue view considered playing
                GuiMain.audioPlayerController.reset(); // Stop audio, clear internal player state, doesn't run onEnd
                if (GuiMain.audioPlayerController.getView() != null) { // Ensure view is not null
                    GuiMain.audioPlayerController.getView().changeTrack(null); // Explicitly clear PlayerView's track display
                }

                List<Track> remainingTracks = queue.getTracks();
                if (!remainingTracks.isEmpty()) {
                    // Play the new first track. This will also set isPlaying = true (done below)
                    // and update viewController.setCurrentlyPlayingTrack() via playTrackAtIndex.
                    playTrackAtIndex(0);
                    this.isPlaying = true; // Mark queue as actively playing.
                } else {
                    // Queue is now empty
                    this.isPlaying = false;
                    viewController.setCurrentlyPlayingTrack(null); // Ensure queue view is cleared
                }
            }
            // If a different track was removed (not the one playing), or no track was playing,
            // just update the queue view. Playback of the (unaffected) current track continues.
            // The onEnd handler of the current track will correctly pick the next track from the modified queue.
        }
    }

    /**
     * Joue un morceau unique : vide la file d'attente, ajoute le morceau et lance la lecture.
     * @param track Le morceau à jouer
     */
    public void playSingleTrack(Track track) {
        // clearTracks will stop/reset player, clear queue model and view, and set isPlaying to false.
        this.clearTracks(); 
        if (queue != null) { // queue is an instance variable, should exist.
            queue.addTrack(track);
            viewController.setQueue(queue);
            // playQueue will start playback of the single track, set isPlaying = true,
            // and set up the necessary player events.
            playQueue();
        }
    }
    
    /**
     * Joue un morceau spécifique de la file d'attente.
     * @param track Le morceau à jouer
     */
    @Override
    public void playTrack(Track track) {
        List<Track> tracks = queue.getTracks();
        for (int i = 0; i < tracks.size(); i++) {
            Track queueTrack = tracks.get(i);
            if (queueTrack.getTrackId() == track.getTrackId()) {
                viewController.setCurrentlyPlayingTrack(track);
                GuiMain.audioPlayerController.play(track.getTrackId());
                isPlaying = true; // Ensure isPlaying is true when a track is explicitly played
                final Track trackToPlayOnEnd = track; // Capture the track for the lambda
                GuiMain.audioPlayerController.addOnEndEvent(() -> {
                    if (queue != null) { // Check if queue still exists
                        // Original logic: remove the specific track that finished
                        // by finding it and removing it by object reference.
                        // This is more robust than assuming it's at a particular index.
                        boolean removed = false;
                        List<Track> currentTracks = queue.getTracks();
                        for (int j_idx = 0; j_idx < currentTracks.size(); j_idx++) {
                            if (currentTracks.get(j_idx).getTrackId() == trackToPlayOnEnd.getTrackId()) {
                                queue.removeTrack(trackToPlayOnEnd); // Use the captured track object
                                viewController.setQueue(queue);
                                removed = true;
                                break;
                            }
                        }
                    }
                    List<Track> updatedTracks = queue.getTracks();
                    if (!updatedTracks.isEmpty()) {
                        playTrackAtIndex(0); // Play the new head of the queue
                    } else {
                        isPlaying = false;
                        viewController.setCurrentlyPlayingTrack(null);
                    }
                });
                return;
            }
        }
    }

    /**
     * Lance la lecture de la file d'attente.
     */
    @Override
    public void playQueue() {
        isPlaying = true;

        AudioPlayerController player = GuiMain.audioPlayerController;
        player.addOnPreviousAction(() -> player.seek(0));
        player.addOnNextAction(() -> {
            List<Track> tracks = queue.getTracks();
            Track currentTrack = viewController.getCurrentlyPlayingTrack();
            if (currentTrack != null && !tracks.isEmpty()) {
                queue.removeTrack(currentTrack);
                viewController.setQueue(queue);
                List<Track> updatedTracks = queue.getTracks();
                if (!updatedTracks.isEmpty()) {
                    playTrackAtIndex(0);
                    return;
                }
            }
            player.stop();
            isPlaying = false;
            viewController.setCurrentlyPlayingTrack(null);
        });
        if (!queue.getTracks().isEmpty()) {
            playTrackAtIndex(0);
        }
    }
    
    /**
     * Joue le morceau à l'index donné dans la file d'attente.
     * @param index L'index du morceau à jouer
     */
    private void playTrackAtIndex(int index) {
        List<Track> tracks = queue.getTracks();
        if (index >= 0 && index < tracks.size()) {
            Track trackToPlay = tracks.get(index); // The track that will be played now
            viewController.setCurrentlyPlayingTrack(trackToPlay);
            GuiMain.audioPlayerController.play(trackToPlay.getTrackId());
            // Callers (playQueue, playTrack, modified removeTrack) are responsible for setting QueueController.isPlaying.
            GuiMain.audioPlayerController.addOnEndEvent(() -> {
                if (queue != null && !queue.getTracks().isEmpty()) {
                    // Original logic: The track that just finished should be the one at the head of the queue.
                    // Remove it before playing the next one.
                    Track finishedTrack = queue.getTracks().get(0); 
                    queue.removeTrack(finishedTrack); // Remove by object reference
                    viewController.setQueue(queue);
                }
                List<Track> currentTracks = queue.getTracks();
                if (!currentTracks.isEmpty()) {
                    playTrackAtIndex(0); // Play the new head
                } else {
                    isPlaying = false;
                    viewController.setCurrentlyPlayingTrack(null);
                }
            });
        }
    }
}