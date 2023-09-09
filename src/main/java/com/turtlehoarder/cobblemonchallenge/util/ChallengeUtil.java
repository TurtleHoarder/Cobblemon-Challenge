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
            case "normal" -> Blocks.WHITE_WOOL;
            case "fire" -> Blocks.RED_WOOL;
            case "water" -> Blocks.BLUE_WOOL;
            case "grass" -> Blocks.GREEN_WOOL;
            case "electric" -> Blocks.YELLOW_WOOL;
            case "ice" -> Blocks.ICE;
            case "fighting" -> Blocks.RED_CONCRETE;
            case "poison" -> Blocks.PURPLE_WOOL;
            case "ground" -> Blocks.BROWN_WOOL;
            case "flying" -> Items.LIGHT_BLUE_WOOL;
            case "psychic" -> Blocks.MAGENTA_WOOL;
            case "bug" -> Blocks.LIME_WOOL;
            case "rock" -> Blocks.GRAY_WOOL;
            case "ghost" -> Blocks.BLACK_STAINED_GLASS;
            case "dragon" -> Blocks.BLUE_CONCRETE;
            case "dark" -> Blocks.BLACK_WOOL;
            case "steel" -> Blocks.IRON_BLOCK;
            case "fairy" -> Blocks.PINK_STAINED_GLASS;
            default -> Blocks.BEDROCK;
        };
    }

    public static ListTag generateLoreTagForPokemon(Pokemon pokemon) {
        ListTag loreTag = new ListTag();
        Component abilityComponent = Component.literal(String.format(ChatFormatting.AQUA  + "Ability: %s", LocalizationUtilsKt.lang(String.format("ability.%s", pokemon.getAbility().getName()))));
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
