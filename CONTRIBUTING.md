# Guide de contribution

Merci de l'intérêt que vous portez à ce projet. Ce document décrit les outils requis, la démarche à suivre pour contribuer, les issues traitées avec leur démarche de résolution, ainsi que les standards de code à respecter.

---

## Outils requis

Avant de contribuer, assurez-vous d'avoir les outils suivants installés :

- Un IDE Java (recommandé : IntelliJ, Eclipse, VSCodium)
- Java JDK 17 ou supérieur
- Git et un client Git (optionnel)
- Maven
- Un compte GitHub
- Libgdx (optionnel : utile pour le développement mobile)
- Android SDK (optionnel : pour les versions Android)

---

## Démarche de contribution

### Points importants
- Respectez le style de code existant dans le module `forge-ai`.
- Une issue = une pull request.
- Commentez votre logique pour expliquer *pourquoi* l'IA prend une décision, pas seulement *ce qu'elle fait*.

### Étapes 
- Créer une branche dans votre fork 
- Apporter vos modifications
- Tester vos modifications 
- Soumettre une pull request en rédigeant un titre et une description clairs.
- Référencez le numéro de l'issue.
- Décrivez le comportement avant et après votre correction.

---

# Hiérarchie des projets

Forge est divisé en 4 projets principaux avec des projets supplémentaires ciblant des plateformes spécifiques.

## Projets principaux

- `forge-ai`
- `forge-core`
- `forge-game`
- `forge-gui`

## Projets spécifiques aux plateformes

- `forge-gui-android`
- `forge-gui-desktop`
- `forge-gui-ios`
- `forge-gui-mobile`
- `forge-gui-mobile-dev`

---

## forge-ai

Le projet `forge-ai` contient la logique de l’adversaire contrôlé par l’ordinateur pour le gameplay.  
Il inclut les algorithmes de prise de décision pour des capacités spécifiques, des cartes et les différentes phases du tour.

---

## forge-core

Le projet `forge-core` contient le moteur principal du jeu, les mécaniques des cartes, le moteur de règles ainsi que la logique fondamentale du jeu.  
Il comprend l’implémentation des règles de *Magic: The Gathering*, les interactions entre les cartes et le système de gestion de l’état du jeu.

---

## forge-game

Le projet `forge-game` gère la gestion des sessions de jeu, les interactions entre les joueurs et le contrôle du déroulement de la partie.  
Il inclut les implémentations pour le multijoueur, les modes de jeu, le matchmaking ainsi que la persistance de l’état du jeu.

Ce module fait le lien entre le moteur principal du jeu, l’interface utilisateur et les composants réseau.

---

## forge-gui

Le projet `forge-gui` contient les composants de l’interface utilisateur ainsi que la logique de rendu du jeu.  
Il inclut la fenêtre principale du jeu, l’affichage des cartes, les interactions des joueurs et les définitions des ressources de scripts situées dans le répertoire `res/`.

---

## forge-gui-android

Backend basé sur **LibGDX** ciblant Android.  
Nécessite le SDK Android et s’appuie sur `forge-gui-mobile` pour la logique de l’interface graphique.

---

## forge-gui-desktop

Interface graphique basée sur **Java Swing** ciblant les ordinateurs de bureau.

La disposition des écrans et la logique liée à l’interface graphique se trouvent ici.  
Par exemple, les flèches superposées (lorsqu’elles sont activées) indiquant les attaquants et les bloqueurs, ou les cibles de la pile, sont définies et dessinées dans ce module.

---

## forge-gui-ios

Backend basé sur **LibGDX** ciblant iOS.  
S’appuie sur `forge-gui-mobile` pour la logique de l’interface graphique.

---

## forge-gui-mobile

Logique de l’interface graphique mobile utilisant la bibliothèque **LibGDX**.  
La disposition des écrans et la logique liée à l’interface graphique pour les plateformes mobiles se trouvent dans ce module.

---

## forge-gui-mobile-dev

Backend **LibGDX** pour le développement desktop des backends mobiles.  
Utilise **LWJGL**.  
S’appuie sur `forge-gui-mobile` pour la logique de l’interface graphique.

---
