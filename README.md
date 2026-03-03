# ⚔️ Contributions à Forge MTG — Améliorations de l'IA

Rejoignez la **communauté Forge** sur [Discord](https://discord.gg/HcPJNyD66a) !

[![Test build](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml/badge.svg)](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml)

> **Dépôt officiel :** [Card-Forge/forge](https://github.com/Card-Forge/forge)  
> Ce dépôt contient nos contributions au moteur de jeu Forge, axées sur l'amélioration de l'intelligence artificielle.

---

## Introduction

**Forge** est un moteur de règles libre et open source pour **Magic: The Gathering**, développé par une communauté de
programmeurs passionnés. Il permet aux joueurs d'explorer l'univers de MTG à travers une plateforme flexible et
extensible.

**Note :** Forge est un projet indépendant, non affilié à Wizards of the Coast.

Notre contribution se concentre sur le module `forge-ai`, responsable de toutes les décisions prises par le joueur
contrôlé par l'ordinateur. Les améliorations visent à corriger des comportements erronés de l'IA lors de l'évaluation
d'aptitudes activées, notamment pour des cartes comme **Psychic Frog** et **Emry, Lurker of the Loch**.

---

## Fonctionnalités principales de Forge

- **Support multi-plateforme :** Jouez sur Windows, Mac, Linux et Android.
- **Architecture extensible :** Développé en Java, Forge encourage les contributions de la communauté.
- **Modes de jeu variés :** Mode solo, mode Aventure, formats Scellé, Draft, Commander, Cube.

---

## Prérequis

Avant d'installer le projet, assurez-vous d'avoir les outils suivants sur votre machine :

| Outil         | Version minimale      | Lien                      |
|---------------|-----------------------|---------------------------|
| Java JDK      | 17                    | https://adoptium.net      |
| Git           | Toute version récente | https://git-scm.com       |
| IDE Java      | Demandé : IntelliJ    | https://www.jetbrains.com |
| Compte GitHub | —                     | https://github.com        |

Pour vérifier vos installations, éxectué ces commandes :

```bash
java -version
git --version
```

---

## Installation

#### 1. Forker et cloner le dépôt Forge

Connectez-vous à GitHub, forker et cloner le
projet [Forge](https://github.com/NathanRoy1/forge), puis clonez votre
fork dans un dossier vide sur votre ordinateur.

#### 2. Ouvrir le dossier dans votre IDE préféré

> IntelliJ est l'IDE recommandé pour le développement sur Forge.

## Configuration

Pour configurer le projet sur desktop consultez
le [guide de configuration IntelliJ](https://github.com/Card-Forge/forge/wiki/IntelliJ-setup) du dépot officiel de
Forge.
Ce tutoriel présente l’ensemble des étapes, de l’installation jusqu’au lancement de l’application.

Si certaines étapes ont déjà été complétées (par exemple, l’installation d’IntelliJ ou du JDK Java), il n’est pas nécessaire de les refaire.

> La compilation complète peut prendre plusieurs minutes lors du premier lancement (téléchargement des dépendances
> Maven).

---

## Issues réalisées

### Issue 1 — L'IA n'active pas la première aptitude de Psychic Frog

**Description**

L'IA contrôlant un **Psychic Frog** sur le champ de bataille n'activait jamais sa première aptitude (Défausser une
carte : Poser un marqueur +1/+1 sur Psychic Frog), même dans des situations où cette activation lui aurait permis de
survivre en combat ou de tuer une créature adverse.

**Étapes pour reproduire**

1. Le joueur humain attaque avec Shalai, Voice of Plenty.
2. L'IA contrôle un Psychic Frog sur le champ de bataille.
3. L'IA a 4 cartes ou plus en main.
4. L'IA ne déclare pas Psychic Frog comme bloqueur et n'active pas son aptitude.

**Comportement attendu**

L'IA devrait activer la première aptitude de Psychic Frog dans les situations suivantes :

- Le marqueur +1/+1 lui permet de survivre à un combat qu'elle perdrait autrement.
- Le marqueur +1/+1 lui permet de tuer une créature adverse qu'elle ne tuerait pas autrement.
- L'IA a 8 cartes ou plus en main (coût de défausse jugé négligeable).
- La créature est menacée par un sort ou une aptitude adverse.

**Démarche de résolution**

En inspectant le module `forge-ai`, j'ai constaté qu'aucune logique spécifique à Psychic Frog n'existait dans
`SpecialCardAi.java`. La classe `ability_CountersAi.java` évaluait les aptitudes qui posent des marqueurs(+1/+1), mais
ne prenait pas compte du contexte de combat ni du coût de la défausse.

J'ai donc ajouté une classe `PsychicFrog` dans `SpecialCardAi.java` avec deux méthodes :

- `considerCounterAbility` : évalue si l'activation du marqueur est pertinente avant ou pendant le combat (en tant que
  bloqueur.
- `considerFlyingAbility` : évalue si l'activation de l'aptitude de vol est pertinente pour bloquer un voleur adverse ou
  attaquer sans blocage possible.

**Fichiers modifiés**

- `forge-ai/src/main/java/forge/ai/SpecialCardAi.java`
- `forge-ai/src/main/java/forge/ai/ability/ability_CountersAi.java`
- `forge-card/src/main/res/cardsfolder/p/psychic_frog.txt`
- `forge-ai/src/main/java/forge/ai/ability/PumpAi.java`

*Liens vers l'issue*
- L'issue directement sur le dépot Forge : [Issue Psychic Frog](https://github.com/Card-Forge/forge/issues/6379)
- L'issue de mon dépot : [Issue Psychic Frog](https://github.com/NathanRoy1/forge/tree/AI-doesn't-activate-first-ability-of-Psychic-Frog)
- La pull Request : [Pull Request Psychic Frog](https://github.com/Card-Forge/forge/pull/9978)


---

### Issue 2 — L'IA n'utilise pas l'aptitude de tap d'Emry, Lurker of the Loch

**Description**

L'IA contrôlant **Emry, Lurker of the Loch** sur le champ de bataille n'utilisait pas son aptitude de tap (`{T}` :
Choisissez une carte artefact dans votre cimetière. Vous pouvez la lancer ce tour-ci.), même lorsqu'un artefact jouable
était disponible dans son cimetière et que le mana était suffisant.

**Étapes pour reproduire**

1. L'IA contrôle Emry, Lurker of the Loch (non engagée) sur le champ de bataille.
2. Le cimetière de l'IA contient au moins un artefact jouable (mana suffisant pour le lancer).
3. C'est le tour principal de l'IA.
4. L'IA ne tape pas Emry et ne relance pas l'artefact.

**Comportement attendu**

L'IA devrait utiliser l'aptitude de tap d'Emry pour relancer un artefact depuis son cimetière si :

- Un artefact jouable est présent dans le cimetière.
- L'IA a assez de mana pour lancer cet artefact ce tour-ci.
- Taper Emry ne compromet pas un blocage critique.

**Démarche de résolution**

En inspectant `ability_EffectAi.java` (qui gère les Effect abilities),
j'ai constaté que l'IA ne reconnaissait pas automatiquement les abilities qui lui accordent
la permission de lancer des cartes depuis le cimetière : elle retournait toujours `CantPlayAi`.

**Solution implémentée** : J'ai ajouté une auto-détection dans le bloc `RememberObjects` de `EffectAi.java` qui :

**Détecte automatiquement** les abilities "cast from graveyard" en analysant les `StaticAbilities` :

- Vérifie la présence de `MayPlay$ True`
- Vérifie `AffectedZone$ Graveyard`
- Fonctionne pour Emry, Snapcaster Mage, et toutes les cartes similaires

**Ajouter une logique défensive** pour le combat :

- Détecte la phase declare attackers de l'adversaire
- Évalue si des créatures flash dans le cimetière peuvent bloquer profitablement
- Utilise `ComputerUtilCombat.canDestroyAttacker/canDestroyBlocker` pour le calcul de combat
- Vérifie le mana disponible avec `canPayManaCost()` (pas juste les sources totales)

**Respecte les restrictions de timing** :

- Cartes sorcery-speed : uniquement en main phase de l'IA
- Cartes avec Flash : également pendant declare attackers ou après le combat adverse
- Évite d'activer dans end step ou autres phases inappropriées

**Fichiers modifiés** :

- `forge-card/src/main/res/cardsfolder/e/emry_lurker_of_the_loch.txt`
- `forge-ai/src/main/java/forge/ai/ability/EffectAi.java`

*Liens vers l'issue*
- L'issue directement sur le dépot Forge : [Issue Emry's Lurker of the Loch](https://github.com/Card-Forge/forge/issues/6387)
- L'issue de mon dépot : [Issue Emry's Lurker of the Loch](https://github.com/NathanRoy1/forge/tree/AI-doesn't-activate-first-ability-of-Psychic-Frog)
- La pull Request : [Pull Request Emry's Lurker of the Loch](https://github.com/Card-Forge/forge/pull/9783)


## Tester mes corrections

### Psychic Frog

Lancez Forge et démarrez une nouvelle partie avec un deck contenant Psychic Frog est assigner à l'IA.
Assurez-vous que l'IA a au moins 4 cartes en main.
Faites attaquer votre joueur humain avec une créature (ex. : Shalai, Voice of Plenty).
Résultat attendu : l'IA active la première aptitude de Psychic Frog (Défausser une carte → +1/+1) lorsque cela lui
permet de survivre au combat ou de tuer la créature adverse.

### Emry, Lurker of the Loch

Démarrez une nouvelle partie avec un deck contenant Emry et plusieurs artefacts.
Laissez Emry déclencher son entrée en jeu (mouliner 4 cartes).
Assurez-vous que l'IA a assez de mana pour relancer un artefact.
Résultat attendu : l'IA tape Emry pour relancer l'artefact au coût de mana le plus élevé disponible dans son cimetière.

## Ressources

- Dépôt officiel Forge MTG
- Wiki Forge
- Guide utilisateur
- Guide de configuration IntelliJ
- Documentation du scripting de cartes
- Discord communautaire

## License

[GPL-3.0](LICENSE)
<div align="center" style="display: flex; align-items: center; justify-content: center;">
    <div style="margin-left: auto;">
        <a href="#top">
            <img src="https://img.shields.io/badge/Back%20to%20Top-000000?style=for-the-badge&logo=github&logoColor=white" alt="Back to Top">
        </a>
    </div>
</div>



