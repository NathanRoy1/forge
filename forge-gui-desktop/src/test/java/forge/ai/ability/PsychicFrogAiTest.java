package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class PsychicFrogAiTest extends AITest {

    /**
     * Helper: builds a combat where {@code attacker} attacks {@code defender}'s player
     * and {@code blocker} blocks it, then injects it into the game's PhaseHandler.
     */
    private void setupCombatWithBlocker(Game game, Player defender, Card attacker, Card blocker) throws Exception {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker.getController(), false);

        Combat combat = new Combat(attacker.getController());
        combat.addAttacker(attacker, defender);
        combat.addBlocker(attacker, blocker);

        Field combatField = PhaseHandler.class.getDeclaredField("combat");
        combatField.setAccessible(true);
        combatField.set(game.getPhaseHandler(), combat);
    }

    @Test
    public void testPsychicFrogActivatesCounterWhenItKillsTheAttacker() throws Exception {
        // Cas positif : Psychic Frog (1/2) bloque un Grizzly Bears (2/2).
        // Sans marqueur, le Frog ne peut pas tuer le 2/2 (puissance 1 < endurance 2).
        // Avec le marqueur +1/+1, il devient 2/3 et peut tuer le 2/2 (puissance 2 = endurance 2).
        // doCombatAdaptLogic : attacker.toughness(2) > frog.power(1)  ET  attacker.toughness(2) <= frog.power+1(2)
        // => L'IA doit activer : ImpactCombat
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setTeam(0);
        opponent.setTeam(1);

        Card psychicFrog = addCard("Psychic Frog", ai); // 1/2
        addCardToZone("Plains", ai, ZoneType.Hand);      // carte pour payer le coût de défausse

        Card grizzlyBears = addCard("Grizzly Bears", opponent); // 2/2
        grizzlyBears.setSickness(false);

        setupCombatWithBlocker(game, ai, grizzlyBears, psychicFrog);
        game.getAction().checkStateEffects(true);

        SpellAbility putCounterSa = findSAWithPrefix(psychicFrog, "Discard");
        assertNotNull("Psychic Frog doit avoir une aptitude de marqueur à coût de défausse", putCounterSa);

        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, putCounterSa).decision();
        assertEquals("L'IA doit activer l'aptitude de Psychic Frog pour tuer le 2/2 adverse",
                AiPlayDecision.ImpactCombat, decision);
    }

    @Test
    public void testPsychicFrogDoesNotActivateWhenCounterDoesNotHelp() throws Exception {
        // Cas négatif : Psychic Frog (1/2) bloque un Centaur Courser (3/3).
        // Même avec le marqueur +1/+1 (devient 2/3), le Frog ne peut toujours pas tuer le 3/3
        // (puissance 2 < endurance 3), et le Frog meurt quand même (endurance 2+1=3 < puissance 3).
        // doCombatAdaptLogic : attacker.toughness(3) > frog.power+1(2)  et  frog.toughness+1(3) <= attacker.power(3)
        // => L'IA ne doit pas activer : CantPlayAi
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setTeam(0);
        opponent.setTeam(1);

        Card psychicFrog = addCard("Psychic Frog", ai); // 1/2

        Card centaurCourser = addCard("Centaur Courser", opponent); // 3/3
        centaurCourser.setSickness(false);

        setupCombatWithBlocker(game, ai, centaurCourser, psychicFrog);
        game.getAction().checkStateEffects(true);

        SpellAbility putCounterSa = findSAWithPrefix(psychicFrog, "Discard");
        assertNotNull("Psychic Frog doit avoir une aptitude de marqueur à coût de défausse", putCounterSa);

        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, putCounterSa).decision();
        assertEquals("L'IA ne doit pas activer l'aptitude de Psychic Frog si le marqueur ne change rien au combat",
                AiPlayDecision.CantPlayAi, decision);
    }
}
