# Lecteur de Musique JavaFX

Une application de lecture de musique développée avec JavaFX.

## vidéo de présentation

https://www.youtube.com/watch?v=T244C12epLM

## Prérequis

- Kit de Développement Java (JDK) version 18 ou ultérieure
- Apache Maven version 3.6 ou ultérieure

## Installation

le build de l'application est téléchargable [ici https://drive.google.com/file/d/1ZyMm0B8akfcAqjhUrCIrEiH5Q3R2l9WF/view?usp=drive_link](https://drive.google.com/file/d/1ZyMm0B8akfcAqjhUrCIrEiH5Q3R2l9WF/view?usp=drive_link)

Ce .jar est un fat jar qui inclus toutes les dépendences nécessaire, il est donc trop lourd pour être mis sous forme de 
release gitlab.

L'application créé un dossier .deezify_g8 dans le home de l'utilisateur qui contient un dossier /musiques

Pour tester l'application avec vos propres musiques, vous pouvez les placer dans ce dossier, et supprimer le fichier deezify.db puis relancer l'application.

### from source

Clonez le dépôt :

```bash
git clone https://gitlab.ulb.be/ulb-infof307/2025-groupe-8.git
cd 2025-groupe-8
```

Compilez le projet avec Maven :

```bash
mvn compile
```
   
lancement:

```bash
mvn exec:java
```
   
alternativement il est possible de créer un jar:

```bash
mvn clean install
```

et de le lancer en se positionnant à la racine du projet:

```bash
java -jar target/infof307-1.0-SNAPSHOT.jar
```


## Fonctionnalités

- Gestion complète des listes de lecture
- Fonction de recherche avancée pour vos morceaux
- Affichage détaillé des informations des morceaux (titre, artiste, album, durée)

## Résolution des Problèmes Courants

### Problèmes Fréquents

1. **Erreur de version Java**
   - Assurez-vous d'avoir installé JDK 17 ou une version plus récente
   - Vérifiez que votre variable d'environnement JAVA_HOME est correctement configurée

2. **Échec de la compilation Maven**
   - Vérifiez que Maven est correctement installé sur votre système
   - Essayez d'exécuter `mvn clean` avant de recompiler le projet

3. **Composants JavaFX manquants**
   - Les composants JavaFX nécessaires sont inclus dans les dépendances du projet
   - Si le problème persiste, vérifiez que vos paramètres Maven sont correctement configurés
