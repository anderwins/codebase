package ulb.controller;

public class PlayListFrontController {

    private MainController mainController;

    PlayListFrontController(MainController mainController){
        this.mainController = mainController;
    }

    public void onPlaylistClicked(){
        mainController.goToPlaylist();
    }
}
