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
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Vector;

public class ChallengeBattleBuilder {

    public static Vector<PokemonEntity> clonedPokemonList = new Vector<>();
    public static Vector<PokemonBattle> challengeBattles = new Vector<>();
    public void lvlxpvp(ServerPlayer player1, ServerPlayer player2, BattleFormat battleFormat, int level) {

        PartyStore p1Party = Cobblemon.INSTANCE.getStorage().getParty(player1);
        PartyStore p2Party = Cobblemon.INSTANCE.getStorage().getParty(player2);

        // Clone parties so original is not effected
        List<BattlePokemon> player1Team = p1Party.toBattleTeam(true, false, null);
        List<BattlePokemon> player2Team = p2Party.toBattleTeam(true, false, null);

        // Heal both sides and set to appropriate level
        for (BattlePokemon pokemon : player1Team) {
            pokemon.getEffectedPokemon().setLevel(level);
            pokemon.getEffectedPokemon().heal();
        }

        for (BattlePokemon pokemon : player2Team) {
            pokemon.getEffectedPokemon().setLevel(level);
            pokemon.getEffectedPokemon().heal();
        }

        PlayerBattleActor player1Actor = new PlayerBattleActor(player1.getUUID(), player1Team);
        PlayerBattleActor player2Actor = new PlayerBattleActor(player2.getUUID(), player2Team);
        BattleRegistry br = Cobblemon.INSTANCE.getBattleRegistry();

        PokemonBattle battle = br.startBattle(battleFormat, new BattleSide(player1Actor), new BattleSide(player2Actor));

        challengeBattles.add(battle); // Keep a list of challenge battles to keep track of cloned pokemon
    }
}
