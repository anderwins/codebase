package ulb;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe de test pour vérifier le fonctionnement du système de journalisation.
 * Cette classe effectue différents types de logs pour s'assurer que la configuration fonctionne.
 */
public class LoggerTest {
    public static void main(String[] args) {
        // Utilisation de la méthode améliorée pour obtenir un logger
        Logger logger = LoggerConfig.getLogger(LoggerTest.class);
        
        logger.info("📝 INFO: Test de journalisation depuis LoggerTest");
        logger.warning("⚠️ WARNING: Ceci est un avertissement de test");
        logger.severe("🔴 SEVERE: Ceci est une erreur de test");
        
        // Test d'un message de niveau plus bas (DEBUG/FINE) qui devrait apparaître dans le fichier mais pas dans la console
        logger.fine("🔍 FINE/DEBUG: Ce message ne devrait apparaître que dans le fichier log");
        
        try {
            // Provoquer une exception pour tester la journalisation d'exceptions
            int result = 10 / 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "🐞 Exception testée pour la journalisation", e);
        }
        
        System.out.println("Test de journalisation terminé. Vérifiez le fichier log/app.log");
    }
}
