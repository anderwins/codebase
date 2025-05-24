package ulb;
import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configuration du système de journalisation pour l'application.
 * Cette classe fournit des méthodes pour initialiser et configurer
 * le système de journalisation Java (java.util.logging).
 */
public class LoggerConfig {
    
    private static boolean isConfigured = false;
    private static AtomicBoolean fatalErrorOccurred = new AtomicBoolean(false);
    
    /**
     * Interface pour les gestionnaires d'erreurs critiques
     */
    public interface FatalErrorHandler {
        void handleFatalError(LogRecord record, String formattedMessage);
    }
    
    // Gestionnaire d'erreurs par défaut qui arrête l'application
    private static FatalErrorHandler defaultFatalErrorHandler = (record, message) -> {
        Logger logger = Logger.getLogger(LoggerConfig.class.getName());
        logger.info("ERREUR FATALE: " + message);
        logger.info("L'application va s'arrêter suite à une erreur critique.");
        //System.exit(1);
    };
    
    private static FatalErrorHandler userFatalErrorHandler = defaultFatalErrorHandler;
    
    /**
     * Configure le système de journalisation avec des gestionnaires de fichiers et de console.
     * Les journaux sont stockés dans le fichier "log/app.log".
     */
    public static void setup() {
        // Éviter les configurations multiples
        if (isConfigured) {
            return;
        }
        
        try {
            // Créer le dossier log s'il n'existe pas
            File dir = new File("log");
            if (!dir.exists()) {
                dir.mkdir();
            }
            
            // Configurer le gestionnaire de fichiers pour écraser le fichier au lieu d'ajouter
            FileHandler fileHandler = new FileHandler("log/app.log", false); // Changer 'true' en 'false'
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            
            // Configurer le gestionnaire de console avec un niveau INFO pour réduire le bruit
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            
            // Configurer notre gestionnaire d'erreurs critiques personnalisé
            SevereErrorHandler severeHandler = new SevereErrorHandler();
            severeHandler.setLevel(Level.SEVERE);
            
            // Configurer le logger racine
            Logger rootLogger = Logger.getLogger("");
            
            // Supprimer les gestionnaires par défaut pour éviter les doublons
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }
            
            // Ajouter nos gestionnaires personnalisés
            rootLogger.addHandler(fileHandler);
            rootLogger.addHandler(consoleHandler);
            rootLogger.addHandler(severeHandler);
            rootLogger.setLevel(Level.ALL);
            
            // Set the level for all loggers (including library loggers) to WARNING
            Logger.getLogger("").setLevel(Level.WARNING);


            rootLogger.info("✅ Logger initialisé avec succès - journaux stockés dans log/app.log");
            isConfigured = true;
            
        } catch (IOException e) {
            Logger logger = Logger.getLogger(LoggerConfig.class.getName());
            logger.severe("❌ Erreur de configuration du logger : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtient un logger configuré pour une classe spécifique.
     * Garantit que la configuration est appelée une seule fois.
     * 
     * @param clazz La classe pour laquelle obtenir un logger
     * @return Un logger configuré
     */
    public static Logger getLogger(Class<?> clazz) {
        setup();
        return Logger.getLogger(clazz.getName());
    }
    
    /**
     * Définit un gestionnaire d'erreurs critiques personnalisé.
     * 
     * @param handler Le gestionnaire d'erreurs à utiliser
     */
    public static void setFatalErrorHandler(FatalErrorHandler handler) {
        if (handler != null) {
            userFatalErrorHandler = handler;
        } else {
            userFatalErrorHandler = defaultFatalErrorHandler;
        }
    }
    
    /**
     * Vérifie si une erreur fatale s'est produite.
     * 
     * @return true si une erreur fatale s'est produite, false sinon
     */
    public static boolean hasFatalErrorOccurred() {
        return fatalErrorOccurred.get();
    }
    
    /**
     * Gestionnaire d'erreurs personnalisé qui détecte les erreurs de niveau SEVERE
     * et exécute une action spécifique comme arrêter l'application.
     */
    private static class SevereErrorHandler extends Handler {
        
        public SevereErrorHandler() {
            // Initialize the formatter to avoid NullPointerException
            setFormatter(new SimpleFormatter());
        }
        
        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().equals(Level.SEVERE)) {
                // Marquer qu'une erreur fatale s'est produite
                fatalErrorOccurred.set(true);
                
                // Formatter le message
                String message = getFormatter().format(record);
                
                // Déléguer au gestionnaire d'erreurs défini par l'utilisateur
                userFatalErrorHandler.handleFatalError(record, message);
            }
        }
        
        @Override
        public void flush() {
            // Pas besoin d'implémenter
        }
        
        @Override
        public void close() throws SecurityException {
            // Pas besoin d'implémenter
        }
        
        @Override
        public synchronized void setFormatter(java.util.logging.Formatter formatter) {
            super.setFormatter(formatter);
        }
    }
}

