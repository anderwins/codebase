package ulb.controller;


import ulb.model.MetadataManager;
import ulb.model.Track;
import ulb.model.TrackLibrary;
import ulb.view.MetaDataViewController;

import java.util.logging.Logger;


public class MetaDataController extends PageController implements MetaDataViewController.MetaDataObserver {
    private MetaDataViewController metaDataViewController;
    private final TrackLibrary trackLibrary;
    private static final Logger logger = Logger.getLogger(MetadataManager.class.getName());

    public MetaDataController(TrackLibrary library, MetaDataViewController viewController, MainController mainController) {
        // Initialisation du MetaDataViewController
        super(mainController);
        metaDataViewController = viewController;
        metaDataViewController.setObserver(this);
        this.trackLibrary = library;
    }

    public void setTrack(Track t) {
        metaDataViewController.setTrack(t);
    }

    @Override
    public void onMetaDataSave() {

        Track updatedTrack = metaDataViewController.getUpdatedTrack();
        logger.info("updating metadata");
        trackLibrary.get(updatedTrack.getTrackId()).assign(updatedTrack);
        this.mainController.goToHome(); // could use previous here (or not navigate at all)
        // show info to user that update was successfull
    }

    @Override
    public void onMetaDataCancel() {
        mainController.goToHome(); // could use previous here
    }
}
