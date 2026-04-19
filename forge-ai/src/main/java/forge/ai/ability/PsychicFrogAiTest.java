package forge.ai.ability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PsychicFrogAiTest {

    /**
     * Simule la décision finale de l'AI, miroir de AiPlayDecision dans Forge.
     * On reproduit les trois valeurs utilisées dans les branches doCombatAdaptLogic.
     */
    enum AiDecision { WILL_PLAY, IMPACT_COMBAT, CANT_PLAY_AI }

    /**
     * Simule une créature en combat.
     * Remplace forge.game.card.Card pour éviter le bootstrap Forge.
     */
    static class FakeCombatCreature {
        final String name;
        final int power;
        final int toughness;

        FakeCombatCreature(String name, int power, int toughness) {
            this.name      = name;
            this.power     = power;
            this.toughness = toughness;
        }
    }

    /**
     * Reproduit la logique de CountersPutAi.doCombatAdaptLogic (lignes 1157-1191).
     *
     * @param source     la créature source (Psychic Frog ou similaire)
     * @param amount     le nombre de marqueurs +1/+1 ajoutés par l'aptitude
     * @param isAttacking  true si source attaque
     * @param isBlocked    true si source est bloquée (uniquement si isAttacking)
     * @param isBlocking   true si source bloque
     * @param opponents    bloqueurs (si attaquante) ou attaquants bloqués (si bloquante)
     * @return la décision de l'IA
     */
    AiDecision doCombatAdaptLogic(
            FakeCombatCreature source,
            int amount,
            boolean isAttacking,
            boolean isBlocked,
            boolean isBlocking,
            java.util.List<FakeCombatCreature> opponents
    ) {
        if (isAttacking) {
            if (!isBlocked) {
                return AiDecision.WILL_PLAY;
            } else {
                for (FakeCombatCreature blocker : opponents) {
                    if (blocker.toughness > source.power
                            && blocker.toughness <= source.power + amount) {
                        return AiDecision.WILL_PLAY;
                    }
                }
                int totBlkPower = opponents.stream().mapToInt(c -> c.power).sum();
                if (source.toughness <= totBlkPower
                        && source.toughness + amount > totBlkPower) {
                    return AiDecision.IMPACT_COMBAT;
                }
            }
        } else if (isBlocking) {
            for (FakeCombatCreature attacker : opponents) {
                if (attacker.toughness > source.power
                        && attacker.toughness <= source.power + amount) {
                    return AiDecision.IMPACT_COMBAT;
                }
            }
            int totAtkPower = opponents.stream().mapToInt(c -> c.power).sum();
            if (source.toughness <= totAtkPower
                    && source.toughness + amount > totAtkPower) {
                return AiDecision.WILL_PLAY;
            }
        }
        return AiDecision.CANT_PLAY_AI;
    }


    /**
     * Scenario :
     *   - Psychic Frog attaque et n'est pas bloquée
     *
     * Comportement attendu : WillPlay
     */
    @Test
    @DisplayName("Cas 1 : Psychic Frog attaque sans bloqueur -> l'AI active l'aptitude")
    void testPsychicFrog_Attacking_Unblocked_ShouldActivate() {
        FakeCombatCreature frog = new FakeCombatCreature("Psychic Frog", 1, 1);

        AiDecision decision = doCombatAdaptLogic(
                frog,
                1,// +1/+1
                true,// isAttacking
                false,// isBlocked <- non bloquée
                false,
                java.util.Collections.emptyList()
        );

        assertEquals(AiDecision.WILL_PLAY, decision,
                "Une attaquante non bloquée bénéficie toujours du pump -> WillPlay"
        );
    }

    /**
     * Scenario :
     *   - Psychic Frog (1/1 + 1 pump = 2/2) attaque
     *   - Bloquée par une créature 2/2 : toughness (2) > power (1) ET toughness (2) <= power+1 (2)
     *
     * Comportement attendu : WillPlay
     */
    @Test
    @DisplayName("Cas 2 : Psychic Frog attaque, bloquee, pump tue le bloqueur -> l'AI active l'aptitude")
    void testPsychicFrog_Attacking_Blocked_PumpKillsBlocker_ShouldActivate() {
        FakeCombatCreature frog    = new FakeCombatCreature("Psychic Frog", 1, 1);
        FakeCombatCreature blocker = new FakeCombatCreature("Créature adverse 2/2", 2, 2);

        // Act
        AiDecision decision = doCombatAdaptLogic(
                frog,
                1,
                true,
                true,// isBlocked
                false,
                java.util.List.of(blocker)
        );

        // Assert
        assertEquals(AiDecision.WILL_PLAY, decision,
                "Le pump permet de tuer le bloqueur (toughness <= power+amount) -> WillPlay"
        );
    }

    /**
     * Scenario :
     *   - Psychic Frog (1/1) attaque, bloquée par une créature 3/3
     *   - Sans pump : frog meurt (3 >= 1), avec +1 : frog survit (toughness 2 > power bloqueur 3? Non)
     *     Ici on vérifie la branche de survie : toughness (1) <= totBlkPower (3) ET toughness+1 (2) > totBlkPower (3)? Non.
     *
     * En réalité on a besoin d'un cas où toughness <= totBlkPower && toughness + amount > totBlkPower
     *   - Psychic Frog (1/2), bloquée par une créature 1/1 et 1/1 (totBlkPower = 2)
     *   - toughness (2) <= 2 ET toughness+1 (3) > 2 -> ImpactCombat
     *
     * Comportement attendu : ImpactCombat
     */
    @Test
    @DisplayName("Cas 3 : Psychic Frog attaque, pump lui permet de survivre aux degats des bloqueurs -> ImpactCombat")
    void testPsychicFrog_Attacking_Blocked_PumpSavesFrog_ShouldImpactCombat() {
        FakeCombatCreature frog     = new FakeCombatCreature("Psychic Frog", 1, 2);
        FakeCombatCreature blocker1 = new FakeCombatCreature("Créature 1/1 A", 1, 1);
        FakeCombatCreature blocker2 = new FakeCombatCreature("Créature 1/1 B", 1, 1);
        // totBlkPower = 2, toughness (2) <= 2 -> frog meurt sans pump
        // toughness+1 (3) > 2 -> frog survit avec pump

        AiDecision decision = doCombatAdaptLogic(
                frog,
                1,
                true,
                true,
                false,
                java.util.List.of(blocker1, blocker2)
        );

        assertEquals(AiDecision.IMPACT_COMBAT, decision,
                "Le pump permet à Psychic Frog de survivre (toughness+amount > totBlkPower) -> ImpactCombat"
        );
    }

    /**
     * Scenario :
     *   - Psychic Frog (1/1) bloque Shalai (3/4)
     *   - toughness de Shalai (4) > power de frog (1) : sans pump, frog ne tue pas Shalai
     *   - toughness de Shalai (4) <= power+1 (2)? Non, 4 > 2.
     *
     * On choisit un attaquant avec toughness = 2 pour que pump (1) le tue :
     *   - Frog (1/1) bloque une créature 3/2
     *   - toughness (2) > power (1) : sans pump, frog ne tue pas
     *   - toughness (2) <= power+1 (2) : avec +1, frog tue l'attaquant -> ImpactCombat
     *
     * Comportement attendu : ImpactCombat
     */
    @Test
    @DisplayName("Cas 4 : Psychic Frog bloque, pump lui permet de tuer l'attaquant -> ImpactCombat")
    void testPsychicFrog_Blocking_PumpKillsAttacker_ShouldImpactCombat() {
        // Arrange
        FakeCombatCreature frog     = new FakeCombatCreature("Psychic Frog", 1, 1);
        FakeCombatCreature attacker = new FakeCombatCreature("Créature adverse 3/2", 3, 2);
        // toughness (2) > power (1) : sans pump, ne tue pas l'attaquant
        // toughness (2) <= power+1 (2) : avec +1, frog tue l'attaquant

        AiDecision decision = doCombatAdaptLogic(
                frog,
                1,
                false,
                false,
                true,// isBlocking
                java.util.List.of(attacker)
        );

        assertEquals(AiDecision.IMPACT_COMBAT, decision,
                "Le pump permet à Psychic Frog de tuer l'attaquant (toughness <= power+amount) -> ImpactCombat"
        );
    }

    /**
     * Scenario :
     *   - Psychic Frog (1/2) bloque une créature 3/3
     *   - totAtkPower = 3, toughness (2) <= 3 -> frog meurt sans pump
     *   - toughness+1 (3) > 3? Non -> 3 > 3 est faux
     *
     * Cas valide : Frog (1/3) bloque une créature 4/4 (totAtkPower = 4)
     *   - toughness (3) <= 4 ET toughness+1 (4) > 4? Non.
     *
     * Cas valide : Frog (1/3) bloque une créature 3/3 (totAtkPower = 3)
     *   - toughness (3) <= 3 ET toughness+1 (4) > 3 -> WillPlay
     *
     * Comportement attendu : WillPlay
     */
    @Test
    @DisplayName("Cas 5 : Psychic Frog bloque, pump lui permet de survivre aux degats -> WillPlay")
    void testPsychicFrog_Blocking_PumpSavesFrog_ShouldActivate() {
        FakeCombatCreature frog     = new FakeCombatCreature("Psychic Frog", 1, 3);
        FakeCombatCreature attacker = new FakeCombatCreature("Créature adverse 3/3", 3, 3);
        // totAtkPower = 3, toughness (3) <= 3 -> frog meurt sans pump
        // toughness+1 (4) > 3 -> frog survit avec pump

        AiDecision decision = doCombatAdaptLogic(
                frog,
                1,
                false,
                false,
                true,
                java.util.List.of(attacker)
        );

        assertEquals(AiDecision.WILL_PLAY, decision,
                "Le pump permet à Psychic Frog de survivre en tant que bloqueur (toughness+amount > totAtkPower) -> WillPlay"
        );
    }

    /**
     * Scenario :
     *   - Psychic Frog n'attaque pas et ne bloque pas
     *
     * Comportement attendu : CantPlayAi
     */
    @Test
    @DisplayName("Cas 6 : Psychic Frog hors combat -> l'AI n'active pas l'aptitude")
    void testPsychicFrog_NotInCombat_ShouldNotActivate() {
        FakeCombatCreature frog = new FakeCombatCreature("Psychic Frog", 1, 1);


        AiDecision decision = doCombatAdaptLogic(
                frog,
                1,
                false,// isAttacking
                false,
                false,// isBlocking
                java.util.Collections.emptyList()
        );

        assertEquals(AiDecision.CANT_PLAY_AI, decision,
                "Hors combat (ni attaquante ni bloquante), " +
                        "doCombatAdaptLogic doit retourner CantPlayAi"
        );
    }
}