package com.turtlehoarder.cobblemonchallenge.util;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ChallengeUtil {

    public static boolean isPokemonPartOfChallenge(PokemonEntity pokemonEntity) {
        if (pokemonEntity.getBattleId().get().isEmpty()) {
            return false;
        }
        UUID battleId = pokemonEntity.getBattleId().get().get();
        for (PokemonBattle battle : ChallengeBattleBuilder.challengeBattles) {
            if (battleId.equals(battle.getBattleId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBattleChallenge(UUID battleId) {
        return ChallengeBattleBuilder.challengeBattles.stream().anyMatch(battle -> battle.getBattleId().equals(battleId));
    }

    public static boolean isPlayerOnline(ServerPlayer player) {
        return player.getServer().getPlayerList().getPlayer(player.getUUID()) != null;
    }

    public static ChallengeCommand.ChallengeRequest createChallengeRequest(ServerPlayer challengerPlayer, ServerPlayer challengedPlayer, int level) {
        String key = UUID.randomUUID().toString().replaceAll("-", "");
        ChallengeCommand.ChallengeRequest newRequest = new ChallengeCommand.ChallengeRequest(key, challengerPlayer, challengedPlayer, level, System.currentTimeMillis());
        return newRequest;
    }
}
