package com.turtlehoarder.cobblemonchallenge.gui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.util.ChallengeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

public class LeadPokemonMenuProvider implements MenuProvider {

    private final ServerPlayer selector;
    private final ServerPlayer rival;
    public LeadPokemonMenuProvider(ServerPlayer selector, ServerPlayer rivalPlayer) {
        this.selector = selector;
        this.rival = rivalPlayer;
    }
    @Override
    public Component getDisplayName() {
        return Component.literal("Select your Lead Pokemon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        //ChestMenu baseChestMenu = ChestMenu.sixRows(pContainerId, pPlayerInventory);

        LeadPokemonMenu leadPokemonMenu = new LeadPokemonMenu(this, pContainerId, pPlayerInventory);
        setupPokemonRepresentation(leadPokemonMenu);
        return leadPokemonMenu;
    }

    private void setupPokemonRepresentation(LeadPokemonMenu leadPokemonMenu) {
        PartyStore p1Party = Cobblemon.INSTANCE.getStorage().getParty(selector);
        PartyStore p2Party = Cobblemon.INSTANCE.getStorage().getParty(rival);

        for (int x = 0; x < p1Party.size(); x ++) {
            int itemSlot = x * 9; // Lefthand column of the menu
            Pokemon pokemon = p1Party.get(x);
            if (pokemon == null) {
                continue;
            }
            ItemStack pokemonItem = PokemonItem.from(pokemon, 1);
            pokemonItem.setHoverName(Component.literal(ChatFormatting.AQUA + String.format("%s (lvl%d)", pokemon.getDisplayName().getString(), pokemon.getLevel())));
            ListTag pokemonLoreTag = ChallengeUtil.generateLoreTagForPokemon(pokemon);
            pokemonItem.getOrCreateTagElement("display").put("Lore", pokemonLoreTag);
            leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), pokemonItem);
        }

        // Set enemy side:
        for (int x= 0; x < p2Party.size(); x++) {
            CobblemonChallenge.LOGGER.debug("Setting index for : " + x);
            int itemSlot = (x * 9) + 8; // Righthand column of the menu
            Pokemon pokemon = p2Party.get(x);
            if (pokemon == null) {
                continue;
            }
            ItemStack pokemonItem = PokemonItem.from(pokemon, 1);
            pokemonItem.setHoverName(Component.literal(ChatFormatting.RED + String.format("%s's %s (lvl%d)", rival.getDisplayName().getString(), pokemon.getDisplayName().getString(), pokemon.getLevel())));
            leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), pokemonItem);
        }

    }
    public ServerPlayer getSelectorPlayer() {
        return selector;
    }
}
