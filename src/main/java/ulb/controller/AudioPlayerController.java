package ulb.controller;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import ulb.dao.DbInitializer;
import ulb.model.handbleError.LyricsDownloadException;
import ulb.model.handbleError.LyricsLoadException;
import ulb.model.handbleError.LyricsParsingException;
import ulb.view.PlayerViewController;
import ulb.Config;
import ulb.view.utils.AlertManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;

import ulb.Config;
import ulb.dao.DbInitializer;
import ulb.model.KaraokeSynchronizer;
import ulb.model.LrcLibService;
import ulb.model.Track;
import ulb.model.TrackLibrary;



/**
 * The {@code AudioPlayerController} class manages audio playback using JavaFX's {@link MediaPlayer}.
 * It interacts with {@link PlayerViewController} to update the UI based on playback status.
 * Implements {@link PlayerViewController.PlayerViewObserver} to handle user interactions.
 */
public class AudioPlayerController implements PlayerViewController.PlayerViewObserver {

    public interface MediaPlayerFactory {
        MediaPlayer getMediaPlayer(Media media);
    }

    public static class DefaultMediaPlayerFactory implements MediaPlayerFactory {
        @Override
        public MediaPlayer getMediaPlayer(Media media) {
            return new MediaPlayer(media);
        }
    }

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> progressUpdater;
    private MediaPlayerFactory mediaPlayerFactory;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private double volume = 0.5;
    private int fade = 15;
    private Timeline fadeInTimeline;
    private Timeline fadeOutTimeline;
    private boolean stopFade = true;
    private final long progressUpdateRate = 100; // miliseconds
    private TrackLibrary trackLibrary;
    private Thread balanceThread;
    private LrcLibService lrcLibService;
    private PlayerViewController viewController;
    private static final Logger logger = Logger.getLogger(DbInitializer.class.getName());

    Runnable onEnd = null;
    Runnable onPreviousAction = null;
    Runnable onNextAction = null;

    public AudioPlayerController(PlayerViewController viewController, TrackLibrary lib, MediaPlayerFactory mediaPlayerFactory) {
        this.viewController = viewController;
        this.trackLibrary = lib;
        this.mediaPlayerFactory = mediaPlayerFactory;
        this.lrcLibService = new LrcLibService(Config.getFullPathFromRelative(Config.KARAOKE_TRACKS_DIRECTORY));
        viewController.setObserver(this);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public AudioPlayerController(PlayerViewController viewController, TrackLibrary lib) {
        this(viewController, lib, new DefaultMediaPlayerFactory());
    }

    public PlayerViewController getView() {
        return viewController;
    }

    public void playStream(String streamUrl) {
        reset();
        try {
            Media media = new Media(streamUrl);
            mediaPlayer = mediaPlayerFactory.getMediaPlayer(media);
            viewController.bindMediaPlayer(mediaPlayer);

            // Gérer les erreurs de chargement du média
            media.setOnError(() -> {
                logger.log(Level.WARNING, "Erreur du Media : " + media.getError().getMessage());
            });
    
            // Gérer les erreurs du mediaPlayer
            mediaPlayer.setOnError(() -> {
                logger.log(Level.WARNING, "Erreur du MediaPlayer : " + mediaPlayer.getError().getMessage());
            });

            mediaPlayer.setOnReady(() -> {
                System.out.println("Flux prêt, attente du buffering...");
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.schedule(() -> Platform.runLater(() -> {
                    mediaPlayer.play();
                    isPlaying = true;
                    setPause(false);
                    startProgressUpdate();
                    scheduler.shutdown();
                }), 1, TimeUnit.SECONDS);  // wait 1 second for buffering
            });

    
        } catch (Exception e) {
            logger.severe("Erreur lors de la lecture du flux radio : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Plays an audio track using its ID.
     *
     * @param trackId the ID of the track to play
     */
    public void play(int trackId) {
        Track t = trackLibrary.get(trackId);
        if (t == null) {
            logger.log(Level.WARNING, "Erreur : le track avec l'ID " + trackId + " est introuvable.");
            return;
        }
        playFile(t.getFilePath());
        viewController.changeTrack(t);
        viewController.updateTrackInfoPicture(t);
    }

    public void addOnEndEvent(Runnable runnable) {
        //onEndRunnables.add(runnable);
        this.onEnd = runnable;
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Plays an audio file from the given file path.
     *
     * @param filePath the file path of the audio file
     */
    public void playFile(String filePath) {
        reset(); // get clean state
        if (filePath != null) {
            try {
                double fadeDuration = this.fade;

                File file = new File(filePath);
                String uri = file.toURI().toString();
                Media media = new Media(uri);
                mediaPlayer = mediaPlayerFactory.getMediaPlayer(media);
                viewController.bindMediaPlayer(mediaPlayer);

                if (!stopFade){
                    mediaFade(fadeDuration);
                }

                media.setOnError(() -> {
                    String errorMessage = "Erreur Media: Impossible de charger le fichier. " + (media.getError() != null ? media.getError().getMessage() : "Cause inconnue.");
                    logger.log(Level.SEVERE, errorMessage, media.getError());
                    AlertManager.showError("Erreur de Lecture Média", errorMessage);
                    reset();
                });

                mediaPlayer.setOnError(() -> {
                    String errorMessage = "Erreur MediaPlayer: Problème de lecture. " + (mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "Cause inconnue.");
                    logger.log(Level.SEVERE, errorMessage, mediaPlayer.getError());
                    AlertManager.showError("Erreur de Lecteur Média", errorMessage);
                    reset(); // Reset player state on error
                });

                mediaPlayer.setOnEndOfMedia(() -> {
                    if (this.onEnd != null) {
                        this.onEnd.run(); // Call the general onEnd handler first
                    } else {
                        stop(); // Default behavior if no specific onEnd is set by QueueController
                    }
                });
                mediaPlayer.play();
                setVolume(this.volume);
                isPlaying = true;
                setPause(false);
                startProgressUpdate();
                stopFade = true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erreur lors de la préparation du fichier média: " + filePath, e);
                AlertManager.showError("Erreur Média", "Impossible de lire le fichier: " + filePath + ".\n" + e.getMessage());
                reset();
            }
        } else {
            logger.log(Level.WARNING, "File path cannot be null for playFile method.");
            AlertManager.showWarning("Chemin de fichier invalide", "Le chemin du fichier musical est nul.");
            throw new IllegalArgumentException();
        }
    }

    /**
     * mets un "fade" sur la musique qui se joue actuellement
     */
    private void mediaFade(double fadeDuration){
        // Créer une Timeline pour le fondu en entrée (fade-in)
        fadeInTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0), new KeyValue(mediaPlayer.volumeProperty(), 0)),
            new KeyFrame(Duration.seconds(fadeDuration), new KeyValue(mediaPlayer.volumeProperty(), 1))
        );
        fadeInTimeline.setCycleCount(1);
        fadeInTimeline.play();

        double mediaDurationInSeconds = mediaPlayer.getTotalDuration().toSeconds();
        System.out.println("duration et fade : " + mediaPlayer.getTotalDuration().toSeconds() + fade);
        mediaPlayer.setOnReady(() -> {
            // Planifier un fondu en sortie avant la fin de la musique
            fadeOutTimeline = new Timeline(
                new KeyFrame(Duration.seconds(mediaDurationInSeconds - fade), e -> {
                    // Commencer à baisser le volume à partir de ce point
                    Timeline fadeOut = new Timeline(
                        new KeyFrame(Duration.seconds(0), new KeyValue(mediaPlayer.volumeProperty(), 1)),
                        new KeyFrame(Duration.seconds(fadeDuration), new KeyValue(mediaPlayer.volumeProperty(), 0))
                    );
                    fadeOut.setCycleCount(1);
                    fadeOut.play();
                })
            );
            fadeOutTimeline.setCycleCount(1);
            fadeOutTimeline.play();
        });
    }

    /**
     * Initializes the timeline to periodically report the read progress to the view during playback.
     */
    private void initProgressTimeline() {

    }

    /**
     * Starts reporting progress to the view progress.
     */
    private void startProgressUpdate() {
        logger.info("starting progress update");
        progressUpdater = scheduler.scheduleAtFixedRate(() -> {
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                double progress = mediaPlayer.getCurrentTime().toSeconds() / mediaPlayer.getTotalDuration().toSeconds();
                viewController.updateProgress(progress, mediaPlayer.getCurrentTime().toMillis());
            }
        }, 0, progressUpdateRate, TimeUnit.MILLISECONDS);
    }

    /**
     * Pauses the currently playing track.
     */
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPaused = true;
            viewController.updatePlayPause(isPaused);
        }
    }

    /**
     * Resumes playback of a paused track.
     */
    private void resume() {
        if (isPaused) {
            mediaPlayer.play();
            setPause(false);
            viewController.updatePlayPause(isPaused);
            logger.info("Resumed");
        }
    }

    /**
     * Stops the currently playing track, resetting the player state.
     */
    public void stop() {
        logger.info("stopping");
        this.reset();

        if (this.onEnd != null) {
            this.onEnd.run();
        }

        // Only proceed if mediaPlayer is not null and the player is actually playing or paused
        if (mediaPlayer != null && (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED)) {
            // Store the onEnd callback before stopping to avoid recursive calls
            Runnable storedCallback = this.onEnd;
            // Clear the callback before executing it to prevent infinite loops
            this.onEnd = null;
            
            // Stop the media player and timeline
            mediaPlayer.stop();
            
            // Log once
            logger.info("Player stopped");
            
            // Update state
            isPlaying = false;
            setPause(true);
            viewController.changeTrack(null);
            
            // Execute the stored callback if it exists
            if (storedCallback != null) {
                storedCallback.run();
            }
        }
    }

    public void reset() {
        isPlaying = false;
        setPause(true);
        if (progressUpdater != null && !progressUpdater.isCancelled()) {
            progressUpdater.cancel(true);
            logger.info("Progress updater cancelled.");
        }
        if (mediaPlayer != null) {
            mediaPlayer.setOnEndOfMedia(null); // Avoid calling old onEnd logic
            mediaPlayer.setOnError(null);
            if (mediaPlayer.getMedia() != null) {
                 mediaPlayer.getMedia().setOnError(null);
            }
            mediaPlayer.stop();
            mediaPlayer = null; // Release mediaplayer instance
            logger.info("MediaPlayer stopped and nulled in reset.");
        }
        // S'assurer que viewController.changeTrack(null) est appelé pour que le test passe
        viewController.changeTrack(null);
    }

    /**
     * Shuts down the executor service used for progress updates.
     * This should be called when the audio player is no longer needed to free resources.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // Wait a little for existing tasks to terminate
                if (!scheduler.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                    scheduler.shutdownNow(); // Cancel currently executing tasks
                    // Wait a little for tasks to respond to being cancelled
                    if (!scheduler.awaitTermination(200, TimeUnit.MILLISECONDS))
                        logger.severe("Scheduler did not terminate.");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                scheduler.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            logger.info("AudioPlayerController scheduler shutdown.");
        }
    }

    /**
     * Seeks to a specific position in the track.
     *
     * @param seconds the time position in seconds
     */
    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(javafx.util.Duration.seconds(seconds));
        }
    }

    /**
     * Checks if a track is currently playing.
     *
     * @return {@code true} if playing, {@code false} otherwise
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public void onPlayPause() {
        if (!isPlaying) return;
        if (!isPaused) {
            pause();
        } else {
            resume();
        }
    }

    private void setPause(boolean value) {
        this.isPaused = value;
        viewController.updatePlayPause(isPaused);
    }

    public void addOnPreviousAction(Runnable action) {
        this.onPreviousAction = action;
    }

    @Override
    public void onPrevious() {
        if (this.onPreviousAction != null) {
            this.onPreviousAction.run();
        }
    }

    public void addOnNextAction(Runnable action) {
        this.onNextAction = action;
    }

    @Override
    public void onNext() {
        if (this.onNextAction != null) {
            this.onNextAction.run();
        }
    }

    @Override
    public void onVolumeChange(double value) {
        if (mediaPlayer != null) {
            setVolume(value);
        }
    }

    /**
     * Updates the volume of the media player based on the internal volume stored.
     *
     * @param value the volume value to set to
     */
    private void setVolume(double value) {
        this.volume = value;
        mediaPlayer.setVolume(volume);
        viewController.updateVolume(volume);
    }

    public double getVolume() {
        return this.volume;
    }

    /**
     * Handles the audio balance processing.
     *
     * @throws IllegalArgumentException if the file path is invalid
     */
    @Override
    public void onBalanceAudio(double balance){
        if(mediaPlayer != null){
            mediaPlayer.setBalance(balance);
        }else{
            logger.warning("cant set balance media player is null");
        }
    }

    @Override
    public void onSeek(double progress) {
        // Handle seek updates
    }

    @Override
    public void onSeekStart(double progress) {
        if (progressUpdater != null && !progressUpdater.isCancelled()) {
            progressUpdater.cancel(true);
        }
    }

    @Override
    public void onSeekEnd(double progress) {
        seek(progress * mediaPlayer.getTotalDuration().toSeconds());

        // On interrompt les fades si l'utilisateur cherche
        if (fadeInTimeline != null) fadeInTimeline.stop();
        if (fadeOutTimeline != null) fadeOutTimeline.stop();

        if (mediaPlayer.getCurrentTime().lessThan(Duration.seconds(fade))) {
            fadeInTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(mediaPlayer.volumeProperty(), 0)),
                new KeyFrame(Duration.seconds(fade), new KeyValue(mediaPlayer.volumeProperty(), 1))
            );
            fadeInTimeline.play();
        }
        
        // On relance l'update visuel
        startProgressUpdate();

    }


    /**
     * Displays the lyrics for the given track.
     *
     * @param track the track for which to display lyrics
     * @return a list of {@link KaraokeSynchronizer.LyricsLine} objects representing the lyrics
     */
    @Override
    public List<KaraokeSynchronizer.LyricsLine> showLyrics(Track track) {
        if (track == null) {
            logger.log(Level.WARNING, "Track object is null in showLyrics method.");
            return java.util.Collections.emptyList();
        }
        if (track.getTitle() == null || track.getTitle().trim().isEmpty()) {
            logger.log(Level.WARNING, "Track title is null or empty for lyrics search.");
            AlertManager.showError("Données de piste invalides", "Le titre de la piste est manquant ou vide, impossible de rechercher les paroles.");
            return java.util.Collections.emptyList();
        }

        String sanitizedTitle = track.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_");
        String lrcFilePath = Config.getFullPathFromRelative(Config.LYRICS_TRACKS_DIRECTORY) + "/" + sanitizedTitle + ".lrc";
        String txtFilePath = Config.getFullPathFromRelative(Config.LYRICS_TRACKS_DIRECTORY) + "/" + sanitizedTitle + ".txt";

        // Attempt 1: Local .lrc file
        File lrcFile = new File(lrcFilePath);
        if (lrcFile.exists()) {
            logger.info("Found local .lrc file: " + lrcFilePath);
            try {
                KaraokeSynchronizer synchronizer = new KaraokeSynchronizer(lrcFilePath);
                List<KaraokeSynchronizer.LyricsLine> lines = synchronizer.getLyricsLines();
                if (!lines.isEmpty()) {
                    track.setKaraokePath(lrcFilePath);
                    logger.info("Successfully loaded lyrics from .lrc file.");
                    return lines;
                } else {
                    logger.info(".lrc file is empty: " + lrcFilePath);
                }
            } catch (LyricsLoadException e) {
                logger.log(Level.WARNING, "Failed to load/parse .lrc file: " + lrcFilePath, e);
                AlertManager.showError("Erreur de paroles (LRC)", "Impossible de charger les paroles du fichier .lrc local: " + e.getLocalizedMessage());
            }
        }

        // Attempt 2: Local .txt file
        File txtFile = new File(txtFilePath);
        if (txtFile.exists()) {
            logger.info("Found local .txt file: " + txtFilePath);
            try {
                KaraokeSynchronizer synchronizer = new KaraokeSynchronizer(txtFilePath);
                List<KaraokeSynchronizer.LyricsLine> lines = synchronizer.getLyricsLines();
                if (!lines.isEmpty()) {
                    track.setKaraokePath(txtFilePath);
                    logger.info("Successfully loaded lyrics from .txt file.");
                    return lines;
                } else {
                    logger.info(".txt file is empty: " + txtFilePath);
                }
            } catch (LyricsLoadException e) {
                logger.log(Level.WARNING, "Failed to load/parse .txt file: " + txtFilePath, e);
                AlertManager.showError("Erreur de paroles (TXT)", "Impossible de charger les paroles du fichier .txt local: " + e.getLocalizedMessage());
            }
        }
        
        track.setKaraokePath(null); // Clear path before online search

        // Attempt 3: Online search and download
        logger.info("No definitive local lyrics found. Attempting online search for: " + track.getTitle());
        boolean onlineSearchPerformed = true; // Assume we will attempt it
        try {
            lrcLibService.searchAndSaveLyrics(track); 
            if (track.getKaraokePath() == null) {
                logger.info("Online search completed but did not yield a lyrics file path for: " + track.getTitle());
            } else {
                 logger.info("Online search yielded a potential lyrics file: " + track.getKaraokePath());
            }
        } catch (LyricsDownloadException e) {
            logger.log(Level.WARNING, "Lyrics download error for " + track.getTitle() + ": " + e.getMessage(), e);
            AlertManager.showWarning("Erreur de téléchargement", "Impossible de télécharger les paroles: " + e.getLocalizedMessage());
            track.setKaraokePath(null);
        } catch (LyricsParsingException e) {
            logger.log(Level.WARNING, "Lyrics parsing error for " + track.getTitle() + ": " + e.getMessage(), e);
            AlertManager.showWarning("Erreur d'analyse des paroles", "Impossible d'analyser les paroles téléchargées: " + e.getLocalizedMessage());
            track.setKaraokePath(null);
        } catch (Exception e) { 
            logger.log(Level.SEVERE, "Unexpected error during online lyrics search for " + track.getTitle() + ": " + e.getMessage(), e);
            AlertManager.showError("Erreur Inattendue (Paroles)", "Une erreur système est survenue lors de la recherche de paroles en ligne.");
            track.setKaraokePath(null);
            onlineSearchPerformed = false; // Indicate online search itself had a major issue
        }

        // Attempt 4: Load from path (if set by online search)
        if (track.getKaraokePath() != null) {
            logger.info("Attempting to load lyrics from path: " + track.getKaraokePath());
            try {
                KaraokeSynchronizer synchronizer = new KaraokeSynchronizer(track.getKaraokePath());
                List<KaraokeSynchronizer.LyricsLine> lines = synchronizer.getLyricsLines();
                if (!lines.isEmpty()) {
                    logger.info("Successfully loaded lyrics from: " + track.getKaraokePath());
                    return lines;
                } else {
                    logger.warning("Lyrics file found but is empty: " + track.getKaraokePath());
                    AlertManager.showInfo("Paroles Vides", "Le fichier de paroles trouvé ('" + new File(track.getKaraokePath()).getName() + "') est vide.");
                }
            } catch (LyricsLoadException e) {
                logger.log(Level.WARNING, "Failed to load/parse lyrics from file: " + track.getKaraokePath(), e);
                AlertManager.showError("Erreur de Chargement des Paroles", "Impossible de charger les paroles du fichier '" + new File(track.getKaraokePath()).getName() + "': " + e.getLocalizedMessage());
            }
        } else {
            // If karaokePath is null here, it means local attempts failed or yielded empty files,
            // and online search either failed with an exception (alert shown) or didn't find anything.
            if (onlineSearchPerformed) { // Only show "not found" if online search was attempted and didn't yield a path or failed quietly
                 AlertManager.showInfo("Paroles Non Trouvées", "Aucune parole n'a été trouvée pour '" + track.getTitle() + "' après recherche locale et en ligne.");
            }
            // If onlineSearchPerformed is false, it means a severe error occurred during the service call itself, and an alert was already shown.
        }

        logger.warning("Returning empty list of lyrics for track: " + track.getTitle() + " after all attempts.");
        return java.util.Collections.emptyList();
    }

    @Override
    public long getCurrentTime() {
        return (long) mediaPlayer.getCurrentTime().toMillis();
    }

    // Ajout des méthodes de nettoyage d'événements
    private void clearOnEndEvent() {
        this.onEnd = null;
    }
    
    private void clearOnPreviousAction() {
        this.onPreviousAction = null;
    }
    
    private void clearOnNextAction() {
        this.onNextAction = null;
    }
    
    public void removeOnEndEvent(Runnable runnable) {
        if (this.onEnd == runnable) {
            this.onEnd = null;
        }
    }

    /// Set the speed of the media player
    @Override
    public void setPlaySpeed(double speed){
        if (mediaPlayer != null){
            mediaPlayer.setRate(speed);
        }

    }

    public void setFade(){
        if (stopFade){
            stopFade = false;
            logger.info("fade on");
        } else {
            stopFade = true;
            logger.info("fade off");
        }
    }
    
}
