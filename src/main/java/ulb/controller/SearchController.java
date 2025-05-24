package ulb.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import ulb.dao.DbManagerSearch;
import ulb.services.AppServices;
import ulb.view.SearchViewController;
import ulb.model.*;

public class SearchController extends PageController implements SearchViewController.SearchObserver {
    private DbManagerSearch searchManager;
    private SearchViewController searchViewController;

    public SearchController(SearchViewController viewController, MainController mainController) {
        super(mainController);
        // Initialisation du SearchManager
        searchManager = AppServices.getDbSearch();
        searchViewController = viewController;
        viewController.setObserver(this);
    }

    // Méthode de recherche qui met à jour les résultats
    public void onSearch(String query, SearchType type) {
        System.out.println("search controller onSearch");

        // sync db with changes
        // should be the responsibility of db code to decide when is a sync necessary
        AppServices.getDbUpdate().syncTracks();

        List<Track> results;
        Map<SearchType, Function<String, List<Track>>> searchMap = Map.of(
            SearchType.TITLE, searchManager::searchTracksByTitle,
            SearchType.ALBUM, searchManager::searchTracksByAlbum,
            SearchType.ARTIST, searchManager::searchTracksByArtist
        );

        switch(type){
            case TITLE:
            case ALBUM:
            case ARTIST:
                results = searchMap.get(type).apply(query);
                break;
            case ALL:
                results = searchManager.searchTracksByTitle(query);
                results.addAll(searchManager.searchTracksByAlbum(query));
                results.addAll(searchManager.searchTracksByArtist(query));
                break;
            case PLAYLIST:
            default:
                results = searchManager.searchTracksByTitle(query);

        }
        System.out.println("found " + results.size() + " results");
        searchViewController.setResult(results);
    }
}
