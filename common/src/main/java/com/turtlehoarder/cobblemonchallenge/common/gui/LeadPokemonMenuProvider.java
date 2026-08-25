package com.turtlehoarder.cobblemonchallenge.common.gui;

import com.turtlehoarder.cobblemonchallenge.common.ChallengeLang;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.turtlehoarder.cobblemonchallenge.common.battle.ChallengeFormat;
import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
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
            return ChallengeLang.get("cobblemonchallenge.gui.select_lead");
        else
            return ChallengeLang.get("cobblemonchallenge.gui.select_n", request.format().getTotalPokemonSelected(), request.format().getTitle());
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
            pokemonItem.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.pokemon_name", pokemon.getDisplayName(false).getString(), request.level()));
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
                pokemonItem.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.rival_pokemon_name", rival.getDisplayName().getString(), pokemon.getDisplayName(false).getString(), request.level()));
                leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), pokemonItem);
            } else {
                ItemStack pokemonItem = new ItemStack(CobblemonItems.POKE_BALL.asItem());
                pokemonItem.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                pokemonItem.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.rival_pokemon", rival.getDisplayName().getString()));
                leadPokemonMenu.setItem(itemSlot, leadPokemonMenu.getStateId(), pokemonItem);
            }
        }
    }

    private void setGlassDisplayName(ItemStack s, int secondsLeft) {
        s.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.seconds_left", secondsLeft));
        ItemLore glassLoreTag = generateLoreTagForGlass(s);
        s.set(DataComponents.LORE, glassLoreTag);
    }

    private ItemLore generateLoreTagForGlass(ItemStack s) {
        List<Component> components = new ArrayList<>();
        Component additionalInformation;
        if (menuState == MenuState.WAITING_FOR_RIVAL) {
            additionalInformation = ChallengeLang.get("cobblemonchallenge.gui.waiting_rival", rival.getDisplayName().getString());
        } else if (menuState == MenuState.WAITING_FOR_PLAYER) {
            if (request.format().getTotalPokemonSelected() == 1) {
                additionalInformation = ChallengeLang.get("cobblemonchallenge.gui.waiting_you_lead");
            } else {
                additionalInformation = ChallengeLang.get("cobblemonchallenge.gui.waiting_you_n", request.format().getTotalPokemonSelected());
            }
        } else {
            additionalInformation = ChallengeLang.get("cobblemonchallenge.gui.waiting_both");
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
             if (!selectedSlots.isEmpty()) {
                Pokemon selectedPokemon = p1Party.get(selectedSlots.getFirst());
                ItemStack glassFiller = new ItemStack(ChallengeUtil.getDisplayBlockForPokemon(selectedPokemon));
                setGlassDisplayName(glassFiller, timeLeft);
                leadPokemonMenu.setItemSlotMulti(glassFiller, 12, 30, 20);
                ItemStack pokemonFiller = PokemonItem.from(selectedPokemon, 1);
                pokemonFiller.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.you_selected_lead", selectedPokemon.getDisplayName(false).getString()));
                leadPokemonMenu.setItem(21, leadPokemonMenu.getStateId(), pokemonFiller);
            }
            if (rivalSelectedPokemon == selectionSession.getMaxPokemonSelection()) {
                ItemStack glassFiller = new ItemStack(Blocks.WHITE_STAINED_GLASS_PANE);
                setGlassDisplayName(glassFiller, timeLeft);
                leadPokemonMenu.setItemSlotMulti(glassFiller, 23, 13, 31);
                ItemStack pokeballFiller = new ItemStack(CobblemonItems.POKE_BALL.asItem());
                pokeballFiller.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                pokeballFiller.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.rival_selected_lead", rival.getDisplayName().getString()));
                leadPokemonMenu.setItem(22, leadPokemonMenu.getStateId(), pokeballFiller);
            }
        } else { // For other formats, refer to the maps
            Map<Integer, Integer> allySlotMap = LeadPokemonStaticMappings.getPositionAllyMap(request);
            Map<Integer, Integer> rivalSlotMap = LeadPokemonStaticMappings.getPositionRivalMap(request);
            for (int rivalSelectedNumber = 0; rivalSelectedNumber < rivalSelectedPokemon; rivalSelectedNumber++) {
                ItemStack pokeballFiller = new ItemStack(CobblemonItems.POKE_BALL.asItem());
                pokeballFiller.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
                pokeballFiller.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.rival_selected_n", rival.getDisplayName().getString(), rivalSelectedNumber + 1));
                leadPokemonMenu.setItem(rivalSlotMap.get(rivalSelectedNumber + 1), leadPokemonMenu.getStateId(), pokeballFiller);
            }
            for (int selectedNumber = 0; selectedNumber < selectedSlots.size(); selectedNumber++) {
                Pokemon selectedPokemon = p1Party.get(selectedSlots.get(selectedNumber));
                ItemStack pokemonFiller = PokemonItem.from(selectedPokemon, 1);
                // Small notification for letting players know they can deselect
                pokemonFiller.set(DataComponents.CUSTOM_NAME, ChallengeLang.get("cobblemonchallenge.gui.you_selected_n", selectedPokemon.getDisplayName(false).getString(), selectedNumber + 1));
                List<Component> components = new ArrayList<>();
                if (selectedNumber == 0 && request.format().getBattleType().getSlotsPerActor() == 1) {
                    components.add(ChallengeLang.get("cobblemonchallenge.gui.this_is_lead"));
                }
                if (selectedSlots.size() < request.format().getTotalPokemonSelected()) {
                    components.add(ChallengeLang.get("cobblemonchallenge.gui.click_unselect"));
                }
                pokemonFiller.set(DataComponents.LORE, new ItemLore(components));
                leadPokemonMenu.setItem(allySlotMap.get(selectedNumber + 1), leadPokemonMenu.getStateId(), pokemonFiller);
            }

            // Do lock-in glass items:
            ItemStack lockinGlassFillerAlly = new ItemStack(Blocks.WHITE_STAINED_GLASS_PANE);
            ItemStack lockinGlassFillerRival = new ItemStack(Blocks.WHITE_STAINED_GLASS_PANE);
            setGlassDisplayName(lockinGlassFillerAlly, timeLeft);
            setGlassDisplayName(lockinGlassFillerRival, timeLeft);

            if (isLockedIn()) {
                leadPokemonMenu.setItemSlotMulti(lockinGlassFillerAlly, LeadPokemonStaticMappings.getLockinGlassPositionAlly(request));
            }
            if (isRivalLockedIn()) {
                leadPokemonMenu.setItemSlotMulti(lockinGlassFillerRival, LeadPokemonStaticMappings.getLockinGlassPositionRival(request));
            }
        }
    }

    protected void onGeneralMenuClick(LeadPokemonMenu menu, int pSlotId) {
        // Check unclick / undo for unfinished selections
        Map<Integer, Integer> allyMap = LeadPokemonStaticMappings.getPositionAllyMap(request);
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

    private boolean isRivalLockedIn() {
        return rivalSelectedPokemon == request.format().getTotalPokemonSelected();
    }

    // Quick method to see if player is locked-in / has all pokemon selected
    private boolean isLockedIn() {
        return selectedSlots.size() == request.format().getTotalPokemonSelected();
    }
}
