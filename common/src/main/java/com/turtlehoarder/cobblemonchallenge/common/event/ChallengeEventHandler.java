package com.turtlehoarder.cobblemonchallenge.common.event;

import com.turtlehoarder.cobblemonchallenge.common.ChallengeLang;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.*;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyReferencePacket;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.gui.LeadPokemonSelectionSession;
import com.turtlehoarder.cobblemonchallenge.common.util.*;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import kotlin.Unit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class ChallengeEventHandler {

    public static void registerEvents() {

        // Register Cobblemon-Related Events using CobblemonEvents
        registerPostVictoryEvent();
        registerCobblemonSavePrevention();

        // Use Architectury API to abstract event calls away from Neoforge/Fabric
        PlayerEvent.PLAYER_QUIT.register(ChallengeEventHandler::onPlayerLoggedOut);
        TickEvent.SERVER_POST.register(ChallengeEventHandler::onServerTick);

    }

    /*
        Since this plugin uses cloned pokemon in its battles, there will be a *cloned* pokemon left behind after the battle is complete. These
        events ensure that these cloned entities are tracked and removed when a battle ends via Victory, disconnect, or server shutdown
     */
    public static boolean registerPostVictoryEvent() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, (battleVictoryEvent) -> {
            UUID battleId = battleVictoryEvent.getBattle().getBattleId();
            // Send victory message to victor
            if (ChallengeUtil.isBattleChallenge(battleId)) {
                PokemonBattle battle = battleVictoryEvent.getBattle();
                List<ServerPlayer> participants = new ArrayList<>(battle.getPlayers());
                Iterator<ServerPlayer> participantIterator = participants.listIterator();
                while (participantIterator.hasNext()) {
                    ServerPlayer player = participantIterator.next();
                    for (BattleActor actor : battleVictoryEvent.getWinners()) {
                        actor.getPlayerUUIDs().forEach(winnerUUID -> {
                            if (player.getUUID().equals(winnerUUID)) {
                                player.displayClientMessage(ChallengeLang.get("cobblemonchallenge.battle.won"), false);
                                participantIterator.remove();
                            }
                        });
                    }
                }
                // Remaining participants have lost
                participants.forEach(loser -> loser.displayClientMessage(ChallengeLang.get("cobblemonchallenge.battle.lost"), false));
            }
            // Remove challenge battle from tracking
            Iterator<PokemonBattle> challengeBattleIterator = ChallengeBattleBuilder.challengeBattles.iterator();
            while (challengeBattleIterator.hasNext()) {
                PokemonBattle battle = challengeBattleIterator.next();
                if (battle.getBattleId().equals(battleVictoryEvent.getBattle().getBattleId())) {
                    challengeBattleIterator.remove();
                    CobblemonChallenge.LOGGER.debug(String.format("Removing tracked Challenge battle id: %s", battleVictoryEvent.getBattle().getBattleId()));
                }
            }
            return Unit.INSTANCE;
        });
        return true;
    }

    // Prevent Challenge-mons from being saved to the world to prevent odd scenarios where duplicates can be spawned and re-caught
    private static void registerCobblemonSavePrevention() {
        CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.subscribe(Priority.HIGHEST, (saveEvent) -> {
            PokemonEntity pokemonEntity = saveEvent.getPokemonEntity();
            if (ChallengeUtil.isPokemonPartOfChallenge(pokemonEntity)) {
                CobblemonChallenge.LOGGER.debug(String.format(String.format("Cancelling save event for challenge-mon: %s", pokemonEntity.getDisplayName().getString())));
                saveEvent.cancel();
            }
            return Unit.INSTANCE;
        });
    }

    // When a player leaves, check to see if they were part of any challenges. If they are, remove all cloned pokemon that are associated with this challenge. This will prevent duplicate mons from remaining behind
    public static void onPlayerLoggedOut(ServerPlayer serverPlayer) {
        Iterator<PokemonBattle> battleIterator = ChallengeBattleBuilder.challengeBattles.iterator();
        while (battleIterator.hasNext()) {
            PokemonBattle battle = battleIterator.next();
            if (battle.getPlayers().contains(serverPlayer)) {
                CobblemonChallenge.LOGGER.debug(String.format("Found hanging battle! (%s)", battle.getBattleId()));
                battleIterator.remove(); // Remove hanging battle from list
            }
        }
    }

    public static void onServerTick(MinecraftServer server) {
        int tickCount = server.getTickCount();
        if (tickCount % 20 == 0) {
            long nowTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, ChallengeCommand.ChallengeRequest>> requestIterator = ChallengeCommand.CHALLENGE_REQUESTS.entrySet().iterator();
            while (requestIterator.hasNext()) {
                Map.Entry<String, ChallengeCommand.ChallengeRequest> requestMap = requestIterator.next();
                ChallengeCommand.ChallengeRequest request = requestMap.getValue();
                if (request.createdTime() + CobblemonChallenge.REQUEST_EXPIRATION_MILLIS < nowTime) {
                    if (ChallengeUtil.isPlayerOnline(request.challengedPlayer())) {
                        request.challengedPlayer().displayClientMessage(ChallengeLang.get("cobblemonchallenge.challenge.expired_from", request.challengerPlayer().getDisplayName().getString()), false);
                    }
                    if (ChallengeUtil.isPlayerOnline(request.challengerPlayer())) {
                        request.challengerPlayer().displayClientMessage(ChallengeLang.get("cobblemonchallenge.challenge.expired_to", request.challengedPlayer().getDisplayName().getString()), false);
                    }
                    requestIterator.remove();
                }
            }
            Iterator<Map.Entry<UUID, ChallengeCommand.LeadPokemonSelection>> selectionIterator = ChallengeCommand.ACTIVE_SELECTIONS.entrySet().iterator();
            while (selectionIterator.hasNext()) {
                LeadPokemonSelectionSession selectionSession = selectionIterator.next().getValue().selectionWrapper();
                selectionSession.doTick();
                if (selectionSession.creationTime + LeadPokemonSelectionSession.LEAD_TIMEOUT_MILLIS < nowTime) {
                    selectionSession.timeoutRequest();
                    selectionIterator.remove();
                }
            }

            Iterator<LeadPokemonSelectionSession> cancelSessions = LeadPokemonSelectionSession.SESSIONS_TO_CANCEL.iterator();
            while (cancelSessions.hasNext()) {
                LeadPokemonSelectionSession session = cancelSessions.next();
                ChallengeCommand.ACTIVE_SELECTIONS.remove(session.getUuid());
                cancelSessions.remove();
            }
        }
    }
}