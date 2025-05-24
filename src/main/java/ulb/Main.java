package ulb;

import java.io.File;
import java.util.logging.Logger;

public class Main {
    /**
     * Main class of the app, it just wraps GuiMain
     * This is needed to be able to make a jar containing javafx, if we try to use GuiMain directly,
     * javafx will not be included in the .jar (its is a known problem)
     * */
    public static void main(final String[] args) {
        LoggerConfig.setup();

        try {
            Config.setUpFolders();
        }catch(Config.CouldNotSetUpDataFolder e){
            return;
        }

        GuiMain.main(args);
    }
}