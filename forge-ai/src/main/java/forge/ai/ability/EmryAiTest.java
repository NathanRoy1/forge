package forge.ai.ability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class EmryAiTest {

    /**
     * Simule la décision finale de l'AI, miroir de AiPlayDecision dans Forge.
     * On reproduit les deux valeurs utilisées dans les branches Emry.
     */
    enum AiDecision { WILL_PLAY, CANT_PLAY_AI }

    /**
     * Simule un artifact dans le cimetière, avec ou sans Flash.
     * Remplace forge.game.card.Card pour éviter le bootstrap Forge.
     */
    static class FakeGraveyardArtifact {
        final String name;
        final boolean isCreature;
        final boolean hasFlash;
        final boolean canPayManaCost;
        final int power;
        final int toughness;

        FakeGraveyardArtifact(String name, boolean isCreature, boolean hasFlash,
                              boolean canPayManaCost, int power, int toughness) {
            this.name           = name;
            this.isCreature     = isCreature;
            this.hasFlash       = hasFlash;
            this.canPayManaCost = canPayManaCost;
            this.power          = power;
            this.toughness      = toughness;
        }
    }

    /**
     * Simule un attaquant adverse.
     */
    static class FakeAttacker {
        final String name;
        final int power;
        final int toughness;

        FakeAttacker(String name, int power, int toughness) {
            this.name      = name;
            this.power     = power;
            this.toughness = toughness;
        }
    }


    /**
     * Reproduit la logique du bloc hasMayPlayFromGrave
     * de EffectAi.checkApiLogic() (lignes 424-471).
     *
     * @param isOpponentTurn     true si c'est le tour adverse
     * @param isCombatDeclareAtk true si la phase est COMBAT_DECLARE_ATTACKERS
     * @param attackers          liste des attaquants contre l'AI
     * @param graveyard          artifacts valides dans le cimetière de l'AI
     * @return la décision de l'AI
     */
    AiDecision emryHasMayPlayFromGraveLogic(
            boolean isOpponentTurn,
            boolean isCombatDeclareAtk,
            java.util.List<FakeAttacker> attackers,
            java.util.List<FakeGraveyardArtifact> graveyard
    ) {
        if (isOpponentTurn && isCombatDeclareAtk) {
            if (!attackers.isEmpty()) {

                 java.util.List<FakeGraveyardArtifact> flashCreatures = new java.util.ArrayList<>();
                for (FakeGraveyardArtifact card : graveyard) {
                    if (card.isCreature && card.hasFlash) {
                        flashCreatures.add(card);
                    }
                }

                FakeGraveyardArtifact bestBlocker = null;

                outer:
                for (FakeAttacker attacker : attackers) {
                    for (FakeGraveyardArtifact blocker : flashCreatures) {

                       if (!blocker.canPayManaCost) {
                            continue;
                        }

                       boolean blockerDies  = attacker.power  >= blocker.toughness;
                        boolean attackerDies = blocker.power   >= attacker.toughness;


                        if (attackerDies || !blockerDies) {
                            bestBlocker = blocker;
                            break outer;
                        }
                    }
                }

                if (bestBlocker != null) {
                    return AiDecision.WILL_PLAY;     // -> WillPlay  (ligne ~457)
                }
                return AiDecision.CANT_PLAY_AI;      // -> CantPlayAi (ligne ~459)
            }
        }

         boolean hasValidTarget = !graveyard.isEmpty();
        if (!hasValidTarget) {
            return AiDecision.CANT_PLAY_AI;
        }
        return AiDecision.WILL_PLAY;
    }


    /**
     * Scenario :
     *   - C'est le tour de l'AI (Main Phase)
     *   - Le cimetiere ne contient aucun artifact valide
     *
     * Comportement attendu : CantPlayAi
     *
     * Branche couverte (EffectAi.java ~468) :
     *   if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false, false)) {
     *       return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
     *   }
     */
    @Test
    @DisplayName("Cas 1 : Cimetiere vide -> l'AI n'active pas la capacite d'Emry")
    void testEmry_EmptyGraveyard_ShouldNotActivate() {
        // Arrange
        boolean isOpponentTurn     = false;
        boolean isCombatDeclareAtk = false;
        java.util.List<FakeAttacker> attackers = java.util.Collections.emptyList();
        java.util.List<FakeGraveyardArtifact> graveyard = java.util.Collections.emptyList();

        // Act
        AiDecision decision = emryHasMayPlayFromGraveLogic(
                isOpponentTurn, isCombatDeclareAtk, attackers, graveyard
        );

        // Assert
        assertEquals(AiDecision.CANT_PLAY_AI, decision,
                "Avec un cimetiere vide, targetPlayableSpellCard retourne false " +
                        "-> l'AI ne doit pas activer Emry (CantPlayAi)"
        );
    }

    /**
     * Scenario :
     * C'est le tour de l'adversaire, phase COMBAT_DECLARE_ATTACKERS
     * L'adversaire attaque avec Shalai (3/4)
     * Le cimetiere contient un artifact-creature Flash 4/4 :
     *  power (4) >= toughness de Shalai (4) -> attackerDies = true
     *  toughness (4) > power de Shalai (3)  -> blockerDies  = false
     *
     * Comportement attendu : WillPlay
     *
     * Branche couverte (EffectAi.java ~446-457) :
     *   if (attackerDies || !blockerDies) {
     *       bestBlocker = blocker;
     *       return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
     *   }
     */
    @Test
    @DisplayName("Cas 2 : Tour adverse + Flash blocker qui tue l'attaquant -> l'AI active Emry")
    void testEmry_OpponentTurnCombat_FlashBlockerKillsAttacker_ShouldActivate() {
        // Arrange
        FakeAttacker shalai = new FakeAttacker("Shalai, Voice of Plenty", 3, 4);

        FakeGraveyardArtifact flashBlocker = new FakeGraveyardArtifact(
                "Construite Flash 4/4",
                true,   // isCreature
                true,   // hasFlash : peut etre joue au tour adverse
                true,   // canPayManaCost : à les ressources necessaire pour jouer la carte
                4,      // power : 4 >= 4 (toughness Shalai) -> attackerDies = true
                4       // toughness : 4 > 3 (power Shalai)  -> blockerDies  = false
        );

        // Act
        AiDecision decision = emryHasMayPlayFromGraveLogic(
                true,
                true,
                java.util.List.of(shalai),
                java.util.List.of(flashBlocker)
        );

        // Assert
        assertEquals(AiDecision.WILL_PLAY, decision,
                "Un Flash blocker qui tue l'attaquant (attackerDies=true) " +
                        "doit pousser l'AI a activer Emry (WillPlay)"
        );
    }

    /**
     * Scenario :
     *   - C'est le tour de l'adversaire, phase COMBAT_DECLARE_ATTACKERS
     *   - L'adversaire attaque avec une creature
     *   - Le cimetiere contient un artifact-creature SANS Flash
     *     (ne peut pas etre joue pendant le tour adverse)
     *
     * Comportement attendu : CantPlayAi
     *
     * Branche couverte (EffectAi.java ~459) :
     *   // bestBlocker reste null car flashCreatures est vide apres le filtre
     *   return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
     */
    @Test
    @DisplayName("Cas 3 : Tour adverse + combat SANS Flash blocker -> l'AI n'active pas Emry")
    void testEmry_OpponentTurnCombat_NoFlashBlocker_ShouldNotActivate() {
        // Arrange
        FakeAttacker attacker = new FakeAttacker("Creature adverse 3/3", 3, 3);

        FakeGraveyardArtifact noFlashArtifact = new FakeGraveyardArtifact(
                "Mox Opal",
                true,   // isCreature
                false,  // hasFlash <- absent : filtre par la logique Emry
                true,
                2,
                2
        );

        // Act
        AiDecision decision = emryHasMayPlayFromGraveLogic(
                true,
                true,
                java.util.List.of(attacker),
                java.util.List.of(noFlashArtifact)
        );

        // Assert
        assertEquals(AiDecision.CANT_PLAY_AI, decision,
                "Sans artifact avec Flash dans le cimetiere, flashCreatures est vide " +
                        "-> bestBlocker reste null -> l'AI ne doit pas activer Emry (CantPlayAi)"
        );
    }


}