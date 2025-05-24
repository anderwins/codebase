package ulb.view.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import ulb.LoggerConfig;

/**
 * Gestionnaire d'alertes pour l'interface graphique.
 * Cette classe fournit des méthodes statiques pour afficher différents types d'alertes
 * dans l'interface utilisateur, remplaçant ainsi les messages d'erreur dans la console.
 */
public class AlertManager {
    private static final Logger logger = Logger.getLogger(AlertManager.class.getName());
    
    // Options pour la gestion des erreurs critiques
    private static boolean exitOnCriticalError = true;
    private static int maxCriticalErrors = 3;
    private static int criticalErrorCount = 0;
    
    // Nouvelle option pour désactiver les alertes visuelles (utile pour les tests)
    private static boolean alertsDisabled = false;
    
    // Auto-détection de l'environnement de test
    static {
        // Détection automatique d'un environnement de test
        try {
            // Vérifier si nous sommes dans un environnement de test
            boolean isTestEnvironment = isInTestEnvironment();
            if (isTestEnvironment) {
                alertsDisabled = true;
                logger.info("Environnement de test détecté. Alertes visuelles désactivées automatiquement.");
            }
        } catch (Exception e) {
            logger.warning("Erreur lors de la détection de l'environnement de test: " + e.getMessage());
        }
        
        // Enregistrer notre gestionnaire d'erreurs critiques
        LoggerConfig.setFatalErrorHandler((record, message) -> handleCriticalError(record, message));
    }
    
    /**
     * Détecte si nous sommes dans un environnement de test JUnit
     */
    private static boolean isInTestEnvironment() {
        // Vérifier la présence de JUnit dans la stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("org.junit.") || 
                element.getClassName().contains("Test")) {
                return true;
            }
        }
        
        // Vérifier les propriétés système qui pourraient indiquer un environnement de test
        try {
            return Boolean.getBoolean("junit.jupiter.execution.parallel.enabled") || 
                   System.getProperty("java.class.path").contains("junit") ||
                   System.getProperty("java.class.path").contains("surefire");
        } catch (Exception e) {
            // En cas d'erreur, on suppose que non
            return false;
        }
    }
    
    /**
     * Active ou désactive les alertes visuelles.
     * Utile pour les environnements de test où JavaFX n'est pas initialisé.
     * 
     * @param disabled true pour désactiver les alertes visuelles, false pour les activer
     */
    public static void setAlertsDisabled(boolean disabled) {
        alertsDisabled = disabled;
    }
    
    /**
     * Vérifie si les alertes sont désactivées
     * 
     * @return true si les alertes sont désactivées, false sinon
     */
    public static boolean isAlertsDisabled() {
        return alertsDisabled;
    }

    /**
     * Configure si l'application doit se terminer après une erreur critique
     * 
     * @param exitOnError true pour terminer l'application, false pour continuer
     */
    public static void setExitOnCriticalError(boolean exitOnError) {
        exitOnCriticalError = exitOnError;
    }
    
    /**
     * Configure le nombre maximum d'erreurs critiques avant de forcer la fermeture
     * 
     * @param maxErrors le nombre maximum d'erreurs critiques tolérées
     */
    public static void setMaxCriticalErrors(int maxErrors) {
        maxCriticalErrors = maxErrors;
    }

    /**
     * Affiche une alerte d'erreur simple avec un titre et un message
     * 
     * @param title   Le titre de l'alerte
     * @param content Le message d'erreur à afficher
     */
    public static void showError(String title, String content) {
        showAlert(AlertType.ERROR, title, content, null);
    }

    /**
     * Affiche une alerte d'erreur critique qui peut terminer l'application
     * 
     * @param title   Le titre de l'alerte
     * @param content Le message d'erreur à afficher
     * @param exitApplication true pour terminer l'application après affichage
     */
    public static void showCriticalError(String title, String content, boolean exitApplication) {
        // Log en SEVERE pour déclencher le gestionnaire d'erreurs critiques
        logger.log(Level.SEVERE, content);
        
        // Si on ne veut pas terminer immédiatement, on laisse le handler s'en occuper
        if (!exitApplication) {
            showErrorWithAction(title, content, "Continuer", null);
        }
    }

    /**
     * Affiche une alerte d'erreur détaillée avec les informations de l'exception
     * 
     * @param title     Le titre de l'alerte
     * @param content   Le message d'erreur à afficher
     * @param exception L'exception à afficher en détail
     */
    public static void showErrorWithException(String title, String content, Throwable exception) {
        showAlertWithException(AlertType.ERROR, title, content, exception);
    }
    
    /**
     * Affiche une alerte d'erreur critique détaillée avec les informations de l'exception
     * 
     * @param title     Le titre de l'alerte
     * @param content   Le message d'erreur à afficher
     * @param exception L'exception à afficher en détail
     * @param exitApplication true pour terminer l'application après affichage
     */
    public static void showCriticalErrorWithException(String title, String content, 
                                                     Throwable exception, boolean exitApplication) {
        // Log en SEVERE pour déclencher le gestionnaire d'erreurs critiques
        logger.log(Level.SEVERE, content, exception);
        
        if (!exitApplication) {
            showAlertWithExceptionAndAction(AlertType.ERROR, title, content, exception, "Continuer", null);
        }
    }

    /**
     * Affiche une alerte d'information simple
     * 
     * @param title   Le titre de l'alerte
     * @param content Le message d'information à afficher
     */
    public static void showInfo(String title, String content) {
        showAlert(AlertType.INFORMATION, title, content, null);
    }

    /**
     * Affiche une alerte d'avertissement simple
     * 
     * @param title   Le titre de l'alerte
     * @param content Le message d'avertissement à afficher
     */
    public static void showWarning(String title, String content) {
        showAlert(AlertType.WARNING, title, content, null);
    }

    /**
     * Affiche une boîte de dialogue de confirmation et retourne le choix de l'utilisateur
     * 
     * @param title     Le titre de la confirmation
     * @param content   Le message de confirmation à afficher
     * @param yesButtonText Le texte sur le bouton de confirmation (ou null pour "OK")
     * @param noButtonText  Le texte sur le bouton d'annulation (ou null pour "Annuler")
     * @return true si l'utilisateur confirme, false sinon
     */
    public static boolean showConfirmation(String title, String content, String yesButtonText, String noButtonText) {
        // Si les alertes sont désactivées, simplement retourner true comme si l'utilisateur avait confirmé
        if (alertsDisabled) {
            logger.info("Confirmation désactivée (" + title + "): " + content);
            return true;
        }
        
        // Vérifier si JavaFX est disponible
        if (!isJavaFxAvailable()) {
            logger.info("JavaFX non disponible, confirmation simulée: " + content);
            return true;
        }
        
        // Créer les boutons personnalisés si spécifiés
        ButtonType yesButton = (yesButtonText != null) ? new ButtonType(yesButtonText) : ButtonType.OK;
        ButtonType noButton = (noButtonText != null) ? new ButtonType(noButtonText) : ButtonType.CANCEL;
        
        Alert alert = new Alert(AlertType.CONFIRMATION, content, yesButton, noButton);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }
    
    /**
     * Affiche une alerte d'erreur avec boutons d'action personnalisés
     *
     * @param title Le titre de l'alerte
     * @param content Le message d'erreur
     * @param primaryButtonText Texte du bouton principal (ou null pour "OK")  
     * @param secondaryButtonText Texte du bouton secondaire (ou null si pas de bouton secondaire)
     * @return true si le bouton principal a été cliqué, false sinon
     */
    public static boolean showErrorWithAction(String title, String content, 
                                            String primaryButtonText, String secondaryButtonText) {
        // Journaliser d'abord le message
        logger.severe(title + ": " + content);
        
        // Si les alertes sont désactivées, simplement retourner true comme si l'utilisateur avait cliqué le bouton principal
        if (alertsDisabled) {
            logger.info("Alerte d'action désactivée: " + title + " - " + content);
            return true;
        }
        
        // Vérifier si JavaFX est disponible
        if (!isJavaFxAvailable()) {
            logger.info("JavaFX non disponible, action simulée: " + content);
            return true;
        }
        
        ButtonType primaryButton = (primaryButtonText != null) ? 
                                  new ButtonType(primaryButtonText) : ButtonType.OK;
        ButtonType[] buttons;
        
        if (secondaryButtonText != null) {
            ButtonType secondaryButton = new ButtonType(secondaryButtonText);
            buttons = new ButtonType[] { primaryButton, secondaryButton };
        } else {
            buttons = new ButtonType[] { primaryButton };
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];
        
        try {
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(AlertType.ERROR, content, buttons);
                    alert.setTitle(title);
                    alert.setHeaderText(null);
                    
                    Optional<ButtonType> response = alert.showAndWait();
                    result[0] = response.isPresent() && response.get() == primaryButton;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur lors de l'affichage de l'alerte: " + e.getMessage(), e);
                    result[0] = true;
                } finally {
                    latch.countDown();
                }
            });
            
            // Attendre que l'alerte soit fermée pour garantir la synchronisation
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (IllegalStateException e) {
            // JavaFX toolkit n'est pas initialisé, retourner true par défaut
            logger.warning("JavaFX Toolkit n'est pas initialisé: " + e.getMessage());
            result[0] = true;
        }
        
        return result[0];
    }

    /**
     * Méthode privée pour afficher une alerte générique
     */
    private static void showAlert(AlertType alertType, String title, String content, ButtonType[] buttons) {
        // Determine log level and log the basic alert information
        Level logLevel = Level.INFO;
        if (alertType == AlertType.ERROR) {
            logLevel = Level.SEVERE;
        } else if (alertType == AlertType.WARNING) {
            logLevel = Level.WARNING;
        }
        logger.log(logLevel, title + ": " + content);

        // Vérifier si les alertes sont désactivées
        if (alertsDisabled) {
            logger.info("Alerte désactivée (" + logLevel.getName() + "): " + title + " - " + content);
            return;
        }

        // Vérifier si JavaFX est disponible
        if (!isJavaFxAvailable()) {
            logger.info("JavaFX non disponible, alerte non affichée: " + title);
            return;
        }
        
        try {
            // Essayer d'afficher l'alerte, mais capturer les exceptions liées à JavaFX
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(alertType, content, buttons == null ? new ButtonType[]{ButtonType.OK} : buttons);
                    alert.setTitle(title);
                    alert.setHeaderText(null);
                    alert.showAndWait();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur lors de l'affichage de l'alerte: " + e.getMessage(), e);
                }
            });
        } catch (IllegalStateException e) {
            // JavaFX toolkit n'est pas initialisé, juste logger l'erreur
            logger.warning("JavaFX Toolkit n'est pas initialisé: " + e.getMessage() + " - L'alerte ne sera pas affichée.");
            setAlertsDisabled(true); // Désactiver les alertes pour éviter d'autres erreurs
        }
    }
    
    /**
     * Vérifie si JavaFX est disponible et initialisé.
     * 
     * @return true si JavaFX est disponible, false sinon
     */
    private static boolean isJavaFxAvailable() {
        try {
            // Vérifier si Platform est accessible
            if (Platform.isFxApplicationThread()) {
                return true;
            }
            
            // Cette ligne va lancer une exception si JavaFX n'est pas initialisé
            Platform.runLater(() -> {});
            return true;
        } catch (IllegalStateException e) {
            // Le toolkit n'est pas initialisé
            return false;
        } catch (Exception e) {
            // Une autre erreur
            logger.warning("Erreur lors de la vérification de disponibilité de JavaFX: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gestionnaire d'erreurs critiques qui affiche une alerte et peut terminer l'application
     * 
     * @param record L'enregistrement de log
     * @param message Le message formaté
     */
    private static void handleCriticalError(LogRecord record, String message) {
        criticalErrorCount++;
        
        // Si on dépasse le nombre maximum d'erreurs critiques, on force la fermeture
        if (criticalErrorCount >= maxCriticalErrors) {
            logger.severe("ERREUR FATALE: Nombre maximum d'erreurs critiques atteint (" + 
                             maxCriticalErrors + "). L'application va s'arrêter.");
            System.exit(1);
            return;
        }
        
        // Si on est configuré pour terminer après une erreur critique
        if (exitOnCriticalError) {
            // Vérifier si JavaFX est disponible
            if (!isJavaFxAvailable()) {
                logger.severe("ERREUR CRITIQUE: " + record.getMessage());
                System.exit(1);
                return;
            }
            
            // Afficher une alerte modale pour informer l'utilisateur
            CountDownLatch latch = new CountDownLatch(1);
            
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR, 
                    "Une erreur critique s'est produite et l'application va s'arrêter.\n\n" +
                    "Détails: " + record.getMessage(), 
                    ButtonType.OK);
                alert.setTitle("Erreur critique");
                alert.setHeaderText(null);
                alert.showAndWait();
                latch.countDown();
            });
            
            // Attendre que l'alerte soit fermée
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Arrêter l'application
            System.exit(1);
        }
        // Sinon, l'erreur est simplement journalisée et l'application continue
    }

    /**
     * Méthode privée pour afficher une alerte avec détails d'exception
     */
    private static void showAlertWithException(AlertType alertType, String title, String content, Throwable exception) {
        // Determine log level
        Level logLevel = Level.INFO; // Default, though typically ERROR or WARNING for exceptions
        if (alertType == AlertType.ERROR) {
            logLevel = Level.SEVERE;
        } else if (alertType == AlertType.WARNING) {
            logLevel = Level.WARNING;
        }

        // Journaliser l'erreur avec les détails de l'exception
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();
        // Log with the determined level, and pass the exception object
        logger.log(logLevel, title + ": " + content + "\n" + exceptionText, exception);

        // Vérifier si les alertes sont désactivées
        if (alertsDisabled) {
            // logger.severe("Alerte désactivée: " + title + ": " + content); // Original was SEVERE
            logger.info("Alerte (avec exception) désactivée (" + logLevel.getName() + "): " + title + " - " + content); // Adjusted
            return;
        }
        
        // Vérifier si JavaFX est disponible
        if (!isJavaFxAvailable()) {
            // Si JavaFX n'est pas disponible, on se contente de journaliser le message
            // (already logged above with exception details)
            return;
        }
        
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            // Créer une zone de texte pour les détails de l'exception
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);

            // Ajouter le GridPane dans un espace de détails extensible
            alert.getDialogPane().setExpandableContent(expContent);
            
            // Afficher l'alerte
            alert.showAndWait();
        });
    }
    
    /**
     * Méthode privée pour afficher une alerte avec détails d'exception et boutons personnalisés
     */
    private static void showAlertWithExceptionAndAction(AlertType alertType, String title, String content, 
                                                     Throwable exception, String primaryButtonText, 
                                                     String secondaryButtonText) {
        // Determine log level
        Level logLevel = Level.INFO; // Default
        if (alertType == AlertType.ERROR) {
            logLevel = Level.SEVERE;
        } else if (alertType == AlertType.WARNING) {
            logLevel = Level.WARNING;
        }
        
        // Journaliser l'erreur avec les détails de l'exception
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();
        // Log with the determined level, and pass the exception object
        logger.log(logLevel, title + ": " + content + "\n" + exceptionText, exception);
        
        // Vérifier si les alertes sont désactivées
        if (alertsDisabled) {
            // logger.severe("Alerte désactivée: " + title + ": " + content); // Original was SEVERE
            logger.info("Alerte (avec exception et action) désactivée (" + logLevel.getName() + "): " + title + " - " + content); // Adjusted
            return;
        }
                
        // Vérifier si JavaFX est disponible
        if (!isJavaFxAvailable()) {
            // Si JavaFX n'est pas disponible, on se contente de journaliser le message
            // (already logged above with exception details)
            return;
        }
        
        Platform.runLater(() -> {
            // Créer les boutons personnalisés
            ButtonType primaryButton = (primaryButtonText != null) ? 
                                      new ButtonType(primaryButtonText) : ButtonType.OK;
            
            ButtonType[] buttons;
            if (secondaryButtonText != null) {
                ButtonType secondaryButton = new ButtonType(secondaryButtonText);
                buttons = new ButtonType[] { primaryButton, secondaryButton };
            } else {
                buttons = new ButtonType[] { primaryButton };
            }
            
            Alert alert = new Alert(alertType, content, buttons);
            alert.setTitle(title);
            alert.setHeaderText(null);

            // Créer une zone de texte pour les détails de l'exception
            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);

            // Ajouter le GridPane dans un espace de détails extensible
            alert.getDialogPane().setExpandableContent(expContent);
            
            // Afficher l'alerte
            alert.showAndWait();
        });
    }
}