**Itération 4 – Visualiseur graphique, Transitions et Multilinguisme**

---

**Histoire 12 : Zôlis dessins**

Description : Pendant la lecture d’un morceau, l’utilisateur peut choisir d’afficher un visualiseur graphique du son. Une animation générée aléatoirement et synchronisée avec le rythme de la musique est affichée.

Tâches :

* Ajout du bouton “Visualiser” dans la page de lecture  
* Première version du visualiseur (formes simples + synchronisation rythmique)  
* Version finale du visualiseur (animations plus fluides, choix de styles)  
* Tests unitaires et fonctionnels du visualiseur  
* Ajout de paramètres de configuration pour le visualiseur  
* Intégration continue pour le module visualiseur  

Responsables : [Oumaima, Esther Rocío, Hac, Mamadou]

Status : Terminé

---

**Histoire 15 : Transition**

Description : L’utilisateur peut activer une option permettant une transition en fondu entre deux morceaux. Il peut également régler la durée de la transition.

Tâches :

* Ajout de la case à cocher “Activer transition” dans l’interface  
* Ajout d’un curseur pour le choix de la durée de la transition  
* Implémentation de la logique de fondu entre morceaux dans le contrôleur  
* Ajout d’un feedback visuel lors de la transition  
* Tests de transition avec différents types de morceaux  
* Documentation technique  

Responsables : [Noah, Marc, Amara, Anderwins]

Status : Terminé

---

**Histoire 18 : Multilinguisme**

Description : L’utilisateur peut changer la langue de l’application (français, néerlandais, anglais) depuis un menu d’options. Toutes les pages doivent être traduites.

Tâches :

* Ajout d’un bouton de sélection de langue dans les paramètres  
* Externalisation de toutes les chaînes de caractères en fichiers `.properties`  
* Traduction de tous les écrans de l’application  
* Ajout de fichiers de langue : `messages_fr.properties`, `messages_nl.properties`, `messages_en.properties`  
* Tests d’affichage dynamique selon la langue choisie  
* Vérification de la compatibilité avec les autres modules (Karaoke, Visualiseur, etc.)  

Responsables : [Mamadou, Amara, Vincent, Abdoulaye]

Status : Terminé

---

**Refactor & Support Technique complémentaire**

Description : Tâches techniques transversales réalisées par des membres moins impliqués dans les histoires principales, mais essentielles pour la stabilité globale du projet.

Tâches :

* Refactor du logger (uniformisation des appels) – Hac  
* Nettoyage et centralisation des fichiers CSS – Esther Rocío  
* Optimisation du chargement des ressources graphiques – Marc  
* Organisation des packages du module visualiseur – Vincent  
* Ajout de commentaires et documentation dans le code de transition – Noah  
* Mise à jour de la configuration Maven – Anderwins  
* Vérification de l’adaptabilité mobile des interfaces – Oumaima  
* Revue de code croisée (peer review) sur l’ensemble des commits – Abdoulaye


Status : Terminé

---

**Remarques générales :**

L’itération 4 a permis de renforcer l’aspect visuel de l’application (visualiseur), d’améliorer l’expérience sonore (transition fluide entre morceaux) et de la rendre accessible à un public multilingue. Tous les membres du groupe ont été mobilisés, soit directement dans les développements majeurs, soit via des tâches techniques complémentaires (refactor, documentation, optimisation), assurant une forte collaboration collective.
