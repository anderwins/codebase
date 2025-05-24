package ulb.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe qui gère la liste des playlists et notifie les observateurs des changements.
 * Implémente le pattern Singleton pour être accessible facilement.
 */
public class PlaylistManager {
    private static PlaylistManager instance;
    private List<Playlist> playlists;
    private List<PlaylistsObserver> observers;

    private PlaylistManager() {
        this.playlists = new ArrayList<>();
        this.observers = new ArrayList<>();
    }

    /**
     * Retourne l'instance unique de PlaylistManager (pattern Singleton).
     * @return L'instance de PlaylistManager
     */
    public static synchronized PlaylistManager getInstance() {
        if (instance == null) {
            instance = new PlaylistManager();
        }
        return instance;
    }

    /**
     * Ajoute une playlist à la liste et notifie les observateurs.
     * @param playlist La playlist à ajouter
     */
    public void addPlaylist(Playlist playlist) {
        if (!playlists.contains(playlist)) {
            playlists.add(playlist);
            notifyPlaylistAdded(playlist);
        }
    }

    // TODO optional return type
    public Playlist findPlaylistWithTitle(String title) {
        for (Playlist playlist : playlists) {
            if (playlist.getTitle().equals(title)) {
                return playlist;
            }
        }
        return null;
    }

    /**
     * Supprime une playlist de la liste et notifie les observateurs.
     * @param playlist La playlist à supprimer
     */
    public void removePlaylist(Playlist playlist) {
        if (playlists.remove(playlist)) {
            notifyPlaylistRemoved(playlist);
        }
    }

    /**
     * Retourne la liste des playlists.
     * @return La liste des playlists
     */
    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists);
    }

    /**
     * Ajoute un observateur à la liste des observateurs.
     * @param observer L'observateur à ajouter
     */
    public void addObserver(PlaylistsObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Supprime un observateur de la liste des observateurs.
     * @param observer L'observateur à supprimer
     */
    public void removeObserver(PlaylistsObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifie tous les observateurs qu'une playlist a été ajoutée.
     * @param playlist La playlist ajoutée
     */
    private void notifyPlaylistAdded(Playlist playlist) {
        for (PlaylistsObserver observer : observers) {
            observer.onPlaylistAdded(playlist);
        }
    }

    /**
     * Notifie tous les observateurs qu'une playlist a été supprimée.
     * @param playlist La playlist supprimée
     */
    private void notifyPlaylistRemoved(Playlist playlist) {
        for (PlaylistsObserver observer : observers) {
            observer.onPlaylistRemoved(playlist);
        }
    }

    /**
     * Notifie tous les observateurs qu'une playlist a été modifiée.
     * @param playlist La playlist modifiée
     */
    public void notifyPlaylistAltered(Playlist playlist) {
        for (PlaylistsObserver observer : observers) {
            observer.onPlaylistAltered(playlist);
        }
    }

}