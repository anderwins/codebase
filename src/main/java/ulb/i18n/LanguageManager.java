package ulb.i18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * Gestionnaire de langues pour l'application.
 * Utilise le pattern Singleton pour centraliser la gestion de la langue.
 */
public class LanguageManager {

    private final ObjectProperty<Locale> locale;
    private static final String BUNDLE_PATH = "i18n.messages";
    private static final Locale DEFAULT_LOCALE = Locale.FRENCH;

    // Instance unique (Singleton)
    private static LanguageManager instance;

    /**
     * Constructeur privé pour assurer le singleton.
     * Initialise la langue par défaut en français.
     */
    private LanguageManager() {
        this.locale = new SimpleObjectProperty<>(DEFAULT_LOCALE);
    }

    /**
     * Retourne l'instance unique de LanguageManager.
     * @return instance unique de LanguageManager
     */
    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    /**
     * Retourne la propriété de la locale pour l'écoute des changements.
     * @return propriété de la locale
     */
    public ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    /**
     * Obtient la locale actuelle.
     * @return locale actuelle
     */
    public Locale getLocale() {
        return locale.get();
    }

    /**
     * Définit une nouvelle locale.
     * @param locale nouvelle locale à définir
     */
    public void setLocale(Locale locale) {
        if (locale != null) {
            this.locale.set(locale);
        } else {
            System.err.println("⚠️ Langue invalide, utilisation de la langue par défaut.");
            this.locale.set(DEFAULT_LOCALE);
        }
    }

    /**
     * Charge le ResourceBundle correspondant à la locale actuelle.
     * @return ResourceBundle chargé
     */
    public ResourceBundle getResourceBundle() {
        try {
            return ResourceBundle.getBundle(BUNDLE_PATH, getLocale());
        } catch (MissingResourceException e) {
            System.err.println("⚠️ Fichier de ressources non trouvé pour la locale : " + getLocale());
            return ResourceBundle.getBundle(BUNDLE_PATH, DEFAULT_LOCALE);
        }
    }

    /**
     * Récupère la chaîne de caractères associée à la clé spécifiée.
     * Si la clé est introuvable, retourne la clé elle-même entre accolades.
     * @param key clé de la chaîne à récupérer
     * @return chaîne localisée ou la clé si non trouvée
     */
    public String getString(String key) {
        try {
            return getResourceBundle().getString(key);
        } catch (MissingResourceException e) {
            System.err.println("⚠️ Clé de traduction manquante : " + key);
            return "{" + key + "}";
        }
    }
}
