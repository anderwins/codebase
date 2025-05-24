package ulb.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class TrackTest {

    /**
     * Vérifie que getMetadata() retourne correctement les métadonnées clés du morceau.
     */
    @Test
    public void testGetMetadataReturnsCorrectData() {
        Track track = new Track("Track Title", "Artist Name", "Album Name", "2024", 210,
                "Pop", "/path/to/file.mp3", "/path/to/cover.jpg",
                "/path/to/lyrics.txt", "/path/to/karaoke.lrc");

        Map<String, Object> metadata = track.getMetadata();

        assertEquals("Track Title", metadata.get("title"));
        assertEquals("Artist Name", metadata.get("artist"));
        assertEquals("Album Name", metadata.get("album"));
        assertEquals("2024", metadata.get("year"));
        assertEquals(210, metadata.get("duration"));
        assertEquals("Pop", metadata.get("genre"));
    }

    /**
     * Vérifie que toString() retourne une chaîne formatée contenant les bonnes informations.
     */
    @Test
    public void testToStringReturnsExpectedFormat() {
        Track track = new Track("Hello", "Adele", "25", "2015", 295, "Soul",
                "/music/hello.mp3", "/images/adele.jpg",
                "/lyrics/hello.txt", "/karaoke/hello.lrc");

        String result = track.toString();
        assertTrue(result.contains("🎵 Hello - Adele (25, 2015) [295 sec, Soul]"),
                "Le format de toString n'est pas correct !");
    }

    /**
     * Vérifie que les setters modifient bien les champs, et que les getters retournent les nouvelles valeurs.
     */
    @Test
    public void testSettersAffectValuesCorrectly() {
        Track track = new Track("Old Title", "Old Artist", "Old Album", "2000", 100, "Jazz",
                "/old.mp3", "/old.jpg", "/old.txt", "/old.lrc");

        track.setTitle("New Title");
        track.setArtist("New Artist");
        track.setAlbum("New Album");
        track.setYear("2025");
        track.setDuration(300);
        track.setGenre("Rock");

        assertEquals("New Title", track.getTitle());
        assertEquals("New Artist", track.getArtist());
        assertEquals("New Album", track.getAlbum());
        assertEquals("2025", track.getYear());
        assertEquals(300, track.getDuration());
        assertEquals("Rock", track.getGenre());
    }

    /**
     * Vérifie que les chemins des fichiers sont correctement enregistrés et retournés.
     */
    @Test
    public void testFilePathsSetCorrectly() {
        Track track = new Track("T", "A", "Al", "2020", 200, "Pop",
                "/music.mp3", "/cover.jpg", "/lyrics.txt", "/karaoke.lrc");

        assertEquals("/music.mp3", track.getFilePath());
        assertEquals("/cover.jpg", track.getCoverPath());
        assertEquals("/lyrics.txt", track.getLyricsPath());
        assertEquals("/karaoke.lrc", track.getKaraokePath());
    }

    /**
     * Vérifie que assign() copie correctement les données d'un autre morceau.
     */
    @Test
    public void testAssign() {
        Track track1 = new Track(1, "Track 1", "Artist A", "Album A", "2021", 250, "Pop",
                "/path/to/file1.mp3", "/path/to/cover1.jpg", "/path/to/lyrics1.txt", "/path/to/karaoke1.lrc");

        Track track2 = new Track(2, "Track 2", "Artist B", "Album B", "2022", 300, "Rock",
                "/path/to/file2.mp3", "/path/to/cover2.jpg", "/path/to/lyrics2.txt", "/path/to/karaoke2.lrc");

        track2.assign(track1);

        assertEquals(track1.getTrackId(), track2.getTrackId());
        assertEquals(track1.getTitle(), track2.getTitle());
        assertEquals(track1.getArtist(), track2.getArtist());
        assertEquals(track1.getAlbum(), track2.getAlbum());
        assertEquals(track1.getYear(), track2.getYear());
        assertEquals(track1.getDuration(), track2.getDuration());
        assertEquals(track1.getGenre(), track2.getGenre());
        assertEquals(track1.getFilePath(), track2.getFilePath());
        assertEquals(track1.getCoverPath(), track2.getCoverPath());
        assertEquals(track1.getLyricsPath(), track2.getLyricsPath());
        assertEquals(track1.getKaraokePath(), track2.getKaraokePath());
    }

    /**
     * Vérifie que isEqual() fonctionne correctement pour comparer deux morceaux.
     */
    @Test
    public void testIsEqual() {
        Track track1 = new Track(1, "Track A", "Artist A", "Album A", "2021", 250, "Pop",
                "/path/to/file1.mp3", "/path/to/cover1.jpg", "/path/to/lyrics1.txt", "/path/to/karaoke1.lrc");

        Track track2 = new Track(1, "Track A", "Artist A", "Album A", "2021", 250, "Pop",
                "/path/to/file1.mp3", "/path/to/cover1.jpg", "/path/to/lyrics1.txt", "/path/to/karaoke1.lrc");

        assertTrue(track1.isEqual(track2));

        track2.setTitle("Different Track");
        assertFalse(track1.isEqual(track2));
    }

    /**
     * Vérifie que les observateurs sont notifiés correctement lors d'un changement de données.
     */
    @Test
    public void testObserversAreNotifiedOnChange() {
        Track track = new Track(1, "Track", "Artist", "Album", "2024", 210, "Pop",
                "/path/to/file.mp3", "/path/to/cover.jpg", "/path/to/lyrics.txt", "/path/to/karaoke.lrc");

        TestTrackObserver observer = new TestTrackObserver();
        track.addObserver(observer);

        // Modifying data should notify the observer
        track.setTitle("New Track Title");
        assertTrue(observer.isNotified(), "Observer was not notified on data change!");
        assertEquals("New Track Title", observer.getChangedTrack().getTitle());
    }

    /**
     * Test observer implementation.
     */
    private static class TestTrackObserver implements Track.TrackObserver {
        private boolean notified = false;
        private Track changedTrack;

        @Override
        public void onChangeData(Track t) {
            this.notified = true;
            this.changedTrack = t;
        }

        public boolean isNotified() {
            return notified;
        }

        public Track getChangedTrack() {
            return changedTrack;
        }
    }
}
