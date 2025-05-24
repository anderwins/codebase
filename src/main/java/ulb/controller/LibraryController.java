package ulb.controller;


import ulb.model.Track;
import ulb.model.TrackLibrary;
import ulb.view.LibraryViewController;

/**
 * The {@code LibraryController} controls the view of the library of the user. It listens to user actions from the
 * The controller retrieves the list of tracks from the {@link MainModel} and updates the view
 * accordingly.
 */
public class LibraryController extends PageController {

    /** The view controller associated with this controller. */
    private LibraryViewController viewController;
    private TrackLibrary tracksLibrary;

    /**
     * Constructs a new {@code LibraryController} and start observing the provided
     * {@code LibraryViewController}.
     *
     * @param controller The {@code LibraryViewController} that this controller will manage.
     */
    public LibraryController(TrackLibrary library, LibraryViewController controller, MainController mainController) {
        super(mainController);
        viewController = controller;
        viewController.setController(this);
        this.tracksLibrary = library;
        this.viewController.setTracks(tracksLibrary.getTracks());
    }

    public TrackController getTrackFrontController(Track t){
        return new TrackController(t, this.mainController, this.mainController.getQueue());
    }
}
