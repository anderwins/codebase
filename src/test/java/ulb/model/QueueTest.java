package ulb.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class QueueTest {

    private Queue queue;
    private Track track1;
    private Track track2;

    @BeforeEach
    void setUp() {
        queue = new Queue("Test Queue");
        track1 = new Track("Track 1", "Artist 1", "Album 1", "2025", 180, "Pop", "path/to/file1.mp3", "path/to/cover1.jpg", "path/to/lyrics1.txt", "path/to/karaoke1.lrc");
        track2 = new Track("Track 2", "Artist 2", "Album 2", "2025", 200, "Rock", "path/to/file2.mp3", "path/to/cover2.jpg", "path/to/lyrics2.txt", "path/to/karaoke2.lrc");
    }

    @Test
    void testAddTrack() {
        queue.addTrack(track1);
        queue.addTrack(track2);
        queue.addTrack(track1); // Duplicate, should not be added

        assertEquals(2, queue.getTracks().size());
        assertTrue(queue.getTracks().contains(track1));
        assertTrue(queue.getTracks().contains(track2));
    }

    @Test
    void testIterator() {
        queue.addTrack(track1);
        queue.addTrack(track2);

        Iterator<Track> iterator = queue.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(track1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(track2, iterator.next());
        assertFalse(iterator.hasNext());
    }
}