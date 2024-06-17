package com.turtlehoarder.cobblemonchallenge.event;

import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.api.ChallengeRequest;
import com.turtlehoarder.cobblemonchallenge.api.LeadPokemonSelection;
import com.turtlehoarder.cobblemonchallenge.api.storage.FakePokemonStore;
import com.turtlehoarder.cobblemonchallenge.api.storage.party.FakePartyPosition;
import com.turtlehoarder.cobblemonchallenge.api.storage.party.FakePlayerPartyStore;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.config.ChallengeConfig;
import com.turtlehoarder.cobblemonchallenge.gui.LeadPokemonSelectionSession;
import com.turtlehoarder.cobblemonchallenge.util.ChallengeUtil;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.*;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyReferencePacket;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;
import kotlin.Unit;

public class ChallengeEventHandler {

    public static void registerEvents() {
        registerPostVictoryEvent();
        registerChallengeLootPrevention();
        registerCobblemonSavePrevention();
        ServerEntityEvents.ENTITY_LOAD.register((entity, server) -> checkSpawn(entity));
        ServerPlayConnectionEvents.DISCONNECT.register((event, server) -> onPlayerLoggedOut(event.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> onServerShutdown());
        ServerTickEvents.END_SERVER_TICK.register(ChallengeEventHandler::onServerTick);
    }

    /*
        Since this plugin uses cloned pokemon in its battles, there will be a *cloned* pokemon left behind after the battle is complete. These
        events ensure that these cloned entities are tracked and removed when a battle ends via Victory, disconnect, or server shutdown
     */
    public static void registerPostVictoryEvent() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, (battleVictoryEvent) -> {
            CobblemonChallenge.LOGGER.debug("Battle victory!");
            UUID battleId = battleVictoryEvent.getBattle().getBattleId();
            Iterator<PokemonEntity> clonedPokemonIterator = ChallengeBattleBuilder.clonedPokemonList.iterator();
            // remove cloned pokemon associated with battle
            while (clonedPokemonIterator.hasNext()) {
                PokemonEntity clonedPokemon = clonedPokemonIterator.next();
                if (clonedPokemon.isBattling() && clonedPokemon.getBattleId() != null && clonedPokemon.getBattleId().equals(battleId)) {
                    clonedPokemon.remove(Entity.RemovalReason.DISCARDED);
                    clonedPokemonIterator.remove();
                    CobblemonChallenge.LOGGER.debug(String.format("Removing cloned pokemon from battle: %s", clonedPokemon.getDisplayName().getString()));
                }
            }
            // Send victory message to victor
            if (ChallengeUtil.isBattleChallenge(battleId)) {
                PokemonBattle battle = battleVictoryEvent.getBattle();
                List<ServerPlayer> participants = new ArrayList<>(battle.getPlayers());
                Iterator<ServerPlayer> participantIterator = participants.listIterator();
                while (participantIterator.hasNext()) {
                    ServerPlayer player = participantIterator.next();
                    /* Bug fix for stuck cobblemon input after battles. Since clients are switching to cloned pokemon UUIDs, their selected slot will be -1. By sending the party reference packet to them, we can reset this position */
                    UUID partyuuid = Cobblemon.INSTANCE.getStorage().getParty(player).getUuid();
                    CobblemonNetwork.INSTANCE.sendPacketToPlayer(player, new SetPartyReferencePacket(partyuuid));
                    for (BattleActor actor : battleVictoryEvent.getWinners()) {
                        actor.getPlayerUUIDs().forEach(winnerUUID -> {
                            if (player.getUUID().equals(winnerUUID)) {
                                player.displayClientMessage(Component.literal(ChatFormatting.GREEN + "You have won the challenge!"), false);
                                participantIterator.remove();
                            }
                        });
                    }
                }
                // Remaining participants have lost
                participants.forEach(loser -> loser.displayClientMessage(Component.literal(ChatFormatting.RED + "You have lost the challenge"), false));
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

    private static void registerChallengeLootPrevention() {
        CobblemonEvents.LOOT_DROPPED.subscribe(Priority.HIGHEST, (lootDroppedEvent) -> {
            if (lootDroppedEvent.getEntity() instanceof PokemonEntity pokemonEntity) {
                if (ChallengeUtil.isPokemonPartOfChallenge(pokemonEntity)) {
                    CobblemonChallenge.LOGGER.debug(String.format(String.format("Prevented drop from cloned pokemon: %s", pokemonEntity.getDisplayName().getString()), ChallengeBattleBuilder.clonedPokemonList.size()));
                    lootDroppedEvent.cancel(); // Cancel loot dropped event if it's part of a challenge
                }
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
                Iterator<PokemonEntity> clonedPokemonIterator = ChallengeBattleBuilder.clonedPokemonList.iterator();
                while (clonedPokemonIterator.hasNext()) {
                    PokemonEntity clonedPokemon = clonedPokemonIterator.next();
                    if (clonedPokemon.getBattleId() != null && clonedPokemon.getBattleId().equals(battle.getBattleId())) {
                        CobblemonChallenge.LOGGER.debug(String.format("Removing cloned pokemon from battle: %s | Battle id %s", clonedPokemon.getDisplayName().getString(), clonedPokemon.getBattleId()));
                        clonedPokemon.remove(Entity.RemovalReason.DISCARDED); // This will call despawnPokemon event and remove it from the list
                        clonedPokemonIterator.remove();
                    } else {
                        CobblemonChallenge.LOGGER.debug(String.format("Removing cloned pokemon from world that no longer has battle id: %s", clonedPokemon.getDisplayName().getString()));
                        clonedPokemon.remove(Entity.RemovalReason.DISCARDED); // This will call despawnPokemon event and remove it from the list
                        clonedPokemonIterator.remove();
                    }
                }
                battleIterator.remove(); // Remove hanging battle from list
            }
        }
    }

    // To keep track of cloned pokemon, check to see if they have a battle id matching that of a Challenge upon spawning.
    public static void checkSpawn(Entity entity) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            if (ChallengeUtil.isPokemonPartOfChallenge(pokemonEntity)) {
                CobblemonChallenge.LOGGER.debug(String.format("Entity Joined already in battle: %s | Battle id %s", entity.getDisplayName().getString(), pokemonEntity.getBattleId()));
                ChallengeBattleBuilder.clonedPokemonList.add(pokemonEntity);
                // Trick Cobblemon into thinking the clones are *not* wild pokemon. This will prevent duplicates being caught if something unexpected happens to the battle, like /stopbattle or a server crash
                PokemonBattle pb = ChallengeUtil.getAssociatedBattle(pokemonEntity);
                if (pb != null) {
                    UUID foundplayerUUID = ChallengeUtil.getOwnerUuidOfClonedPokemon(pb, pokemonEntity);
                    UUID playerUUID = (foundplayerUUID != null ? foundplayerUUID : new UUID(0,0));
                    FakePlayerPartyStore fakePlayerStore = new FakePlayerPartyStore(playerUUID);
                    PokemonStore<StorePosition> fakePokemonStore = new FakePokemonStore<FakePartyPosition>(fakePlayerStore,playerUUID);
                    pokemonEntity.getPokemon().getStoreCoordinates().set(new StoreCoordinates<>(fakePokemonStore, new FakePartyPosition()));
                    pokemonEntity.getBusyLocks().add("Cloned_Pokemon"); // Busy lock prevents others from interacting with cloned pokemon
                }
            }
        }
    }

    public static void onServerShutdown() {
        CobblemonChallenge.LOGGER.debug("Performing Server Shutdown tasks for Cobblemon Challenge");
        if (!ChallengeBattleBuilder.clonedPokemonList.isEmpty()) {
            CobblemonChallenge.LOGGER.debug(String.format("Cloned pokemon (%d) from challenges detected. Removing all before server shuts down", ChallengeBattleBuilder.clonedPokemonList.size()));
            ArrayList<PokemonEntity> clonedPokemonCopyList = new ArrayList<PokemonEntity>(ChallengeBattleBuilder.clonedPokemonList); // Create a copy since other list may be altered by despawn events
            clonedPokemonCopyList.forEach(pokemonEntity -> pokemonEntity.remove(Entity.RemovalReason.DISCARDED));
            clonedPokemonCopyList.clear();
        }
        CobblemonChallenge.LOGGER.debug("Finished performing Server Shutdown tasks for Cobblemon Challenge");
    }

    public static void onServerTick(MinecraftServer server) {
        int tickCount = server.getTickCount();
        if (tickCount % 20 == 0) {
            long nowTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, ChallengeRequest>> requestIterator = ChallengeCommand.CHALLENGE_REQUESTS.entrySet().iterator();
            while (requestIterator.hasNext()) {
                Map.Entry<String, ChallengeRequest> requestMap = requestIterator.next();
                ChallengeRequest request = requestMap.getValue();
                if (request.createdTime() + ChallengeConfig.REQUEST_EXPIRATION_MILLIS < nowTime) {
                    if (ChallengeUtil.isPlayerOnline(request.challengedPlayer())) {
                        request.challengedPlayer().displayClientMessage(Component.literal(ChatFormatting.RED + String.format("Challenge from %s has expired", request.challengerPlayer().getDisplayName().getString())), false);
                    }
                    if (ChallengeUtil.isPlayerOnline(request.challengerPlayer())) {
                        request.challengerPlayer().displayClientMessage(Component.literal(ChatFormatting.RED + String.format("Challenge to %s has expired", request.challengedPlayer().getDisplayName().getString())), false);
                    }
                    requestIterator.remove();
                }
            }
            Iterator<Map.Entry<UUID, LeadPokemonSelection>> selectionIterator = ChallengeCommand.ACTIVE_SELECTIONS.entrySet().iterator();
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
                CobblemonChallenge.LOGGER.info(String.format("Removing hanging session. Size remaining: %d | %d", ChallengeCommand.ACTIVE_SELECTIONS.size(), LeadPokemonSelectionSession.SESSIONS_TO_CANCEL.size()));
                cancelSessions.remove();
            }
        }
        // Once per 30 seconds, check for hanging cloned pokemon that are no longer part of a battle and remove them
        if (tickCount % 600 == 0) {
            Iterator<PokemonEntity> clonedPokemonIterator = ChallengeBattleBuilder.clonedPokemonList.iterator();
            while (clonedPokemonIterator.hasNext()) {
                PokemonEntity pokemonEntity = clonedPokemonIterator.next();
                if (pokemonEntity.getBattleId() == null) {
                    pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
                    clonedPokemonIterator.remove();
                    CobblemonChallenge.LOGGER.debug(String.format("Removed hanging duplicate pokemon %s", pokemonEntity.getDisplayName().getString()));
                }
            }
        }
    }
}