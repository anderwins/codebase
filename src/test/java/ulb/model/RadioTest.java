package ulb.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ulb.model.Radio;
import java.lang.reflect.Type;


import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.List;

public class RadioTest {
    private Radio radio;

    @BeforeEach
    public void setUp() {
        radio = new Radio("Test Radio", "http://testurl.com/stream");
    }

    @Test
    public void testRadioCreation() {
        assertEquals("Test Radio", radio.getTitle());
        assertEquals("http://testurl.com/stream", radio.getStreamUrl());
    }

    @Test
    public void testRadioTitleUpdate() {
        radio.setTitle("New Title");
        assertEquals("New Title", radio.getTitle());
    }

    @Test
    public void testRadioStreamUrlUpdate() {
        radio.setStreamUrl("http://newurl.com/stream");
        assertEquals("http://newurl.com/stream", radio.getStreamUrl());
    }

    @Test
    public void testLoadValidJson() {
        String fakeJson = "[{\"title\":\"Radio One\",\"streamUrl\":\"http://radio1.com\"}, {\"title\":\"Radio Two\",\"streamUrl\":\"http://radio2.com\"}]";

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Radio>>(){}.getType();
        List<Radio> radios = gson.fromJson(new StringReader(fakeJson), listType);

        assertEquals(2, radios.size());
        assertEquals("Radio One", radios.get(0).getTitle());
        assertEquals("http://radio2.com", radios.get(1).getStreamUrl());
    }

    @Test
    public void testLoadInvalidJson() {
        String badJson = "[{\"title\": \"Broken Radio\", \"streamUrl\": }"; // syntaxe cassée

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Radio>>(){}.getType();

        try {
            gson.fromJson(new StringReader(badJson), listType);
            fail("Une exception était attendue");
        } catch (Exception e) {
            assertTrue(e instanceof com.google.gson.JsonSyntaxException);
        }
    }

}