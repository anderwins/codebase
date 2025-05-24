package ulb.controller.handleError;

public class PageExistsException extends Exception {
    public PageExistsException(String message) {
        super(message);
    }
}