package ulb.model;

public class Radio {
    private String title;
    private String streamUrl;

    // default constructor needed for json deserialization
    public Radio(){}

    // Constructor with parameters
    public Radio(String title, String streamUrl) {
        this.title = title;
        this.streamUrl = streamUrl;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    // Setters (optional, but useful for deserialization)
    public void setTitle(String title) {
        this.title = title;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
}