package ulb.model;

/**
 * Interface pour observer les changements dans la liste des playlists.
 * Permet d'être notifié lorsqu'une playlist est ajoutée ou supprimée.
 */
public interface PlaylistsObserver {
    /**
     * Appelé lorsqu'une nouvelle playlist est ajoutée
     * @param playlist La playlist ajoutée
     */
    void onPlaylistAdded(Playlist playlist);
    
    /**
     * Appelé lorsqu'une playlist est supprimée
     * @param playlist La playlist supprimée
     */
    void onPlaylistRemoved(Playlist playlist);

    /**
     * Appelé lorsqu'une playlist est modifiée
     * @param playlist La playlist modifiée
     */
    void onPlaylistAltered(Playlist playlist);
}