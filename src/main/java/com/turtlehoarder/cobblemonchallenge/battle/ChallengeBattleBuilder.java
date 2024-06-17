package com.turtlehoarder.cobblemonchallenge.battle;

import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.api.ChallengeProperties;
import com.turtlehoarder.cobblemonchallenge.battle.pokemon.ChallengeBattlePokemon;

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

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import kotlin.Unit;

public class ChallengeBattleBuilder {

    public static Vector<PokemonEntity> clonedPokemonList = new Vector<>();
    public static Vector<PokemonBattle> challengeBattles = new Vector<>();
    private final ChallengeFormat format = ChallengeFormat.STANDARD_6V6;
    public void lvlxpvp(ServerPlayer player1, ServerPlayer player2, BattleFormat battleFormat, ChallengeProperties properties, List<Integer> player1Selection, List<Integer> player2Selection) throws ChallengeBuilderException {

        PartyStore p1Party = Cobblemon.INSTANCE.getStorage().getParty(player1);
        PartyStore p2Party = Cobblemon.INSTANCE.getStorage().getParty(player2);

        // Clone parties so original is not effected
        List<BattlePokemon> player1Team = createBattleTeamFromParty(p1Party, player1Selection, properties, properties.getHandicapP1());
        List<BattlePokemon> player2Team = createBattleTeamFromParty(p2Party, player2Selection, properties, properties.getHandicapP2());

        PlayerBattleActor player1Actor = new PlayerBattleActor(player1.getUUID(), player1Team);
        PlayerBattleActor player2Actor = new PlayerBattleActor(player2.getUUID(), player2Team);
        BattleRegistry br = Cobblemon.INSTANCE.getBattleRegistry();

         br.startBattle(battleFormat, new BattleSide(player1Actor), new BattleSide(player2Actor), false).ifSuccessful(battle -> {
             challengeBattles.add(battle); // Keep a list of challenge battles to keep track of cloned pokemon
             return Unit.INSTANCE;
         });
    }

    // Method to create our own clones according to the format
    private List<BattlePokemon> createBattleTeamFromParty(PartyStore party, List<Integer> selectedSlots, ChallengeProperties properties, int handicap) throws ChallengeBuilderException {

        List<BattlePokemon> battlePokemonList = new ArrayList<>();
        if (format == ChallengeFormat.STANDARD_6V6) {
            int leadSlot = selectedSlots.get(0);
            Pokemon leadPokemon = party.get(leadSlot);
            if (leadPokemon == null) {
                CobblemonChallenge.LOGGER.error("Mysterious null lead pokemon selected.");
                throw new ChallengeBuilderException();
            }

            // BattlePokemon needs to be the subclass ChallengeBattlePokemon to differentiate BattlePokemon when filtering battles
            ChallengeBattlePokemon leadBattlePokemon = ChallengeBattlePokemon.Companion.safeCopyOfChallenge(leadPokemon);
            leadBattlePokemon.applyChallengePropertiesToEffectedPokemon(properties.getMinLevel(), properties.getMaxLevel(), handicap,true);
            for (int slot = 0; slot < party.size(); slot++) {
                if (slot != leadSlot) {
                    Pokemon pokemon = party.get(slot);
                    if (pokemon != null) {
                        ChallengeBattlePokemon battlePokemon = ChallengeBattlePokemon.Companion.safeCopyOfChallenge(pokemon);
                        battlePokemon.applyChallengePropertiesToEffectedPokemon(properties.getMinLevel(), properties.getMaxLevel(), handicap,true);
                        battlePokemonList.add(battlePokemon);
                    }
                }
            }
        }
        return battlePokemonList;
    }
}