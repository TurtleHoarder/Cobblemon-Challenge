package com.turtlehoarder.cobblemonchallenge.common.gui;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

public class LeadPokemonMenu extends ChestMenu {

    private LeadPokemonMenuProvider menuProvider;
    private boolean isValid = true;
    public LeadPokemonMenu(LeadPokemonMenuProvider menuProvider, int pContainerId, Inventory pPlayerInventory) {
        super(MenuType.GENERIC_9x6, pContainerId, pPlayerInventory, new SimpleContainer(9 * 6), 6);
        this.menuProvider = menuProvider;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        menuProvider.onPlayerCloseContainer();
    }

    public void invalidateMenu() {
        this.isValid = false;
    }
    @Override
    public boolean stillValid(@NotNull Player player) {
        return super.stillValid(player) && isValid;
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        if (isValidSlotIndex(pSlotId)) {
            if (pSlotId == -999) { // If the container is closed... bubble the event up
                //menuProvider.onPlayerCloseContainer();
            } else {
                if (pSlotId % 9 == 0) {
                    menuProvider.onSelectPokemonSlot(this, pSlotId / 9);
                }
            }
        }
    }
}