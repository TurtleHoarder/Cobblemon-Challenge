package com.turtlehoarder.cobblemonchallenge.common.gui;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.turtlehoarder.cobblemonchallenge.common.battle.BattlePeekTracker;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BattlePeekMenuProvider implements MenuProvider {

    private final List<BattlePeekTracker.SeenPokemon> seenPokemon;
    private final BattleActor opponentActor;

    public BattlePeekMenuProvider(List<BattlePeekTracker.SeenPokemon> seenPokemon, BattleActor opponentActor) {
        this.seenPokemon = seenPokemon;
        this.opponentActor = opponentActor;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Opponent's Revealed Pokemon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        BattlePeekMenu menu = new BattlePeekMenu(pContainerId, pPlayerInventory);
        populateMenu(menu);
        return menu;
    }

    private void populateMenu(BattlePeekMenu menu) {
        // Fill all slots with gray glass panes first
        ItemStack grayFiller = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        grayFiller.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int slot = 0; slot < 54; slot++) {
            menu.setItem(slot, menu.getStateId(), grayFiller.copy());
        }

        // Place seen pokemon in the left column (slots 0, 9, 18, 27, 36, 45)
        for (int i = 0; i < seenPokemon.size() && i < 6; i++) {
            int slot = i * 9;
            BattlePeekTracker.SeenPokemon seen = seenPokemon.get(i);

            ItemStack pokemonItem = matchPokemonItem(seen);
            pokemonItem.set(DataComponents.CUSTOM_NAME, Component.literal(
                    ChatFormatting.RED + String.format("%s (lvl%d)", seen.getSpecies(), seen.getLevel())));

            ItemLore lore = ChallengeUtil.generatePeekLoreForPokemon(seen);
            pokemonItem.set(DataComponents.LORE, lore);

            menu.setItem(slot, menu.getStateId(), pokemonItem);
        }
    }

    /**
     * Try to match a SeenPokemon to an actual BattlePokemon in the opponent's team
     * for an accurate sprite. Falls back to a Poke Ball item.
     */
    private ItemStack matchPokemonItem(BattlePeekTracker.SeenPokemon seen) {
        if (opponentActor != null) {
            for (BattlePokemon bp : opponentActor.getPokemonList()) {
                String bpSpecies = bp.getEffectedPokemon().getSpecies().getName();
                if (bpSpecies.equalsIgnoreCase(seen.getSpecies())) {
                    return PokemonItem.from(bp.getEffectedPokemon(), 1);
                }
            }
        }
        // Fallback: Poke Ball with species name
        ItemStack fallback = new ItemStack(CobblemonItems.POKE_BALL.asItem());
        fallback.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        return fallback;
    }
}
