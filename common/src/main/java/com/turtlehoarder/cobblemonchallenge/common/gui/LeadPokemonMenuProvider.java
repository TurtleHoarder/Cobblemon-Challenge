package com.turtlehoarder.cobblemonchallenge.common.gui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import com.turtlehoarder.cobblemonchallenge.common.battle.ChallengeFormat;
import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LeadPokemonMenuProvider implements MenuProvider {

    private final ServerPlayer selector;
    private final ServerPlayer rival;
    private PartyStore p1Party;
    private int rivalSelectedPokemon = 0;
    private final LeadPokemonSelectionSession selectionSession; // Menu Provider reports to wrapper when pokemon is selected

    private boolean guiModifierFlag = false;
    private enum MenuState {WAITING_FOR_BOTH, WAITING_FOR_RIVAL, WAITING_FOR_PLAYER};
    private MenuState menuState = MenuState.WAITING_FOR_BOTH;
    private LeadPokemonMenu openedMenu;
    public List<Integer> selectedSlots = new ArrayList<Integer>();
    // Mappings of # pokemon selected and where in the menu to put it
    // Maps for Doubles / 2v2s
    Map<Integer, Integer> allySlotToMenuIDDoubles = Map.of(1,20,2,21);
    Map<Integer, Integer> rivalSlotToMenuIDDoubles = Map.of(1,23,2,24);
    // Maps for 3v3
    Map<Integer, Integer> allySlotToMenuID3v3 = Map.of(1,12,2,21,3,30);
    Map<Integer, Integer> rivalSlotToMenuID3v3 = Map.of(1,13,2,22,3,31);
    // Maps for 4v4
    Map<Integer, Integer> allySlotToMenuID4v4 = Map.of(1,12,2,21,3,30, 4, 39);
    Map<Integer, Integer> rivalSlotToMenuID4v4 = Map.of(1,13,2,22,3,31, 4, 40);
    // Maps for 5v5
    Map<Integer, Integer> allySlotToMenuID5v5 = Map.of(1,12,2,21,3,30, 4, 39, 5, 48);
    Map<Integer, Integer> rivalSlotToMenuID5v5 = Map.of(1,13,2,22,3,31, 4, 40, 5, 49);

    private ChallengeCommand.ChallengeRequest request;

    public LeadPokemonMenuProvider(LeadPokemonSelectionSession wrapper, ServerPlayer selector, ServerPlayer rivalPlayer, ChallengeCommand.ChallengeRequest request) {
        this.selector = selector;
        this.rival = rivalPlayer;
        this.selectionSession = wrapper;
        this.request = request;
    }
    @Override
    public @NotNull Component getDisplayName() {
        if (request.format().getTotalPokemonSelected() == 1)
            return Component.literal("Select your Lead Pokemon");
        else
            return Component.literal("Select %d Pokemon for %s".formatted(request.format().getTotalPokemonSelected(), request.format().getTitle()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        LeadPokemonMenu leadPokemonMenu = new LeadPokemonMenu(this, pContainerId, pPlayerInventory);
        setupPokemonRepresentation(leadPokemonMenu);
        this.openedMenu = leadPokemonMenu;
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
            BattlePokemon copy = BattlePokemon.Companion.safeCopyOf(pokemon);
            pokemon = ChallengeUtil.applyFormatTransformations(ChallengeFormat.STANDARD_6V6, copy, request.level()).getEffectedPokemon(); // Apply battle transformations to each pokemon
            ItemStack pokemonItem = PokemonItem.from(pokemon, 1);
            pokemonItem.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.AQUA + String.format("%s (lvl%d)", pokemon.getDisplayName().getString(), request.level())));
            ItemLore pokemonLoreTag = ChallengeUtil.generateLoreTagForPokemon(pokemon);

            pokemonItem.set(DataComponents.LORE, pokemonLoreTag);
            leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), pokemonItem);
        }

        // Set enemy side:
        for (int x= 0; x < p2Party.size(); x++) {
            int itemSlot = (x * 9) + 8; // Righthand column of the menu
            Pokemon pokemon = p2Party.get(x);
            if (pokemon == null) {
                continue;
            }
            if (selectionSession.teamPreviewOn()) {
                ItemStack pokemonItem = PokemonItem.from(pokemon, 1);
                pokemonItem.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.RED + String.format("%s's %s (lvl%d)", rival.getDisplayName().getString(), pokemon.getDisplayName().getString(), request.level())));
                leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), pokemonItem);
            } else {
                ItemStack pokemonItem = new ItemStack(CobblemonItems.POKE_BALL.asItem());
                pokemonItem.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                pokemonItem.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.RED + String.format("%s's Pokemon", rival.getDisplayName().getString())));
                leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), pokemonItem);
            }
        }
    }

    private Map<Integer, Integer> getPositionAllyMap() {
        return switch (request.format().getTotalPokemonSelected()) {
            case 5 -> allySlotToMenuID5v5;
            case 4 -> allySlotToMenuID4v4;
            case 3 -> allySlotToMenuID3v3;
            case 2 -> allySlotToMenuIDDoubles;
            case 1 -> Collections.emptyMap();
            default -> null;
        };
    }

    private Map<Integer, Integer> getPositionRivalMap() {
        return switch (request.format().getTotalPokemonSelected()) {
            case 5 -> rivalSlotToMenuID5v5;
            case 4 -> rivalSlotToMenuID4v4;
            case 3 -> rivalSlotToMenuID3v3;
            case 2 -> rivalSlotToMenuIDDoubles;
            case 1 -> Collections.emptyMap();
            default -> null;
        };
    }

    private void setGlassDisplayName(ItemStack s, int secondsLeft) {
        s.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.AQUA + String.format("Seconds left to choose: %d", secondsLeft)));
        ItemLore glassLoreTag = generateLoreTagForGlass(s);
        s.set(DataComponents.LORE, glassLoreTag);
    }

    private ItemLore generateLoreTagForGlass(ItemStack s) {
        List<Component> components = new ArrayList<>();
        Component additionalInformation;
        if (menuState == MenuState.WAITING_FOR_RIVAL) {
            additionalInformation = Component.literal(ChatFormatting.WHITE + String.format("Waiting on %s...", rival.getDisplayName().getString()));
        } else if (menuState == MenuState.WAITING_FOR_PLAYER) {
            if (request.format().getTotalPokemonSelected() == 1) {
                additionalInformation = Component.literal(ChatFormatting.WHITE + "Waiting on you to select Lead...");
            } else {
                additionalInformation = Component.literal(ChatFormatting.WHITE + "Waiting on you to select %d pokemon...".formatted(request.format().getTotalPokemonSelected()));
            }
        } else {
            additionalInformation = Component.literal(ChatFormatting.WHITE + "Waiting on both players to select leads...");
        }
        components.add(additionalInformation);
        return new ItemLore(components);
    }

    private void setupGlassFiller(LeadPokemonMenu leadPokemonMenu) {
        int timeLeft = (int) Math.ceil(((selectionSession.creationTime + LeadPokemonSelectionSession.LEAD_TIMEOUT_MILLIS) - System.currentTimeMillis()) / 1000f);
        for (int column = 1; column <= 7; column++) {
            for (int row = 0; row < 6; row++) {
                int itemSlot = (row * 9) + column;
                ItemStack itemFiller;
                if (column == 1) { // Green for player side
                    if ((row +(guiModifierFlag ? 0 : 1)) % 2 == 0 || menuState == MenuState.WAITING_FOR_RIVAL)
                        itemFiller = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
                    else
                        itemFiller = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
                } else if (column == 7) { // red for challenger side
                    if ((row +(guiModifierFlag ? 0 : 1)) % 2 == 0 || menuState == MenuState.WAITING_FOR_PLAYER)
                        itemFiller = new ItemStack(Items.PINK_STAINED_GLASS_PANE);
                    else
                        itemFiller = new ItemStack(Items.RED_STAINED_GLASS_PANE);
                } else {
                    itemFiller = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                }
                setGlassDisplayName(itemFiller, timeLeft);
                leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), itemFiller);
            }
        }
    }

    // Setup the item slots for selected pokemon
    private void setupPokemonSelection(LeadPokemonMenu leadPokemonMenu) {
        int timeLeft = (int) Math.ceil(((selectionSession.creationTime + LeadPokemonSelectionSession.LEAD_TIMEOUT_MILLIS) - System.currentTimeMillis()) / 1000f);
        if (request.format().getTotalPokemonSelected() == 1) { // Do special effects for single-selection of pokemon
             if (selectedSlots.size() > 0) {
                Pokemon selectedPokemon = p1Party.get(selectedSlots.get(0));
                ItemStack glassFiller = new ItemStack(ChallengeUtil.getDisplayBlockForPokemon(selectedPokemon));
                setGlassDisplayName(glassFiller, timeLeft);
                leadPokemonMenu.setItemSlotMulti(glassFiller, 12, 30, 20);
                ItemStack pokemonFiller = PokemonItem.from(selectedPokemon, 1);
                pokemonFiller.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.GREEN + String.format("You've selected %s as your lead", selectedPokemon.getDisplayName().getString())));
                leadPokemonMenu.setItem(21, leadPokemonMenu.getStateId(), pokemonFiller);
            }
            if (rivalSelectedPokemon == selectionSession.getMaxPokemonSelection()) {
                ItemStack glassFiller = new ItemStack(Blocks.GLASS_PANE);
                setGlassDisplayName(glassFiller, timeLeft);
                leadPokemonMenu.setItemSlotMulti(glassFiller, 23, 13, 31);
                ItemStack pokeballFiller = new ItemStack(CobblemonItems.POKE_BALL.asItem());
                pokeballFiller.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                pokeballFiller.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.RED + String.format("%s has selected their lead", rival.getDisplayName().getString())));
                leadPokemonMenu.setItem(22, leadPokemonMenu.getStateId(), pokeballFiller);
            }
        } else { // For other formats, refer to the maps
            Map<Integer, Integer> allySlotMap = getPositionAllyMap();
            Map<Integer, Integer> rivalSlotMap = getPositionRivalMap();
            for (int rivalSelectedNumber = 0; rivalSelectedNumber < rivalSelectedPokemon; rivalSelectedNumber++) {
                ItemStack pokeballFiller = new ItemStack(CobblemonItems.POKE_BALL.asItem());
                pokeballFiller.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                pokeballFiller.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.RED + String.format("%s has selected Pokemon #" + (rivalSelectedNumber + 1), rival.getDisplayName().getString())));
                leadPokemonMenu.setItem(rivalSlotMap.get(rivalSelectedNumber + 1), leadPokemonMenu.getStateId(), pokeballFiller);
            }
            for (int selectedNumber = 0; selectedNumber < selectedSlots.size(); selectedNumber++) {
                Pokemon selectedPokemon = p1Party.get(selectedSlots.get(selectedNumber));
                ItemStack pokemonFiller = PokemonItem.from(selectedPokemon, 1);
                // Small notification for letting players know they can deselect
                pokemonFiller.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.GREEN + String.format("You've selected %s as Pokemon #" + (selectedNumber + 1), selectedPokemon.getDisplayName().getString())));
                List<Component> components = new ArrayList<>();
                if (selectedNumber == 0) {
                    components.add(Component.literal(String.format(ChatFormatting.  + "This is your lead")));
                }
                if (selectedSlots.size() < request.format().getTotalPokemonSelected()) {
                    components.add(Component.literal(String.format(ChatFormatting.YELLOW  + "Click to unselect pokemmon")));
                }
                pokemonFiller.set(DataComponents.LORE, new ItemLore(components));
                leadPokemonMenu.setItem(allySlotMap.get(selectedNumber + 1), leadPokemonMenu.getStateId(), pokemonFiller);
            }
        }
    }

    protected void onGeneralMenuClick(LeadPokemonMenu menu, int pSlotId) {
        // Check unclick / undo for unfinished selections
        Map<Integer, Integer> allyMap = getPositionAllyMap();
        Collection<Integer> possiblePositions = allyMap.values(); // Possible slots to be clicked on
        if (possiblePositions != null && possiblePositions.contains(pSlotId)) {
            if (selectedSlots.size() < request.format().getTotalPokemonSelected()) { // If all pokemon haven't been selected yet...
                int selectedSlot = -1;
                for (Map.Entry<Integer, Integer> entry : allyMap.entrySet()) {
                    int slot = entry.getKey();
                    int menuSlot = entry.getValue();
                    if (menuSlot == pSlotId) {
                        selectedSlot = slot;
                        break;
                    }
                }
                if (selectedSlot != -1 && selectedSlot <= selectedSlots.size()) {
                    selectedSlots.remove(selectedSlot - 1);
                    refreshInnerGuiItems();
                    selectionSession.onPokemonUnselected(this);
                }
            }
        }
    }

    protected void onSelectPokemonSlot(LeadPokemonMenu menu, int slotId) {
        if (selectedSlots.size() < selectionSession.getMaxPokemonSelection()) {
            Pokemon selectedPokemon = p1Party.get(slotId);
            if (selectedPokemon != null && !selectedSlots.contains(slotId)) {
                selectedSlots.add(slotId);
                refreshInnerGuiItems(); // Update GUI
                selectionSession.onPokemonSelected(this); // Update the upstream so other player receives state update
                updateMenuState();
            }
        }
    }

    private void refreshInnerGuiItems() {
        setupGlassFiller(this.openedMenu);
        setupPokemonSelection(this.openedMenu);
    }

    public void timedGuiUpdate() {
        guiModifierFlag = !guiModifierFlag; // Switch the color of glass every second
        refreshInnerGuiItems();
    }

    public void forceCloseMenu() {
        if (openedMenu != null) {
            openedMenu.invalidateMenu();
        }
    }

    protected void onPlayerCloseContainer() {
        selectionSession.onPlayerCloseMenu(selector);
    }

    private void updateMenuState() {
        if (rivalSelectedPokemon == selectionSession.getMaxPokemonSelection() && selectedSlots.size() < selectionSession.getMaxPokemonSelection()) {
            menuState = MenuState.WAITING_FOR_PLAYER;
        } else if (selectedSlots.size() == selectionSession.getMaxPokemonSelection() && rivalSelectedPokemon < selectionSession.getMaxPokemonSelection()) {
            menuState = MenuState.WAITING_FOR_RIVAL;
        } else {
            menuState = MenuState.WAITING_FOR_BOTH;
        }
    }

    public void updateRivalCount(int newCount) {
        this.rivalSelectedPokemon = newCount;
        updateMenuState();
    }
}
