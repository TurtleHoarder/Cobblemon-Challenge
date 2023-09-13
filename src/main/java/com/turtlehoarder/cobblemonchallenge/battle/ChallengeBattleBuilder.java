package com.turtlehoarder.cobblemonchallenge.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class ChallengeBattleBuilder {

    public static Vector<PokemonEntity> clonedPokemonList = new Vector<>();
    public static Vector<PokemonBattle> challengeBattles = new Vector<>();
    private ChallengeFormat format = ChallengeFormat.STANDARD_6V6;
    public void lvlxpvp(ServerPlayer player1, ServerPlayer player2, BattleFormat battleFormat, int level, List<Integer> player1Selection, List<Integer> player2Selection) throws ChallengeBuilderException {

        PartyStore p1Party = Cobblemon.INSTANCE.getStorage().getParty(player1);
        PartyStore p2Party = Cobblemon.INSTANCE.getStorage().getParty(player2);

        // Clone parties so original is not effected
        List<BattlePokemon> player1Team = createBattleTeamFromParty(p1Party, player1Selection, level);
        List<BattlePokemon> player2Team = createBattleTeamFromParty(p2Party, player2Selection, level);

        PlayerBattleActor player1Actor = new PlayerBattleActor(player1.getUUID(), player1Team);
        PlayerBattleActor player2Actor = new PlayerBattleActor(player2.getUUID(), player2Team);
        BattleRegistry br = Cobblemon.INSTANCE.getBattleRegistry();

        PokemonBattle battle = br.startBattle(battleFormat, new BattleSide(player1Actor), new BattleSide(player2Actor));
        challengeBattles.add(battle); // Keep a list of challenge battles to keep track of cloned pokemon
    }

    // Method to create our own clones according to the format
    private List<BattlePokemon> createBattleTeamFromParty(PartyStore party, List<Integer> selectedSlots, int level) throws ChallengeBuilderException {
        List<BattlePokemon> battlePokemonList = new ArrayList<BattlePokemon>();
        if (format == ChallengeFormat.STANDARD_6V6) {
            int leadSlot = selectedSlots.get(0);
            Pokemon leadPokemon = party.get(leadSlot);
            if (leadPokemon == null) {
                CobblemonChallenge.LOGGER.error("Mysterious null lead pokemon selected.");
                throw new ChallengeBuilderException();
            }
            BattlePokemon leadBattlePokemon = BattlePokemon.Companion.safeCopyOf(leadPokemon);
            battlePokemonList.add(applyFormatTransformations(leadBattlePokemon, level));
            for (int slot = 0; slot < party.size(); slot++) {
                if (slot != leadSlot) {
                    Pokemon pokemon = party.get(slot);
                    if (pokemon != null) {
                        BattlePokemon battlePokemon = applyFormatTransformations(BattlePokemon.Companion.safeCopyOf(pokemon), level);
                        battlePokemonList.add(battlePokemon);
                    }
                }
            }
        }
        return battlePokemonList;
    }

    private BattlePokemon applyFormatTransformations(BattlePokemon pokemon, int level) {
        pokemon.getEffectedPokemon().setLevel(level);
        pokemon.getEffectedPokemon().heal();
        return pokemon;
    }
}
