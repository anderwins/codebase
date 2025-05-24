package ulb.controller;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import ulb.model.TrackLibrary;
import ulb.view.PlayerViewController;
import ulb.model.Track;
import ulb.model.Radio;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AudioPlayerControllerTest {

    @Mock
    private PlayerViewController viewController;

    @Mock
    private TrackLibrary trackLibrary;

    @Mock
    private MediaPlayer mockMediaPlayer;

    private AudioPlayerController controller;

    private URL resource;
    private String path;

    @BeforeEach
    public void setUp() throws Exception {
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler);
        rootLogger.setLevel(Level.OFF);

        // Crée un répertoire temporaire pour les lyrics et autres données
        Path tempDataDir = Files.createTempDirectory("test_deezify_g8_data");
        ulb.Config.setDataDirectoryPath(tempDataDir.toString());

        // S'assure que le dossier lrc existe et n'est pas un fichier
        Path lrcPath = tempDataDir.resolve("lrc");
        if (Files.exists(lrcPath) && !Files.isDirectory(lrcPath)) {
            Files.delete(lrcPath);
        }
        Files.createDirectories(lrcPath);

        resource = getClass().getResource("/musiques/sample.mp3");
        assertNotNull(resource, "Fichier audio introuvable");
        path = resource.toURI().getPath();

        MockitoAnnotations.openMocks(this);

        doAnswer(invocation -> null).when(viewController).updateProgress(anyDouble(), anyDouble());


        // Create a mock MediaPlayerFactory
        AudioPlayerController.MediaPlayerFactory mockMediaPlayerFactory = media -> mockMediaPlayer;

        Duration mockDuration = Duration.seconds(200);
        Duration mockCurrent = Duration.seconds(200);

        when(mockMediaPlayer.getTotalDuration()).thenReturn(mockDuration);
        when(mockMediaPlayer.getCurrentTime()).thenReturn(mockCurrent);

        // Initialize the AudioPlayerController with the mock factory
        controller = new AudioPlayerController(viewController, trackLibrary, mockMediaPlayerFactory);
    }

    @Test
    public void testSetVolume() throws Exception {
        // Lecture du fichier audio
        controller.playFile(path);
        Thread.sleep(500); // Laisser le temps à MediaPlayer de se préparer

        controller.onVolumeChange(0.8);  // Change le volume

        // Vérifie que le volume interne a été mis à jour
        assertEquals(0.8, controller.getVolume(), 0.01);

        // Vérifie que la vue a été notifiée
        verify(viewController).updateVolume(0.8);
    }

    @Test
    public void testSeek() throws Exception {
        // Lecture du fichier audio
        controller.playFile(path);

        when(mockMediaPlayer.getStatus()).thenReturn(MediaPlayer.Status.PLAYING);

        Thread.sleep(300); // Laisser le temps de quelque updates

        // Seek à 50%
        controller.onSeekEnd(0.5);

        // Vérifie que la vue a bien été mise à jour
        verify(viewController, atLeastOnce()).updateProgress(anyDouble(), anyDouble());
    }


    @Test
    public void testPlay() {
        // Arrange: create a mock Track
        Track mockTrack = mock(Track.class);
        when(mockTrack.getFilePath()).thenReturn(path);
        when(trackLibrary.get(1)).thenReturn(mockTrack);

        // Act: call play with trackId
        controller.play(1);

        // Assert: Verify that the media player was created and played
        verify(viewController).changeTrack(mockTrack);
        verify(viewController).updateTrackInfoPicture(mockTrack);
        verify(mockMediaPlayer).play();
    }

    @Test
    public void testPlayTrackNotFound() {
        // Arrange: Return null when track is searched by ID
        when(trackLibrary.get(999)).thenReturn(null);

        // Act: call play with a non-existent track ID
        controller.play(999);

        // Assert: The track should not be played, and no interaction with MediaPlayer should happen
        verify(mockMediaPlayer, never()).play();
    }

    @Test
    public void testPauseAndResume() {
        // Arrange: create a mock Track
        Track mockTrack = mock(Track.class);
        when(mockTrack.getFilePath()).thenReturn(path);
        when(trackLibrary.get(1)).thenReturn(mockTrack);

        assertFalse(controller.isPlaying());
        // Act: call play and then pause
        controller.play(1);
        controller.pause();

        assertTrue(controller.isPlaying());
        assertTrue(controller.isPaused());

        // Assert: Verify the play and pause were called
        verify(mockMediaPlayer).play();
        verify(mockMediaPlayer).pause();

        // Act: call resume
        controller.onPlayPause();

        assertTrue(controller.isPlaying());
        assertFalse(controller.isPaused());

        // Assert: Verify the resume play (verify only one play after pause)
        InOrder inOrder = inOrder(mockMediaPlayer);
        inOrder.verify(mockMediaPlayer).play();  // Verify play was called first
        inOrder.verify(mockMediaPlayer).pause(); // Verify pause was called
        inOrder.verify(mockMediaPlayer).play();  // Verify play is called again after resume
    }

    @Test
    public void testStopResetsState() throws Exception {
        controller.playFile(path);
        when(mockMediaPlayer.getStatus()).thenReturn(MediaPlayer.Status.PLAYING);
        Thread.sleep(1000);
        controller.stop();
        assertFalse(controller.isPlaying());
        assertTrue(controller.isPaused());
        verify(viewController, atLeastOnce()).changeTrack(null);
    }

    @Test
    public void testVolumePersistsAfterPauseResume() {
        controller.playFile(path);
        controller.onVolumeChange(0.7);
        controller.pause();
        controller.onPlayPause();
        assertEquals(0.7, controller.getVolume(), 0.01);
    }

    @Test
    public void testPlayFileWithNullPath() {
        assertThrows(IllegalArgumentException.class, () -> controller.playFile(null));
    }

    //@Test
    //public void testPlayRadioStream() throws InterruptedException{
    //    String radioUrl = "http://belrtl.ice.infomaniak.ch/belrtl-mp3-192.mp3"; 
    //    Radio radio = new Radio("Test Radio", radioUrl);
    //    controller.playStream(radio.getStreamUrl());
    //    assertTrue(controller.isPlaying());
    //}

    @Test
    public void testStopRadioStream() throws InterruptedException {
        String radioUrl = "http://belrtl.ice.infomaniak.ch/belrtl-mp3-192.mp3"; 
        Radio radio = new Radio("Test Radio", radioUrl);
        controller.playStream(radio.getStreamUrl());
        Thread.sleep(1000); // on attend un peu que le player se mette en route
        controller.stop();
        assertFalse(controller.isPlaying());
    }
}
