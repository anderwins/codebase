package ulb.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

public class ChangeTrackerTest {

    private ChangeTracker changeTracker;
    private Track track1;
    private Track track2;

    /**
     * Setup before each test to initialize common test data.
     */
    @BeforeEach
    public void setUp() {
        // Initialize the ChangeTracker
        changeTracker = new ChangeTracker();

        // Create Track objects for testing
        track1 = new Track(1, "Track 1", "Artist A", "Album A", "2021", 250, "Pop",
                "/path/to/file1.mp3", "/path/to/cover1.jpg", "/path/to/lyrics1.txt", "/path/to/karaoke1.lrc");

        track2 = new Track(2, "Track 2", "Artist B", "Album B", "2022", 300, "Rock",
                "/path/to/file2.mp3", "/path/to/cover2.jpg", "/path/to/lyrics2.txt", "/path/to/karaoke2.lrc");
    }

    /**
     * Vérifie que les tracks modifiés sont correctement ajoutés au set de tracks modifiés.
     */
    @Test
    public void testInvalidateTrackAddsToModifiedTracks() {
        assertTrue(changeTracker.getTracksChanged().isEmpty(), "Modified tracks should be empty initially.");

        changeTracker.invalidateTrack(track1.getTrackId());

        // After invalidating, track ID 1 should be added to the modifiedTracks set
        Set<Integer> modifiedTracks = changeTracker.getTracksChanged();
        assertEquals(1, modifiedTracks.size(), "There should be one modified track.");
        assertTrue(modifiedTracks.contains(1), "Modified tracks should contain track ID 1.");
    }

    /**
     * Vérifie que clearChanges() vide correctement la liste des tracks modifiés.
     */
    @Test
    public void testClearChangesRemovesModifiedTracks() {

        changeTracker.invalidateTrack(track1.getTrackId());
        changeTracker.invalidateTrack(track2.getTrackId());

        // Verify that both tracks are added to modifiedTracks
        Set<Integer> modifiedTracks = changeTracker.getTracksChanged();
        assertTrue(modifiedTracks.contains(1), "Modified tracks should contain track ID 1.");
        assertTrue(modifiedTracks.contains(2), "Modified tracks should contain track ID 2.");

        // Now clear the changes
        changeTracker.clearChanges();

        // After clearing, the modifiedTracks set should be empty
        assertTrue(changeTracker.getTracksChanged().isEmpty(), "Modified tracks should be empty after clearChanges.");
    }

    /**
     * Vérifie que les modifications d'un morceau dans la bibliothèque déclenchent bien la notification des observateurs.
     */
    @Test
    public void testTrackObserverNotification() {
        // Add the track and register the observer
        changeTracker.onAddTrack(track1);

        // Track should not be modified initially
        assertTrue(changeTracker.getTracksChanged().isEmpty(), "There should be no modified tracks initially.");

        // Modify the track's data (e.g., update the title)
        track1.setTitle("Updated Track 1");

        // The changeTracker should now have the track ID in the modified set
        assertTrue(changeTracker.getTracksChanged().contains(1), "Modified tracks should contain track ID 1 after modification.");
    }

    /**
     * Vérifie que l'ajout d'un morceau sans changement de données ne déclenche pas une notification.
     */
    @Test
    public void testTrackAddWithoutChange() {
        // Add track but do not change its data
        changeTracker.onAddTrack(track1);

        // There should be no modification as the track was not updated
        assertTrue(changeTracker.getTracksChanged().isEmpty(), "There should be no modified tracks since the track wasn't changed.");
    }

    /**
     * Vérifie que le ChangeTracker ne contient aucune modification après la suppression de tous les morceaux.
     */
    @Test
    public void testNoChangeAfterTrackRemoval() {
        changeTracker.onAddTrack(track1);
        changeTracker.onAddTrack(track2);

        track1.setTitle("Updated Track 1");
        track2.setTitle("Updated Track 2");

        assertTrue(changeTracker.getTracksChanged().contains(1), "Track 1 should be in the modified set.");
        assertTrue(changeTracker.getTracksChanged().contains(2), "Track 2 should be in the modified set.");

        // Remove tracks from the library (which doesn't remove them from the ChangeTracker itself)
        changeTracker.onRemoveTrack(track1);
        changeTracker.onRemoveTrack(track2);

        assertTrue(changeTracker.getTracksChanged().contains(1), "Track 1 should still be in the modified set.");
        assertTrue(changeTracker.getTracksChanged().contains(2), "Track 2 should still be in the modified set.");
    }
}
