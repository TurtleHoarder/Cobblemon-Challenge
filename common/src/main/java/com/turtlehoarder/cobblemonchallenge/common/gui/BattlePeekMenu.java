package com.turtlehoarder.cobblemonchallenge.common.gui;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

public class BattlePeekMenu extends ChestMenu {

    public BattlePeekMenu(int pContainerId, Inventory pPlayerInventory) {
        super(MenuType.GENERIC_9x6, pContainerId, pPlayerInventory, new SimpleContainer(9 * 6), 6);
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        // Read-only menu - all clicks are no-ops
    }
}
