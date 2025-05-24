package ulb.controller;

import ulb.model.Track;

public class TrackController {
    private final MainController mainController;
    private final QueueController queueController;

    private final Track track;

    public TrackController(Track t, MainController mainController, QueueController queueController){
        this.mainController = mainController;
        this.queueController = queueController;
        this.track = t;
    }

    public void onMetadataClick(){
        mainController.goToChangeMetaData(this.track);
    }

    public void onTrackClick(){
        queueController.playSingleTrack(this.track);
    }

    public void onAddToQueue(){
        queueController.addTrack(this.track);
    }
}
