package com.turtlehoarder.cobblemonchallenge.gui;

import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import org.apache.logging.log4j.Level;

public class LeadPokemonMenu extends ChestMenu {

    private LeadPokemonMenuProvider menuProvider;
    public LeadPokemonMenu(LeadPokemonMenuProvider menuProvider, int pContainerId, Inventory pPlayerInventory) {
        super(MenuType.GENERIC_9x6, pContainerId, pPlayerInventory, new SimpleContainer(9 * 6), 6);
        this.menuProvider = menuProvider;
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        CobblemonChallenge.LOGGER.info(String.format("Clicked on a location: pSlotId=%d pButton=%d, clicktype=%s, player=%s!!", pSlotId, pButton, pClickType.toString(), pPlayer.getDisplayName().getString()));
        if (isValidSlotIndex(pSlotId)) {
            CobblemonChallenge.LOGGER.info("Clicked Item: " + getSlot(pSlotId).getItem().getDisplayName().getString());
            if (pSlotId % 9 == 0) {
                CobblemonChallenge.LOGGER.info(String.format("Selected pokemon @ %d", pSlotId / 9));
                menuProvider.onSelectPokemonSlot(this, pSlotId / 9);
            }
        }
    }
}
