package com.turtlehoarder.cobblemonchallenge.util;

import com.cobblemon.mod.common.battles.BattleFormat;
import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeBuilderException;
import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.gui.LeadPokemonMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class LeadPokemonSelectionWrapper {
    private final LeadPokemonMenuProvider challengerMenuProvider;
    private final LeadPokemonMenuProvider challengedMenuProvider;
    private final ChallengeCommand.ChallengeRequest originRequest;
    private final UUID uuid;
    public long creationTime;
    private int pokemonToSelect = 1;

    public LeadPokemonSelectionWrapper(UUID uuid, long creationTime,  ChallengeCommand.ChallengeRequest request) {
        this.originRequest = request;
        this.uuid = uuid;
        this.creationTime = creationTime;
        challengerMenuProvider = new LeadPokemonMenuProvider(this, request.challengerPlayer(), request.challengedPlayer());
        challengedMenuProvider = new LeadPokemonMenuProvider(this, request.challengedPlayer(), request.challengerPlayer());
    }

    public void openPlayerMenus() {
        originRequest.challengedPlayer().openMenu(challengedMenuProvider);
        originRequest.challengerPlayer().openMenu(challengerMenuProvider);
    }

    private ServerPlayer getOtherPlayer(ServerPlayer player) {
        if (player == originRequest.challengerPlayer()) {
            return originRequest.challengedPlayer();
        } else {
            return originRequest.challengerPlayer();
        }
    }

    // Method called when a player selects a new pokemon from their selection screen
    public void onPokemonSelected(LeadPokemonMenuProvider menuProvider) {
        if (isBattleReady()) {
            // Finally! We can start the battle
            CobblemonChallenge.LOGGER.info("All pokemon selected, initiating battle sequence");
            beginBattle();
        } else {
            getOtherMenu(menuProvider).updateRivalCount(menuProvider.selectedSlots.size());
        }
    }

    private void beginBattle() {
        int level = originRequest.level();
        ChallengeBattleBuilder challengeBuilder = new ChallengeBattleBuilder();
        try {
            challengeBuilder.lvlxpvp(originRequest.challengerPlayer(), originRequest.challengedPlayer(), BattleFormat.Companion.getGEN_9_SINGLES(), level, challengerMenuProvider.selectedSlots, challengedMenuProvider.selectedSlots);
        } catch (ChallengeBuilderException e) {
            e.printStackTrace();
        }
    }

    private boolean isBattleReady() {
        return challengedMenuProvider.selectedSlots.size() == getMaxPokemonSelection() && challengerMenuProvider.selectedSlots.size() == getMaxPokemonSelection();
    }

    // TODO: Make this more flexible for formats like 3v3
    public int getMaxPokemonSelection() {
        return pokemonToSelect;
    }

    private LeadPokemonMenuProvider getOtherMenu(LeadPokemonMenuProvider menu) {
        if (menu == challengedMenuProvider) {
            return challengerMenuProvider;
        } else {
            return challengedMenuProvider;
        }
    }

    public void onPlayerCloseMenu(ServerPlayer player) {
        ServerPlayer otherPlayer = getOtherPlayer(player);
        challengerMenuProvider.forceCloseMenu();
        challengedMenuProvider.forceCloseMenu();
        otherPlayer.sendSystemMessage(Component.literal(ChatFormatting.RED + String.format("%s has canceled the request", player.getDisplayName().getString())));
    }

    public void doTick() {
        challengedMenuProvider.timedGuiUpdate();
        challengerMenuProvider.timedGuiUpdate();
    }
}
