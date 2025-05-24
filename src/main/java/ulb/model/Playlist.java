package ulb.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Playlist implements Iterable<Track> {
    private String title;        // Nom unique de la playlist
    protected List<Track> tracks;  // Liste des morceaux de la playlist

    private List<PlaylistObserver> observers = new ArrayList<>();
    
    /**
     * Reorders a track in the playlist by moving it from one position to another.
     * 
     * @param fromIndex the current index of the track
     * @param toIndex the target index where the track should be moved
     */
    public void reorderTrack(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= tracks.size() || toIndex < 0 || toIndex >= tracks.size() || fromIndex == toIndex) {
            return; // Invalid indices or no change needed
        }
        
        // Get the track to move
        Track trackToMove = tracks.get(fromIndex);
        
        // Remove from current position and add at new position
        tracks.remove(fromIndex);
        tracks.add(toIndex, trackToMove);
        
        // Notify observers about the reordering
        for (PlaylistObserver ob : this.observers) {
            ob.onReorderTrack(trackToMove.getTrackId(), fromIndex, toIndex);
        }
    }


    public Playlist(String title) {
        this.title = title;
        this.tracks = new ArrayList<>();
    }

    public void addTrack(Track track) {
        if (!tracks.contains(track)) { // si le ttrack ne se trouve  pas déja dans la Playist
            addTrackInternal(track);   
        }
    }

    protected void addTrackInternal(Track track) {
        tracks.add(track);
        for (PlaylistObserver ob : this.observers) {
            ob.onAddTrack(track.getTrackId());
        }
    } 

    public void clearTracks() {
        tracks.clear();
        for (PlaylistObserver ob : this.observers) {
            ob.onClear();
        }
    }

    public void removeTrack(Track track) {
        boolean removed = tracks.remove(track);
        if (removed) {
            for (PlaylistObserver ob : this.observers) {
                ob.onRemoveTrack(track.getTrackId());
            }
        }
    }

    public List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }

    public String getTitle() {
        return title;
    }

    public void addObserver(PlaylistObserver ob) {
        this.observers.add(ob);
    }

    @Override
    public Iterator<Track> iterator() {
        return tracks.iterator();
    }

    public void removeObserver(PlaylistObserver ob) {
        this.observers.remove(ob);
    }
}