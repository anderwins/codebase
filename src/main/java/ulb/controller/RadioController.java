package ulb.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ulb.GuiMain;
import ulb.model.Radio;
import ulb.view.RadioViewController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RadioController extends PageController implements RadioViewController.radioObserver {

    private final AudioPlayerController audioPlayerController = GuiMain.audioPlayerController;
    public static final Logger logger = Logger.getLogger(RadioViewController.class.getName());

    public RadioController(RadioViewController viewController, MainController mainController) {
        super(mainController);
        viewController.setObserver(this);
        List<Radio> radios = loadRadios();
        viewController.displayRadios(radios);
    }

    @Override
    public void onRadioSelected(String url) {
        audioPlayerController.playStream(url);
    }

    public List<Radio> loadRadios() {
        List<Radio> radios = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream input = getClass().getResourceAsStream("/radio/fluxRadios.json")) {
            if (input == null) {
                logger.warning("Fichier radios.json introuvable");
                return radios;
            }

            radios = objectMapper.readValue(input, new TypeReference<List<Radio>>() {});
        } catch (IOException e) {
            logger.severe("Erreur lors de la lecture du fichier radios.json: " + e.getMessage());
        }

        return radios;
    }
}