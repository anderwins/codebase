package ulb.dao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.util.Objects;

/**
 * La classe SQLLoader permet de charger et parser un fichier SQL externe contenant plusieurs requêtes SQL
 * identifiées par des tags personnalisés. Chaque requête est stockée dans une map accessible par son tag.
 
 */
public class SQLLoader {

    private final Map<String, String> queries = new HashMap<>(); // Map contenant les requêtes SQL indexées par leur tag (il s'agit de toutes les requttes presente dans le fichier sql passer dans le constructeur) (avec comme structure clé:valeur (tag:requette) tag est la clé et requette la valeur).

    /**
     * Constructeur de la classe SQLLoader.
     * Charge et parse le fichier SQL spécifié dans le paramètre resourcePath.
     *
     * @param resourcePath Le chemin du fichier SQL à charger 
     */
    public SQLLoader(String resourcePath) {
        loadQueries(resourcePath);
    }

    /**
     * Charge et parse le fichier SQL pour remplir la map de requêtes.
     *
     * @param resourcePath Le chemin du fichier SQL dans le dossier src/main/resources
     */
    private void loadQueries(String resourcePath) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(SQLLoader.class.getClassLoader().getResourceAsStream(resourcePath))))) {

            if (resourcePath == null) {
                throw new IOException("❌ Fichier SQL non trouvé : " + resourcePath);
            }
            StringBuilder queryBuilder = new StringBuilder();
            String currentTag = null;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Si on détecte un nouveau tag
                if (line.startsWith("-- [") && line.endsWith("]")) {
                    // Enregistre la requête précédente si elle existe
                    if (currentTag != null) {
                        queries.put(currentTag, queryBuilder.toString().trim());
                        queryBuilder.setLength(0); // Réinitialise le StringBuilder
                    }
                    // Récupère le nom du tag actuel
                    currentTag = line.substring(4, line.length() - 1).trim();
                }
                // Sinon, ajoute la ligne courante à la requête en cours
                else if (currentTag != null && !line.startsWith("--")) {
                    queryBuilder.append(line).append("\n");
                }
            }
            // Ajoute la dernière requête à la map si elle existe
            if (currentTag != null) {
                queries.put(currentTag, queryBuilder.toString().trim());
            }
        } catch (Exception e) {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SQLLoader.class.getName());
            logger.severe("❌ Erreur lors du chargement du fichier SQL '" + resourcePath + "' : " + e.getMessage());
        }
    }

    /**
     * Récupère une requête SQL à partir de son tag.
     *
     * @param tag Le nom du tag (exemple : "FIND_TRACK_BY_TITLE")
     * @return La requête SQL correspondante ou null si le tag n'existe pas.
     */
    public String getQuery(String tag) {
        return queries.get(tag);
    }
}
