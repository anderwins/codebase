package ulb.model;

/**
 * Interface pour observer les changements sur le contenu d'une playlist individuelle.
 * <p>
 * Permet d'être notifié lorsqu'un morceau est ajouté, retiré, la playlist vidée ou les morceaux réordonnés.
 * Typiquement utilisée par les vues ou contrôleurs qui doivent se mettre à jour en temps réel
 * lorsqu'une playlist spécifique change (ex : PlaylistViewController).
 */
public interface PlaylistObserver {
    /**
     * Appelé lorsqu'un morceau est ajouté à la playlist.
     * @param id L'identifiant du morceau ajouté
     */
    public void onAddTrack(Integer id);

    /**
     * Appelé lorsqu'un morceau est retiré de la playlist.
     * @param id L'identifiant du morceau retiré
     */
    public void onRemoveTrack(Integer id);

    /**
     * Appelé lorsque la playlist est vidée de tous ses morceaux.
     */
    public void onClear();

    /**
     * Appelé lorsqu'un morceau est réordonné dans la playlist.
     * @param id L'identifiant du morceau réordonné
     * @param fromIndex Index de départ
     * @param toIndex Index d'arrivée
     */
    public default void onReorderTrack(Integer id, int fromIndex, int toIndex) {
        // Implémentation par défaut : ne fait rien
        // Cette méthode par défaut évite de casser les implémentations existantes
    }
}
