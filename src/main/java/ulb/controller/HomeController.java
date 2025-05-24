// src/main/java/ulb/controller/HomeController.java
package ulb.controller;

import ulb.view.HomeViewController;

public class HomeController extends PageController implements HomeViewController.HomeObserver {

    private HomeViewController homeViewController;

    public HomeController(HomeViewController viewController, MainController mainController) {
        super(mainController);
        homeViewController = viewController;
        homeViewController.setObserver(this);
    }

    @Override
    public void handleSongClick(String fileName) {
        System.out.println("Album card " + fileName + " cliquée");
    }

    @Override
    public void searchByTag(String tag) {
        System.out.println("Recherche pour le tag : " + tag);
    }

    @Override
    public void handlePlaylistClick(String playlistName) {
        System.out.println("Playlist " + playlistName + " cliquée");
    }
}
