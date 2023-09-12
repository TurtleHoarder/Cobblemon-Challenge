package com.turtlehoarder.cobblemonchallenge.gui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LeadPokemonMenuProvider implements MenuProvider {

    private final ServerPlayer selector;
    private final ServerPlayer rival;
    private PartyStore p1Party;
    private Pokemon selectedPokemon;
    private boolean rivalSelectedPokemon = false;

    private enum MenuState {WAITING_FOR_BOTH, WAITING_FOR_RIVAL, WAITING_FOR_PLAYER};
    private MenuState menuState = MenuState.WAITING_FOR_BOTH;

    public LeadPokemonMenuProvider(ServerPlayer selector, ServerPlayer rivalPlayer) {
        this.selector = selector;
        this.rival = rivalPlayer;
    }
    @Override
    public @NotNull Component getDisplayName() {
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
        p1Party = Cobblemon.INSTANCE.getStorage().getParty(selector);
        PartyStore p2Party = Cobblemon.INSTANCE.getStorage().getParty(rival);

        setupGlassFiller(leadPokemonMenu);
        for (int x = 0; x < p1Party.size(); x ++) {
            int itemSlot = x * 9; // Lefthand column of the menu
            Pokemon pokemon = p1Party.get(x);
            if (pokemon == null) // Skip any empty slots in the pokemon team
                continue;
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

    private void setGlassDisplayName(ItemStack s) {
        s.setHoverName(Component.literal(ChatFormatting.BLUE + String.format("Seconds left to choose: %d", 60)));
        ListTag glassLoreTag = generateLoreTagForGlass(s);
        s.getOrCreateTagElement("display").put("Lore", glassLoreTag);
    }

    private ListTag generateLoreTagForGlass(ItemStack s) {
        ListTag loreTag = new ListTag();
        Component additionalInformation;
        if (menuState == MenuState.WAITING_FOR_RIVAL) {
            additionalInformation = Component.literal(ChatFormatting.LIGHT_PURPLE + String.format("Waiting on %s...", rival.getDisplayName()));
        } else if (menuState == MenuState.WAITING_FOR_PLAYER) {
            additionalInformation = Component.literal(ChatFormatting.LIGHT_PURPLE + "Waiting on you to select Lead...");
        } else {
            additionalInformation = Component.literal(ChatFormatting.LIGHT_PURPLE + "Waiting on both players to select leads...");
        }
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(additionalInformation)));
        return loreTag;
    }

    private void setupGlassFiller(LeadPokemonMenu leadPokemonMenu) {
        for (int column = 1; column <= 7; column++) {
            for (int row = 0; row < 6; row++) {
                int itemSlot = (row * 9) + column;
                ItemStack glass;
                if (column == 1) { // Green for player side
                    if (row % 2 == 0)
                        glass = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
                    else
                        glass = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
                } else if (column == 7) { // red for challenger side
                    glass = new ItemStack(Items.RED_STAINED_GLASS_PANE);
                } else {
                    glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                }
                setGlassDisplayName(glass);
                if (selectedPokemon != null) {
                    if (itemSlot == 12 || itemSlot == 30 || itemSlot == 20) {
                        glass = new ItemStack(ChallengeUtil.getDisplayBlockForPokemon(selectedPokemon));
                    } else if (itemSlot == 21) {
                        glass = PokemonItem.from(selectedPokemon, 1);
                        glass.setHoverName(Component.literal(ChatFormatting.GREEN + String.format("You've selected %s as your lead", selectedPokemon.getDisplayName().getString())));
                    }
                }
                leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), glass);
            }
        }
    }

    protected void onSelectPokemonSlot(LeadPokemonMenu menu, int slotId) {
        Pokemon selectedPokemon = p1Party.get(slotId);
        if (selectedPokemon != null) {
            CobblemonChallenge.LOGGER.info(String.format("Player selected: %s", selectedPokemon.getDisplayName().getString()));
            this.selectedPokemon = selectedPokemon;
            setupGlassFiller(menu);
        } else {
            CobblemonChallenge.LOGGER.info("Player selected null pokemon slot");
        }
    }

    public ServerPlayer getSelectorPlayer() {
        return selector;
    }
}
