package ulb.model;

import java.util.*;
import java.util.logging.Logger;

/**
 * Classe représentant un morceau de musique.
 * Cette classe stocke les informations essentielles sur un morceau telles que :
 * - L'identifiant unique du morceau
 * - Le titre du morceau
 * - Le nom de l'artiste
 * - L'album auquel il appartient
 * - L'année de sortie
 * - La durée du morceau en secondes
 * - Le genre musical
 * - Le chemin du fichier audio stocké localement
 * - Le chemin de l'image de couverture
 */
public class Track {

    public interface TrackObserver {
        public void onChangeData(Track t);
    }

    private int trackId;      // Identifiant unique du morceau
    private String title;     // Titre du morceau
    private String artist;    // Nom de l'artiste
    private String album;     // Nom de l'album
    private String year;      // Année de sortie
    private int duration;     // Durée du morceau en secondes
    private String genre;     // Genre musical
    private String filePath;  // Chemin vers le fichier audio
    private String coverPath; // Chemin vers l'image de couverture
    private String lyricsPath; // chemin vers le fichier txt contenant les paroles du morceau 
    private String karaokePath; // chemin vers le fichier lrc contenant les paroles synchronisés avec l'audio (sec:mots)

    List<TrackObserver> observers = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(MetadataManager.class.getName());

    /**
     * Constructeur complet pour créer un objet `Track`.
     *
     * @param trackId    Identifiant unique du morceau (optionnel, peut être défini après l'ajout en base).
     * @param title      Titre du morceau
     * @param artist     Nom de l'artiste
     * @param album      Nom de l'album
     * @param year       Année de sortie
     * @param duration   Durée du morceau en secondes
     * @param genre      Genre musical
     * @param filePath   Chemin du fichier audio
     * @param coverPath  Chemin du fichier image de couverture
     * @param lyricsPath chemin vers le fichier txt contenant les paroles du morceau
     */

    /*
     * Constructeur avec l'identifiant (utilisé après l'ajout en base).
     */
    public Track(int trackId, String title, String artist, String album, String year, int duration, String genre, String filePath, String coverPath, String lyricsPath, String karaokePath) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.year = year;
        this.duration = duration;
        this.genre = genre;
        this.filePath = filePath;
        this.coverPath = coverPath;
        this.lyricsPath = lyricsPath;
        this.karaokePath = karaokePath;
    }

    /**
     * Constructeur sans l'identifiant (utilisé avant l'ajout en base).
     */
    public Track(String title, String artist, String album, String year, int duration, String genre, String filePath, String coverPath, String lyricsPath, String karaokePath) {
        this(0, title, artist, album, year, duration, genre, filePath, coverPath, lyricsPath, karaokePath);
    }

    public Track(Track t){
        this.assign(t);
    }

    // Getters
    public Integer getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getYear() {
        return year;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getGenre() {
        return genre;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public String getLyricsPath() {
        return lyricsPath;
    }

    public String getKaraokePath() {
        return karaokePath;
    }

    public Map<String, Object> getMetadata() {
        return Map.of("title", title, "artist", artist, "album", album, "year", year, "duration", duration, "genre", genre);
    }

    // Setters
    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyChangeMetadata();
    }

    public void setArtist(String artist) {
        this.artist = artist;
        notifyChangeMetadata();
    }

    public void setAlbum(String album) {
        this.album = album;
        notifyChangeMetadata();
    }

    public void setYear(String year) {
        this.year = year;
        notifyChangeMetadata();
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setGenre(String genre) {
        this.genre = genre;
        notifyChangeMetadata();
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public void setLyricsPath(String lyricsPath) {
        this.lyricsPath = lyricsPath;
    }

    public void setKaraokePath(String karaokePath) {
        this.karaokePath = karaokePath;
    }

    public void addObserver(TrackObserver ob) {
        this.observers.add(ob);
    }

    public void removeObservers(TrackObserver ob) {
        observers.remove(ob);
    }

    private void notifyChangeMetadata() {
        Iterator<TrackObserver> iterator = observers.iterator();
        while (iterator.hasNext()) {
            TrackObserver observer = iterator.next();
            if (observer != null) {
                observer.onChangeData(this);
            } else {
                iterator.remove();  // This line ensures that null references are removed
            }
        }
    }

    public void assign(Track t){

        if(isEqual(t)){
            logger.info("nothing to change");
            return;
        }

        this.trackId = t.trackId;
        this.title = t.title;
        this.artist = t.artist;
        this.album = t.album;
        this.year = t.year;
        this.duration = t.duration;
        this.genre = t.genre;
        this.filePath = t.filePath;
        this.coverPath = t.coverPath;
        this.lyricsPath = t.lyricsPath;
        this.karaokePath = t.karaokePath;

        notifyChangeMetadata();
    }

    public boolean isEqual(Track t) {
        if (this.trackId != t.trackId) return false;
        if (!Objects.equals(this.title, t.title)) return false;
        if (!Objects.equals(this.artist, t.artist)) return false;
        if (!Objects.equals(this.album, t.album)) return false;
        if (!Objects.equals(this.year, t.year)) return false;
        if (this.duration != t.duration) return false;
        if (!Objects.equals(this.genre, t.genre)) return false;
        if (!Objects.equals(this.filePath, t.filePath)) return false;
        if (!Objects.equals(this.coverPath, t.coverPath)) return false;
        if (!Objects.equals(this.lyricsPath, t.lyricsPath)) return false;
        if (!Objects.equals(this.karaokePath, t.karaokePath)) return false;

        return true;
    }


    /**
     * Affiche les informations du morceau sous forme de chaîne.
     *
     * @return Informations formatées sur le morceau.
     */
    @Override
    public String toString() {
        return "🎵 " + title + " - " + artist + " (" + album + ", " + year + ") [" + duration + " sec, " + genre + "]\n";
    }
}

