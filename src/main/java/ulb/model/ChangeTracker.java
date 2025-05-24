package ulb.model;

import ulb.dao.DbInitializer;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The {@code ChangeTracker} class is responsible for keeping tracks of modified tracks.
 * It is able to listen to a TrackLibrary and to the {@link Track} objects added to that library.
 * When a track fires a data change event, it keeps track of it until manually reset with clearChanges()
 */
public class ChangeTracker implements TrackLibrary.TrackLibraryObserver, Track.TrackObserver {

     // Set of modified track IDs
     private Set<Integer> modifiedTracks = new HashSet<>();

     public static final Logger logger = Logger.getLogger(DbInitializer.class.getName());

     public ChangeTracker() {}

     /**
      * Returns the set of modified track IDs.
      *
      * @return a {@link Set} containing the IDs of the modified tracks.
      */
     public Set<Integer> getTracksChanged() {
          return modifiedTracks;
     }

     /**
      * Marks a track as modified by adding its ID to the set of modified tracks.
      *
      * @param id the ID of the track that has been modified.
      */
     public void invalidateTrack(Integer id) {
          modifiedTracks.add(id);
          // Optional: Printing unsynchronized tracks (debugging purpose)
          logger.info("unsync tracks:" + modifiedTracks);
     }

     /**
      * Clears all the tracked changes, removing all modified track IDs from the set.
      */
     public void clearChanges() {
          this.modifiedTracks.clear();
     }

     /**
      * Callback method when a track is added to the library.
      * Registers this {@code ChangeTracker} as an observer of the given track.
      *
      * @param t the track that has been added.
      */
     @Override
     public void onAddTrack(Track t) {
          t.addObserver(this);
     }

     /**
      * Callback method when a track is added to the library.
      * Remove this {@code ChangeTracker} from observers of the given track
      *
      * @param t the track that has been removed.
      */
     @Override
     public void onRemoveTrack(Track t) {
          t.removeObservers(this);
     }

     /**
      * Callback method when a track's data has been changed.
      * Invalidate the track by adding its ID to the modified tracks set.
      *
      * @param t the track whose data has been modified.
      */
     @Override
     public void onChangeData(Track t) {
          invalidateTrack(t.getTrackId());
     }
}
