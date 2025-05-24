package ulb.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code TrackLibrary} class manages a collection of {@link Track} objects.
 * It allows adding, retrieving, and managing tracks, while also notifying observers about changes in the library.
 */
public class TrackLibrary {

    /**
     * The {@code TrackLibraryObserver} interface defines the methods that observers must implement
     * to be notified about changes in the track library.
     */
    public interface TrackLibraryObserver {
        public void onAddTrack(Track t);
        public void onRemoveTrack(Track t);
    }

    private final List<TrackLibraryObserver> observers = new ArrayList<>();

    // Map storing the tracks by their ID
    private final Map<Integer, Track> tracks = new HashMap<>();

    public TrackLibrary() {}

    /**
     * Sets the tracks in the library, clearing the existing ones and adding the provided tracks.
     * Observers are notified when the tracks are added.
     *
     * @param tracks the list of tracks to set in the library.
     */
    public void setTracks(List<Track> tracks) {
        this.tracks.clear(); // clear existing tracks
        for (Track track : tracks) {
            this.addTrack(track); // add each track and notify observers
        }
    }

    /**
     * Adds a new track to the library and notifies all observers about it.
     *
     * @param t the track to add to the library.
     */
    public void addTrack(Track t) {
        this.tracks.put(t.getTrackId(), t);
        for (TrackLibraryObserver observer : observers) {
            observer.onAddTrack(t); // notify all observers about the addition
        }
    }

    /**
     * Retrieves a track from the library by its ID.
     *
     * @param id the ID of the track to retrieve.
     * @return the track with the specified ID, or {@code null} if not found.
     */
    public Track get(Integer id) {
        return tracks.get(id);
    }

    /**
     * Retrieves a list of all tracks in the library.
     *
     * @return a list of all tracks in the library.
     */
    public List<Track> getTracks() {
        return new ArrayList<>(tracks.values()); // return the list of all tracks
    }

    /**
     * Adds an observer to the library. The observer will be notified when tracks are added or removed.
     *
     * @param ob the observer to add.
     */
    public void addObserver(TrackLibraryObserver ob) {
        this.observers.add(ob);
    }

    /**
     * Removes an observer from the library. The observer will no longer be notified of track changes.
     *
     * @param ob the observer to remove.
     */
    public void removeObserver(TrackLibraryObserver ob) {
        this.observers.remove(ob);
    }
}
