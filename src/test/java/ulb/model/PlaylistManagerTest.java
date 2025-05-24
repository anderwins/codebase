package ulb.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistManagerTest {

    private PlaylistManager playlistManager;
    private Playlist playlist1;
    private Playlist playlist2;

    @BeforeEach
    void setUp() {
        playlistManager = PlaylistManager.getInstance();
        playlistManager.getPlaylists().clear(); // Clear existing playlists
        playlistManager = null; // Reset the Singleton instance
        try {
            java.lang.reflect.Field instance = PlaylistManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset PlaylistManager Singleton instance", e);
        }
        playlistManager = PlaylistManager.getInstance();
        playlist1 = new Playlist("Playlist 1");
        playlist2 = new Playlist("Playlist 2");
    }

    @Test
    void testInitialState() {
        List<Playlist> playlists = playlistManager.getPlaylists();
        assertTrue(playlists.isEmpty(), "The initial state of playlists should be empty.");
    }

    @Test
    void testAddPlaylist() {
        playlistManager.addPlaylist(playlist1);
        playlistManager.addPlaylist(playlist2);
        playlistManager.addPlaylist(playlist1); // Duplicate, should not be added

        List<Playlist> playlists = playlistManager.getPlaylists();
        assertEquals(2, playlists.size());
        assertTrue(playlists.contains(playlist1));
        assertTrue(playlists.contains(playlist2));
    }

    @Test
    void testRemovePlaylist() {
        playlistManager.addPlaylist(playlist1);
        playlistManager.addPlaylist(playlist2);

        playlistManager.removePlaylist(playlist1);

        List<Playlist> playlists = playlistManager.getPlaylists();
        assertEquals(1, playlists.size());
        assertFalse(playlists.contains(playlist1));
        assertTrue(playlists.contains(playlist2));
    }

    @Test
    void testFindPlaylistWithTitle() {
        playlistManager.addPlaylist(playlist1);
        playlistManager.addPlaylist(playlist2);

        Playlist found = playlistManager.findPlaylistWithTitle("Playlist 1");
        assertNotNull(found);
        assertEquals("Playlist 1", found.getTitle());

        Playlist notFound = playlistManager.findPlaylistWithTitle("Nonexistent Playlist");
        assertNull(notFound);
    }
}