package ulb.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class TrackLibraryTest {

    private TrackLibrary trackLibrary;
    private TestObserver testObserver;

    @BeforeEach
    void setUp() {
        trackLibrary = new TrackLibrary();
        testObserver = new TestObserver();
        trackLibrary.addObserver(testObserver);
    }

    @Test
    void testAddTrack() {
        Track track = new Track(1,
                "title", "artist", "album", "2024", 120, "pop",
                null, null, null, null
        );
        trackLibrary.addTrack(track);

        // Check that the track was added to the library
        assertEquals(1, trackLibrary.getTracks().size());
        assertEquals(track, trackLibrary.get(1));
    }

    @Test
    void testGetTrack() {
        Track track = new Track(1,
                "title", "artist", "album", "2024", 120, "pop",
                null, null, null, null
        );
        trackLibrary.addTrack(track);

        // Check that we can retrieve the track correctly
        Track retrievedTrack = trackLibrary.get(1);
        assertNotNull(retrievedTrack);
        assertEquals(track, retrievedTrack);
    }

    @Test
    void testObserversNotifiedOnAddTrack() {
        Track track = new Track(1,
                "title", "artist", "album", "2024", 120, "pop",
                null, null, null, null
        );

        // Initially, no track should be added
        assertFalse(testObserver.isTrackAdded());

        // Add track to library
        trackLibrary.addTrack(track);

        // Check if observer was notified
        assertTrue(testObserver.isTrackAdded());
        assertEquals(track, testObserver.getAddedTrack());
    }

    // Test observer implementation
    private static class TestObserver implements TrackLibrary.TrackLibraryObserver {
        private boolean trackAdded = false;
        private Track addedTrack;

        @Override
        public void onAddTrack(Track t) {
            trackAdded = true;
            addedTrack = t;
        }

        @Override
        public void onRemoveTrack(Track t) {
            // Not testing this for now, so we leave it empty
        }

        public boolean isTrackAdded() {
            return trackAdded;
        }

        public Track getAddedTrack() {
            return addedTrack;
        }
    }

}
