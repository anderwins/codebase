package ulb.model;

public class RadioTest {
    private String title;
    private String streamUrl;

    public RadioTest(String title, String streamUrl) {
        this.title = title;
        this.streamUrl = streamUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getStreamUrl() {
        return streamUrl;
    }
}
