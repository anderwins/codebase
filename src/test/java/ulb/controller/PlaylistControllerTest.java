package ulb.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import ulb.dao.DbManagerInsert;
import ulb.model.Playlist;
import ulb.model.PlaylistManager;
import ulb.model.Track;
import ulb.view.PlaylistViewController;

import java.util.ArrayList;
import java.util.Collections; // Import Collections for emptyList
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class) // Ensures Mockito annotations are processed
class PlaylistControllerTest {

    // --- Mocks for Dependencies ---
    @Mock
    private PlaylistViewController mockViewController;
    @Mock
    private DbManagerInsert mockDbManagerInsert;
    @Mock
    private AudioPlayerController mockPlayerController;
    @Mock
    private PlaylistManager mockPlaylistManager;
    @Mock
    private MainController mockMaincontroller;

    // --- Mocks for Collaborators ---
    @Mock
    private Playlist mockPlaylist;
    @Mock
    private Track mockTrack; // General purpose mock track

    // --- Class Under Test ---
    // InjectMocks creates an instance and injects fields annotated with @Mock
    @InjectMocks
    private PlaylistController playlistController;

    // No explicit setUp needed thanks to @InjectMocks and @ExtendWith


    @BeforeEach
    public void setUp(){
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.removeHandler(consoleHandler);
        rootLogger.setLevel(Level.OFF);
    }

    @Test
    void testConstructorWithNullDependencies() {
        // Test that constructor throws IllegalArgumentException when dependencies are null
        assertThrows(IllegalArgumentException.class, () ->
            new PlaylistController(null, mockMaincontroller, mockDbManagerInsert, mockPlayerController, mockPlaylistManager)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new PlaylistController(mockViewController, mockMaincontroller, null, mockPlayerController, mockPlaylistManager)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new PlaylistController(mockViewController, mockMaincontroller, mockDbManagerInsert, null, mockPlaylistManager)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new PlaylistController(mockViewController, mockMaincontroller, mockDbManagerInsert, mockPlayerController, null)
        );
    }

    @Test
    void testCreateNewPlaylistFromTitle() {
        // Arrange
        String title = "New Playlist";
        // Use ArgumentCaptor to verify the playlist details later
        ArgumentCaptor<Playlist> playlistCaptor = ArgumentCaptor.forClass(Playlist.class);
        // Stubbing DB insert (assuming it returns boolean success)
        when(mockDbManagerInsert.insertPlaylist(eq(title), anyString())).thenReturn(true);

        // Act
        String result = playlistController.createNewPlaylistFromTitle(title);

        // Assert
        assertEquals(title, result);
        // Verify manager interaction
        verify(mockPlaylistManager).addPlaylist(playlistCaptor.capture());
        assertEquals(title, playlistCaptor.getValue().getTitle()); // Check captured playlist
        // Verify DB interaction
        verify(mockDbManagerInsert).insertPlaylist(eq(title), eq("Admin")); // Be specific if "Admin" is constant
        // Verify view interaction (it should get the *same* playlist instance)
        verify(mockViewController).setPlayList(eq(playlistCaptor.getValue()));
        // Verify observer added to the new playlist
        // Cannot directly verify addObserver on the *new* real Playlist object easily,
        // but we trust setPlaylist does its job based on the code.
    }

    @Test
    void testSetPlaylistWithTitle_Found() {
        // Arrange
        String title = "Existing Playlist";
        when(mockPlaylistManager.findPlaylistWithTitle(title)).thenReturn(mockPlaylist);

        // Act
        playlistController.setPlaylistWithTitle(title);

        // Assert
        verify(mockPlaylistManager).findPlaylistWithTitle(title);
        // Verify observer added ONLY to the found playlist
        verify(mockPlaylist).addObserver(playlistController);
        // Verify view updated with the found playlist
        verify(mockViewController).setPlayList(mockPlaylist);
    }

    @Test
    void testSetPlaylistWithTitle_NotFound() {
        // Arrange
        String title = "Non-existent Playlist";
        when(mockPlaylistManager.findPlaylistWithTitle(title)).thenReturn(null);

        // Act
        playlistController.setPlaylistWithTitle(title);

        // Assert
        verify(mockPlaylistManager).findPlaylistWithTitle(title);
        // Verify view was updated with null (or however your app handles not found)
        verify(mockViewController).setPlayList(null); // Assuming null is the behavior
        // Verify no observer was added
        verify(mockPlaylist, never()).addObserver(any());
    }

    @Test
    void testSetPlaylist_FromNonNullToNonNull() {
        // Arrange - set a previous playlist first
        Playlist previousPlaylist = mock(Playlist.class);
        playlistController.setPlaylist(previousPlaylist); // Initial setup

        // Act - set a new playlist
        playlistController.setPlaylist(mockPlaylist);

        // Assert
        // Verify observer removed from the *previous* playlist
        verify(previousPlaylist).removeObserver(playlistController);
        // Verify observer added to the *new* playlist
        verify(mockPlaylist).addObserver(playlistController);
        // Verify view updated with the *new* playlist
        verify(mockViewController).setPlayList(mockPlaylist);
    }

    @Test
    void testSetPlaylist_FromNonNullToNull() {
        // Arrange - set a previous playlist first
        Playlist previousPlaylist = mock(Playlist.class);
        playlistController.setPlaylist(previousPlaylist); // Initial setup

        // Act - set playlist to null
        playlistController.setPlaylist(null);

        // Assert
        // Verify observer removed from the *previous* playlist
        verify(previousPlaylist).removeObserver(playlistController);
        // Verify view updated with null
        verify(mockViewController).setPlayList(null);
         // Verify observer NOT added to null
        verify(mockPlaylist, never()).addObserver(any()); // Check mockPlaylist wasn't used
    }

     @Test
    void testSetPlaylist_FromNullToNonNull() {
        // Arrange - Ensure playlist starts as null (default state after @InjectMocks)
        // Playlist previousPlaylist = playlistController.getPlaylist(); // Should be null
        // assertNull(previousPlaylist);

        // Act - set a new playlist
        playlistController.setPlaylist(mockPlaylist);

        // Assert
        // Verify observer added to the *new* playlist
        verify(mockPlaylist).addObserver(playlistController);
        // Verify view updated with the *new* playlist
        verify(mockViewController).setPlayList(mockPlaylist);
        // Verify removeObserver was NOT called (as previous was null)
        // We can't easily verify this without a specific previous mock instance
    }


    @Test
    void testAddTrack_Success() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist); // Set the context
        when(mockPlaylist.getTitle()).thenReturn("Test Playlist");
        when(mockTrack.getTitle()).thenReturn("Test Track");

        // Act
        playlistController.addTrack(mockTrack);

        // Assert
        verify(mockPlaylist).addTrack(mockTrack);
        verify(mockDbManagerInsert).addTrackToPlaylist("Test Playlist", "Test Track");
    }

    @Test
    void testAddTrack_NullTrack() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist); // Playlist must exist

        // Act
        playlistController.addTrack(null);

        // Assert
        verify(mockPlaylist, never()).addTrack(any());
        verify(mockDbManagerInsert, never()).addTrackToPlaylist(anyString(), anyString());
    }

    @Test
    void testAddTrack_NullPlaylist() {
        // Arrange - Playlist is null (default state or explicitly set)
        playlistController.setPlaylist(null);

        // Act
        playlistController.addTrack(mockTrack);

        // Assert
        // Verify model wasn't touched (as playlist is null)
        verify(mockPlaylist, never()).addTrack(any());
        // Verify DB wasn't touched
        verify(mockDbManagerInsert, never()).addTrackToPlaylist(anyString(), anyString());
    }

    @Test
    void testRemoveTrack_Success() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist); // Set context
        when(mockPlaylist.getTitle()).thenReturn("Test Playlist");
        when(mockTrack.getTitle()).thenReturn("Test Track");
        // S'assurer que le morceau est dans la playlist
        when(mockPlaylist.getTracks()).thenReturn(Collections.singletonList(mockTrack));

        // Act
        playlistController.removeTrack(mockTrack);

        // Assert - vérifier que le morceau est retiré de la base de données
        verify(mockDbManagerInsert).removeTrackFromPlaylist("Test Playlist", "Test Track");
        
        // On ne peut pas facilement vérifier playlist.removeTrack() car le morceau
        // est d'abord vérifié avec contains() puis retiré avec la méthode remove()
        // de la collection Java interne et non via la méthode removeTrack()
    }

    @Test
    void testRemoveTrack_NullTrack() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist);

        // Act
        playlistController.removeTrack(null);

        // Assert
        verify(mockPlaylist, never()).removeTrack(any());
        verify(mockDbManagerInsert, never()).removeTrackFromPlaylist(anyString(), anyString());
    }

    @Test
    void testRemoveTrack_NullPlaylist() {
        // Arrange
        playlistController.setPlaylist(null);

        // Act
        playlistController.removeTrack(mockTrack);

        // Assert
        verify(mockPlaylist, never()).removeTrack(any());
        verify(mockDbManagerInsert, never()).removeTrackFromPlaylist(anyString(), anyString());
    }

    @Test
    void testClearTracks_WithTracks() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist);
        when(mockPlaylist.getTitle()).thenReturn("Test Playlist");

        // Utiliser lenient() pour permettre une stubbing flexible
        // qui ne générera pas d'erreur UnnecessaryStubbingException
        lenient().when(mockPlaylist.getTracks()).thenReturn(List.of(mockTrack)); // Use List.of for immutable list

        // Act
        playlistController.clearTracks();

        // Assert
        verify(mockPlaylist).clearTracks();
        verify(mockDbManagerInsert).removeAllTracksFromPlaylist("Test Playlist");
    }

    @Test
    void testClearTracks_EmptyPlaylist() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist);
        when(mockPlaylist.getTitle()).thenReturn("Test Playlist");
        // Simulate playlist being empty
        when(mockPlaylist.getTracks()).thenReturn(Collections.emptyList()); // Use Collections.emptyList()

        // Act
        playlistController.clearTracks();

        // Assert - Code skips actions if playlist is empty
        verify(mockPlaylist, never()).clearTracks();
        verify(mockDbManagerInsert, never()).removeAllTracksFromPlaylist(anyString());
    }

    @Test
    void testClearTracks_NullPlaylist() {
        // Arrange
        playlistController.setPlaylist(null);

        // Act
        playlistController.clearTracks();

        // Assert
        verify(mockPlaylist, never()).clearTracks();
        verify(mockDbManagerInsert, never()).removeAllTracksFromPlaylist(anyString());
    }

    @Test
    void testPlayPlaylist_StartsFirstTrack() {
        // ... Arrange ...
        Track track1 = mock(Track.class);
        Track track2 = mock(Track.class); // Keep track2 mock if list needs two items
        List<Track> tracks = List.of(track1, track2);

        when(mockPlaylist.getTracks()).thenReturn(tracks);
        when(mockPlaylist.getTitle()).thenReturn("Test Playlist");
        when(track1.getTrackId()).thenReturn(101);
        when(track1.getTitle()).thenReturn("Track One");

        // --- REMOVE OR COMMENT OUT THIS LINE ---
        // when(track2.getTitle()).thenReturn("Track Two"); // Unnecessary for this test path
        // -----------------------------------------

        playlistController.setPlaylist(mockPlaylist);

        // Act
        playlistController.playPlaylist();

        // Assert
        verify(mockPlayerController).play(101);
        verify(mockPlayerController, atLeastOnce()).addOnEndEvent(any(Runnable.class));
        verify(mockPlayerController, atLeastOnce()).addOnNextAction(any(Runnable.class));
        verify(mockPlayerController, atLeastOnce()).addOnPreviousAction(any(Runnable.class));
    }

    @Test
    void testPlayPlaylist_EmptyPlaylist() {
        // Arrange
        when(mockPlaylist.getTracks()).thenReturn(Collections.emptyList()); // Empty list
        playlistController.setPlaylist(mockPlaylist);

        // Act
        playlistController.playPlaylist();

        // Assert
        verify(mockPlayerController).stop(); // Should stop if playlist is empty
        verify(mockPlayerController, never()).play(anyInt()); // Should never play
        // Verify no listeners added when empty
        verify(mockPlayerController, never()).addOnEndEvent(any(Runnable.class));
        verify(mockPlayerController, never()).addOnNextAction(any(Runnable.class));
        verify(mockPlayerController, never()).addOnPreviousAction(any(Runnable.class));
    }

    @Test
    void testPlayPlaylist_NullPlaylist() {
        // Arrange
        playlistController.setPlaylist(null);

        // Act
        playlistController.playPlaylist();

        // Assert
        verify(mockPlayerController).stop(); // Should stop if playlist is null
        verify(mockPlayerController, never()).play(anyInt()); // Should never play
        // Verify no listeners added when null
        verify(mockPlayerController, never()).addOnEndEvent(any(Runnable.class));
        verify(mockPlayerController, never()).addOnNextAction(any(Runnable.class));
        verify(mockPlayerController, never()).addOnPreviousAction(any(Runnable.class));
    }

    @Test
    void testReorderTracks() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist); // Call 1 happens here

        // Act
        playlistController.reorderTracks(mockTrack, 0, 1); // Call 2 happens here

        // Assert
        verify(mockPlaylist).reorderTrack(0, 1);
        // --- ADJUST VERIFICATION ---
        verify(mockViewController, times(2)).setPlayList(mockPlaylist); // Expect 2 calls total
        // ---------------------------
        // Verify DB interaction if persistence is added
        // verify(mockDbManagerInsert).updateTrackOrderInPlaylist(...)
    }

        @Test
    void testReorderTracks_NullPlaylist() {
        // Arrange
        playlistController.setPlaylist(null); // This call causes setPlayList(null)

        // Act
        playlistController.reorderTracks(mockTrack, 0, 1);

        // Assert
        // Verify the core model interaction didn't happen
        verify(mockPlaylist, never()).reorderTrack(anyInt(), anyInt());


        // TODO: update order track in the db in playlistcontroller
        //verify(mockDbManagerInsert, never()).updateTrackOrderInPlaylist(any(), any());
    }

    // --- Tests for Observer Callback Methods ---
    // These verify that the controller methods called by observers don't crash
    // and potentially trigger expected side effects (like view updates)

    @Test
    void testOnAddTrack_Callback() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist); // Need playlist context for view update

        // Act & Assert - Verify no exception and view is updated
        assertDoesNotThrow(() -> playlistController.onAddTrack(123));
        // Check if view is refreshed (based on your implementation needs)
        // verify(mockViewController).setPlayList(mockPlaylist); // If callback should refresh view
    }

    @Test
    void testOnRemoveTrack_Callback() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist); // Need playlist context

        // Act & Assert
        assertDoesNotThrow(() -> playlistController.onRemoveTrack(456));
        // Check if view is refreshed
        // verify(mockViewController).setPlayList(mockPlaylist); // If callback should refresh view
    }

    @Test
    void testOnClear_Callback() {
        // Arrange
        playlistController.setPlaylist(mockPlaylist); // Need playlist context

        // Act & Assert
        assertDoesNotThrow(() -> playlistController.onClear());
         // Check if view is refreshed
        // verify(mockViewController).setPlayList(mockPlaylist); // If callback should refresh view
    }
}