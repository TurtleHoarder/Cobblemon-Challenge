package com.turtlehoarder.cobblemonchallenge.common.gui;

import com.turtlehoarder.cobblemonchallenge.common.ChallengeLang;

import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.battle.*;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
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

    public void onPokemonUnselected(LeadPokemonMenuProvider menuProvider) {
        getOtherMenu(menuProvider).updateRivalCount(menuProvider.selectedSlots.size());
    }

    private void beginBattle() {
        SESSIONS_TO_CANCEL.add(this);
        challengerMenuProvider.forceCloseMenu();
        challengedMenuProvider.forceCloseMenu();
        ChallengeBattleBuilder challengeBuilder = new ChallengeBattleBuilder();
        try {
            challengeBuilder.lvlxpvp(originRequest.challengerPlayer(), originRequest.challengedPlayer(),originRequest.level(), challengerMenuProvider.selectedSlots, challengedMenuProvider.selectedSlots, originRequest.format());
        } catch (ChallengeBuilderException e) {
            originRequest.challengedPlayer().displayClientMessage(ChallengeLang.get("cobblemonchallenge.error.init_failed"), false);
            originRequest.challengerPlayer().displayClientMessage(ChallengeLang.get("cobblemonchallenge.error.init_failed"), false);
        }
    }

    public boolean teamPreviewOn() {
        return originRequest.preview();
    }

    private boolean isBattleReady() {
        return challengedMenuProvider.selectedSlots.size() == getMaxPokemonSelection() && challengerMenuProvider.selectedSlots.size() == getMaxPokemonSelection();
    }

    public int getMaxPokemonSelection() {
        return originRequest.format().getTotalPokemonSelected();
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
            originRequest.challengerPlayer().sendSystemMessage(ChallengeLang.get("cobblemonchallenge.challenge.lead_timeout"));
        }
        if (ChallengeUtil.isPlayerOnline(originRequest.challengedPlayer())) {
            originRequest.challengedPlayer().sendSystemMessage(ChallengeLang.get("cobblemonchallenge.challenge.lead_timeout"));
        }
        challengedMenuProvider.forceCloseMenu();
        challengerMenuProvider.forceCloseMenu();
    }

    public void onPlayerCloseMenu(ServerPlayer player) {
        if (!timedOut && !isBattleReady() && !closedOut) { // Don't send the message if the menus were forced close by timeout
            closedOut = true;
            ServerPlayer otherPlayer = getOtherPlayer(player);
            player.sendSystemMessage(ChallengeLang.get("cobblemonchallenge.challenge.cancelled_by_you", otherPlayer.getDisplayName().getString()));
            otherPlayer.sendSystemMessage(ChallengeLang.get("cobblemonchallenge.challenge.cancelled_by_rival", player.getDisplayName().getString()));
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
