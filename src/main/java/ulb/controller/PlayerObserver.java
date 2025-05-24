package ulb.controller;

import ulb.model.Track;

public interface PlayerObserver {
    void changeTrack(Track t);
    void updatePlayPause(boolean isPlaying);
    void updateProgress(double progress, double timeElapsed);
    void updateLyrics(boolean showLyrics);
}
