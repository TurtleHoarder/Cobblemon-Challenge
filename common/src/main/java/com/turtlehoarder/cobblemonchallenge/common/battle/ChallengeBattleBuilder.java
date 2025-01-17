package com.turtlehoarder.cobblemonchallenge.common.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.Function;

public class ChallengeBattleBuilder {

    public static Vector<PokemonEntity> clonedPokemonList = new Vector<>();
    public static Vector<PokemonBattle> challengeBattles = new Vector<>();
    private ChallengeFormat format = ChallengeFormat.STANDARD_6V6;

    public void lvlxpvp(ServerPlayer player1, ServerPlayer player2, int level, List<Integer> player1Selection, List<Integer> player2Selection, ChallengeFormat format) throws ChallengeBuilderException {
        PartyStore p1Party = Cobblemon.INSTANCE.getStorage().getParty(player1);
        PartyStore p2Party = Cobblemon.INSTANCE.getStorage().getParty(player2);
        Set<String> rules = Set.of(BattleRules.OBTAINABLE, BattleRules.PAST, BattleRules.UNOBTAINABLE);
        BattleFormat bf = new BattleFormat("cobblemon", BattleTypes.INSTANCE.getSINGLES(), rules,9,level);

        // I don't want to maintain my own version of battle initialization, so instead I'm going to leverage the partyAccessor function that's passed in.
        // Instead of pointing to Cobblemon storage for reference, we will create a fake party storage based off the parameters and then return that instead. BattleBuilder will handle the rest
        FakePartyAccessor accessor = new FakePartyAccessor(player1Selection, player2Selection, format);
        // Clone parties so original is not effected
        BattleBuilder.INSTANCE.pvp1v1(player1, player2, p1Party.get(player1Selection.get(0)).getUuid(), p2Party.get(player2Selection.get(0)).getUuid(), bf, true, true, accessor)
                .ifSuccessful(battle -> {
                    challengeBattles.add(battle); // Keep a list of challenge battles to keep track of cloned pokemon
                    return Unit.INSTANCE;
                });
    }

}