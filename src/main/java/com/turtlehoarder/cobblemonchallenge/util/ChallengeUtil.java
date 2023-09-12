package com.turtlehoarder.cobblemonchallenge.util;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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

    public static ItemLike getDisplayBlockForPokemon(Pokemon pokemon) {
        ElementalType type = pokemon.getPrimaryType();
        return switch (type.getName()) {
            case "normal" -> Blocks.WHITE_STAINED_GLASS_PANE;
            case "fire", "fighting" -> Blocks.RED_STAINED_GLASS_PANE;
            case "water", "dragon" -> Blocks.BLUE_STAINED_GLASS_PANE;
            case "grass" -> Blocks.GREEN_STAINED_GLASS_PANE;
            case "electric" -> Blocks.YELLOW_STAINED_GLASS_PANE;
            case "ice" -> Blocks.LIGHT_BLUE_STAINED_GLASS_PANE;
            case "poison" -> Blocks.PURPLE_STAINED_GLASS_PANE;
            case "ground", "rock" -> Blocks.BROWN_STAINED_GLASS_PANE;
            case "flying" -> Items.LIGHT_BLUE_STAINED_GLASS_PANE;
            case "psychic" -> Blocks.MAGENTA_STAINED_GLASS_PANE;
            case "bug" -> Blocks.LIME_STAINED_GLASS_PANE;
            case "ghost" -> Blocks.GRAY_STAINED_GLASS;
            case "dark", "steel" -> Blocks.GRAY_STAINED_GLASS_PANE;
            case "fairy" -> Blocks.PINK_STAINED_GLASS_PANE;
            default -> Blocks.GLASS_PANE;
        };
    }

    public static ListTag generateLoreTagForPokemon(Pokemon pokemon) {
        ListTag loreTag = new ListTag();
        Component abilityComponent = Component.literal(String.format(ChatFormatting.AQUA  + "Ability: %s", LocalizationUtilsKt.lang(String.format("ability.%s", pokemon.getAbility().getName())).getString()));
        Component moveSeperator = Component.literal( "Moves:");
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(abilityComponent)));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(moveSeperator)));
        pokemon.getMoveSet().getMoves().forEach(move -> {
            Component moveComponent = Component.literal(ChatFormatting.LIGHT_PURPLE + String.format("%s - %d/%d", move.getDisplayName().getString() + ChatFormatting.WHITE, move.getCurrentPp(), move.getMaxPp()));
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(moveComponent)));
        });
        return loreTag;
    }
}
