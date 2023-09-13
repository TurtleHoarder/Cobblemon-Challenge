package com.turtlehoarder.cobblemonchallenge.util;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeFormat;
import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
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
            case "fire" -> Blocks.RED_STAINED_GLASS_PANE;
            case "water", "dragon" -> Blocks.BLUE_STAINED_GLASS_PANE;
            case "grass" -> Items.GRASS;
            case "electric" -> Blocks.YELLOW_STAINED_GLASS_PANE;
            case "ice" -> Blocks.CYAN_STAINED_GLASS_PANE;
            case "fighting" -> Items.RED_STAINED_GLASS_PANE;
            case "poison" -> Blocks.PURPLE_STAINED_GLASS_PANE;
            case "ground" -> Blocks.BROWN_STAINED_GLASS_PANE;
            case "flying" -> Items.FEATHER;
            case "psychic" -> Blocks.MAGENTA_STAINED_GLASS_PANE;
            case "bug" -> Items.COBWEB;
            case "rock" -> Blocks.COBBLESTONE;
            case "ghost", "dark" -> Blocks.BLACK_STAINED_GLASS;
            case "steel" -> Blocks.IRON_BARS;
            case "fairy" -> Blocks.PINK_STAINED_GLASS_PANE;
            default -> Blocks.GLASS_PANE;
        };
    }

    public static ListTag generateLoreTagForPokemon(Pokemon pokemon) {
        ListTag loreTag = new ListTag();
        Component abilityComponent = Component.literal(String.format(ChatFormatting.GRAY  + "Ability: %s", ChatFormatting.YELLOW + LocalizationUtilsKt.lang(String.format("ability.%s", pokemon.getAbility().getName())).getString()));
        String natureKey = pokemon.getNature().getName().toLanguageKey();
        Component natureComponent = Component.literal(String.format(ChatFormatting.GRAY + "Nature: %s", ChatFormatting.YELLOW + LocalizationUtilsKt.lang(String.format("nature.%s",natureKey.substring(natureKey.lastIndexOf('.') + 1))).getString()));
        String statSeparator = ChatFormatting.GRAY + " / ";
        Component statsPartOne = Component.literal(String.format(ChatFormatting.RED + "HP: %d" + statSeparator + ChatFormatting.GOLD + "Atk: %d" + statSeparator + ChatFormatting.YELLOW + "Def: %d", pokemon.getHp(), pokemon.getAttack(), pokemon.getDefence()));
        Component statsPartTwo = Component.literal(String.format(ChatFormatting.AQUA + "SpA: %d" + statSeparator + ChatFormatting.GREEN + "SpD: %d" + statSeparator + ChatFormatting.LIGHT_PURPLE + "Spe: %d", pokemon.getSpecialAttack(), pokemon.getSpecialDefence(), pokemon.getSpeed()));
        Component moveSeperator = Component.literal( "Moves:");
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(abilityComponent)));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(natureComponent)));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(statsPartOne)));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(statsPartTwo)));
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(moveSeperator)));
        pokemon.getMoveSet().getMoves().forEach(move -> {
            Component moveComponent = Component.literal(ChatFormatting.WHITE + String.format("%s - %d/%d", move.getDisplayName().getString() + ChatFormatting.GRAY, move.getMaxPp(), move.getMaxPp()));
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(moveComponent)));
        });
        return loreTag;
    }

    public static BattlePokemon applyFormatTransformations(ChallengeFormat format, BattlePokemon pokemon, int level) {
        if (format == ChallengeFormat.STANDARD_6V6) {
            pokemon.getEffectedPokemon().setLevel(level);
            pokemon.getEffectedPokemon().heal();
        }
        return pokemon;
    }
}