package ulb.controller;

public abstract class PageController {
    protected MainController mainController;

    public PageController(MainController mainController){
        this.mainController = mainController;
    }
}
