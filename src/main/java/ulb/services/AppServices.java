package ulb.services;

import ulb.dao.*;
import ulb.model.*;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppServices {
    private static final Logger logger = Logger.getLogger(AppServices.class.getName());

    private static DbInitializer dbInitializer;
    private static DbManagerInsert dbInsert;
    private static DbManagerSearch dbSearch;
    private static DbManagerUpdate dbUpdate;
    private static DatabaseSeeder dbSeeder;
    private static MetadataManager metadataManager;

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        try {
            dbInitializer = new DbInitializer();
            logger.info("[INFO] DbInitializer initialized");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[ERROR] Échec de l'initialisation de la base de données", e);
        }

        try {
            Connection connection = dbInitializer.getConnection();
            dbInsert = new DbManagerInsert(connection);
            logger.info("[INFO] DbManagerInsert initialized");

            dbSearch = new DbManagerSearch(connection);
            logger.info("[INFO] DbManagerSearch initialized");

            dbUpdate = new DbManagerUpdate(connection, dbInsert);
            logger.info("[INFO] DbManagerUpdate initialized");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[ERROR] Erreur lors de la configuration des gestionnaires de base de données", e);
        }

        try {
            metadataManager = new MetadataManager();
            dbSeeder = new DatabaseSeeder(dbInsert, metadataManager);
            logger.info("[INFO] MetadataManager et DatabaseSeeder initialisés");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[ERROR] Échec de l'initialisation de MetadataManager ou DatabaseSeeder", e);
        }

        initialized = true;
        logger.info("[INFO] Tous les services AppServices sont prêts");
    }

    public static DbInitializer getDbInitializer() {
        return dbInitializer;
    }

    public static DbManagerInsert getDbInsert() {
        return dbInsert;
    }

    public static DbManagerSearch getDbSearch() {
        return dbSearch;
    }

    public static DbManagerUpdate getDbUpdate() {
        return dbUpdate;
    }

    public static DatabaseSeeder getDbSeeder() {
        return dbSeeder;
    }

    public static MetadataManager getMetadataManager() {
        return metadataManager;
    }

    public static void close() {
        if (dbInitializer != null) {
            logger.info("[INFO] Fermeture des services AppServices et de la connexion à la base de données.");
            dbInitializer.closeConnection();
        } else {
            logger.warning("[WARNING] Tentative de fermeture des services AppServices, mais DbInitializer n'a pas été initialisé.");
        }
        initialized = false; // Reset initialization status
    }
}
