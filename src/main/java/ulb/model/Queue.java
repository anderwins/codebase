package ulb.model;

import ulb.dao.DbManagerInsert;

import java.util.Iterator;

public class Queue extends Playlist {
    /**
     * Constructeur initialisant une playlist.
     *
     * @param playListTitle Nom unique de la playlist. Gestion des insertions dans la base des donneés
     */
    public Queue(String playListTitle) {
        super(playListTitle);
    }

    @Override
    public Iterator<Track> iterator() {
        return new QueueIterator();
    }

    private class QueueIterator implements Iterator<Track> {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            return currentIndex < tracks.size();
        }

        @Override
        public Track next() {
            Track temp = tracks.get(currentIndex);
            currentIndex++;
            return temp;
        }
    }

    @Override
    public void addTrack(Track track) {
        // Check if the track is already in the queue before adding it
        if (!tracks.contains(track)) {
            addTrackInternal(track);
        }
    }
}
