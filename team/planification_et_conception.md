**Suivis de la planification et motivation des choix**

**Objectif de l'itération:**

Les fonctionnalités prévues pour l’itération suivante ont été complétées, à savoir :

Histoire 12 : Zôlis dessins (Visualiseur audio)
L’utilisateur peut activer un visualiseur graphique lors de la lecture d’un morceau. Ce visualiseur génère une animation synchronisée avec le rythme de la musique.

Histoire 15 : Transition en fondu
L’utilisateur peut activer un fondu automatique entre deux morceaux, et personnaliser la durée de cette transition.

Histoire 18 : Multilinguisme
L’utilisateur peut sélectionner la langue de l’interface parmi le français, le néerlandais et l’anglais.

**Conception:**

Visualiseur audio (Zôlis dessins)
Le visualiseur est un composant graphique déclenché depuis l’interface de lecture. Il utilise les données du MediaPlayer pour détecter le rythme et générer dynamiquement des formes animées. Une première version simple a été construite pour valider l’approche (formes de base, synchronisation approximative), puis enrichie avec différents styles et transitions fluides. L’option d’activation du visualiseur a été intégrée à l’interface principale du lecteur.

Transition audio en fondu
Le fondu entre deux morceaux a été intégré dans le AudioPlayerController, en ajustant progressivement le volume du morceau courant tout en démarrant le suivant. Un bouton d’activation et un slider de durée ont été ajoutés à l’interface. Le code respecte le principe de séparation des responsabilités : la logique de transition reste encapsulée dans le contrôleur audio tandis que la vue gère uniquement les interactions utilisateur.

Multilinguisme
Pour supporter plusieurs langues, tous les textes visibles par l’utilisateur ont été externalisés dans des fichiers .properties (messages_fr.properties, messages_nl.properties, messages_en.properties). Une classe LanguageManager a été ajoutée pour centraliser la gestion de la langue active. Les vues FXML utilisent des balises fx:include et des bindings avec un ResourceBundle chargé au démarrage. Un menu dans les paramètres permet à l’utilisateur de choisir dynamiquement la langue, ce qui met à jour les vues chargées.
