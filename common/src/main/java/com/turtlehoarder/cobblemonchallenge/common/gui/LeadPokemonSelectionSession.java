package com.turtlehoarder.cobblemonchallenge.common.gui;

import com.cobblemon.mod.common.battles.BattleFormat;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.common.battle.ChallengeBuilderException;
import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.Vector;

public class LeadPokemonSelectionSession {
    private final LeadPokemonMenuProvider challengerMenuProvider;
    private final LeadPokemonMenuProvider challengedMenuProvider;
    private final ChallengeCommand.ChallengeRequest originRequest;
    private final UUID uuid;
    public long creationTime;
    private int pokemonToSelect = 1;
    private boolean timedOut = false;

    private boolean closedOut = false;

    public static Vector<LeadPokemonSelectionSession> SESSIONS_TO_CANCEL = new Vector<>();

    public static int LEAD_TIMEOUT_MILLIS = 90000;

    public LeadPokemonSelectionSession(UUID uuid, long creationTime, ChallengeCommand.ChallengeRequest request) {
        this.originRequest = request;
        this.uuid = uuid;
        this.creationTime = creationTime;
        challengerMenuProvider = new LeadPokemonMenuProvider(this, request.challengerPlayer(), request.challengedPlayer(), request);
        challengedMenuProvider = new LeadPokemonMenuProvider(this, request.challengedPlayer(), request.challengerPlayer(), request);
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
        int minLevel = originRequest.minLevel();
        int maxLevel = originRequest.maxLevel();
        int handicapP1 = originRequest.handicapP1();
        int handicapP2 = originRequest.handicapP2();
        SESSIONS_TO_CANCEL.add(this);
        challengerMenuProvider.forceCloseMenu();
        challengedMenuProvider.forceCloseMenu();
        ChallengeBattleBuilder challengeBuilder = new ChallengeBattleBuilder();
        try {
            challengeBuilder.lvlxpvp(originRequest.challengerPlayer(), originRequest.challengedPlayer(), BattleFormat.Companion.getGEN_9_SINGLES(), minLevel, maxLevel, handicapP1, handicapP2, challengerMenuProvider.selectedSlots, challengedMenuProvider.selectedSlots);
        } catch (ChallengeBuilderException e) {
            e.printStackTrace();
        }
    }

    public boolean teamPreviewOn() {
        return originRequest.preview();
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

    public void timeoutRequest() {
        this.timedOut = true;
        if (ChallengeUtil.isPlayerOnline(originRequest.challengerPlayer())) {
            originRequest.challengerPlayer().sendSystemMessage(Component.literal(ChatFormatting.RED + "Challenge timed out: Selecting lead took too long"));
        }
        if (ChallengeUtil.isPlayerOnline(originRequest.challengedPlayer())) {
            originRequest.challengedPlayer().sendSystemMessage(Component.literal(ChatFormatting.RED + "Challenge timed out: Selecting lead took too long"));
        }
        challengedMenuProvider.forceCloseMenu();
        challengerMenuProvider.forceCloseMenu();
    }

    public void onPlayerCloseMenu(ServerPlayer player) {
        if (!timedOut && !isBattleReady() && !closedOut) { // Don't send the message if the menus were forced close by timeout
            closedOut = true;
            ServerPlayer otherPlayer = getOtherPlayer(player);
            player.sendSystemMessage(Component.literal(ChatFormatting.RED + String.format(String.format("You have cancelled the challenge to %s", otherPlayer.getDisplayName().getString()))));
            otherPlayer.sendSystemMessage(Component.literal(ChatFormatting.RED + String.format("%s has cancelled the request", player.getDisplayName().getString())));
            challengerMenuProvider.forceCloseMenu();
            challengedMenuProvider.forceCloseMenu();
            SESSIONS_TO_CANCEL.add(this);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public void doTick() {
        challengedMenuProvider.timedGuiUpdate();
        challengerMenuProvider.timedGuiUpdate();
    }
}
