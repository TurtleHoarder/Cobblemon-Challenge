package com.turtlehoarder.cobblemonchallenge.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.*;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;

import com.turtlehoarder.cobblemonchallenge.battle.ChallengeBattleBuilder;
import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.config.ChallengeConfig;
import com.turtlehoarder.cobblemonchallenge.util.ChallengeUtil;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Level;

import java.util.*;

public class ChallengeEventHandler {
    /*
        Since this plugin uses cloned pokemon in its battles, there will be a *cloned* pokemon left behind after the battle is complete. These
        events ensure that these cloned entities are tracked and removed when a battle ends via Victory, disconnect, or server shutdown
     */
    private static void registerPostVictoryEvent() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, (battleVictoryEvent) -> {
            CobblemonChallenge.LOGGER.info("Battle victory!");
            UUID battleId = battleVictoryEvent.getBattle().getBattleId();
            Iterator<PokemonEntity> clonedPokemonIterator = ChallengeBattleBuilder.clonedPokemonList.iterator();
            // remove cloned pokemon associated with battle
            while (clonedPokemonIterator.hasNext()) {
                PokemonEntity clonedPokemon = clonedPokemonIterator.next();
                if (clonedPokemon.getBattleId().get().isPresent() && clonedPokemon.getBattleId().get().get().equals(battleId)) {
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

    public static boolean registerCobblemonEvents() {
        registerPostVictoryEvent();
        registerChallengeLootPrevention();
        return true;
    }

    // When a player leaves, check to see if they were part of any challenges. If they are, remove all cloned pokemon that are associated with this challenge. This will prevent duplicate mons from remaining behind
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
        Iterator<PokemonBattle> battleIterator = ChallengeBattleBuilder.challengeBattles.iterator();
        while (battleIterator.hasNext()) {
            PokemonBattle battle = battleIterator.next();
            if (battle.getPlayers().contains(serverPlayer)) {
                CobblemonChallenge.LOGGER.debug(String.format("Found hanging battle! (%s)", battle.getBattleId()));
                Iterator<PokemonEntity> clonedPokemonIterator = ChallengeBattleBuilder.clonedPokemonList.iterator();
                while (clonedPokemonIterator.hasNext()) {
                    PokemonEntity clonedPokemon = clonedPokemonIterator.next();
                    if (clonedPokemon.getBattleId().get().isPresent() && clonedPokemon.getBattleId().get().get().equals(battle.getBattleId())) {
                        CobblemonChallenge.LOGGER.debug(String.format("Removing cloned pokemon from battle: %s | Battle id %s", clonedPokemon.getDisplayName().getString(), clonedPokemon.getBattleId().get().get()));
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
    @SubscribeEvent
    public static void checkSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof PokemonEntity pokemonEntity) {
            if (ChallengeUtil.isPokemonPartOfChallenge(pokemonEntity)) {
                CobblemonChallenge.LOGGER.debug(String.format("Entity Joined already in battle: %s | Battle id %s", event.getEntity().getDisplayName().getString(), pokemonEntity.getBattleId().get().get()));
                ChallengeBattleBuilder.clonedPokemonList.add(pokemonEntity);
                // Trick Cobblemon into thinking the clones are *not* wild pokemon. This will prevent duplicates being caught if something unexpected happens to the battle, like /stopbattle or a server crash
                pokemonEntity.getPokemon().getStoreCoordinates().set(new StoreCoordinates<BottomlessPosition>(new BottomlessStore(new UUID(0,0)), new BottomlessPosition(0)));
                pokemonEntity.getBusyLocks().add("Cloned_Pokemon"); // Busy lock prevents others from interacting with cloned pokemon
            }
        }
    }

    @SubscribeEvent
    public static void onServerShutdown(ServerStoppingEvent event) {
        CobblemonChallenge.LOGGER.debug( "Performing Server Shutdown tasks for Cobblemon Challenge");
        if (!ChallengeBattleBuilder.clonedPokemonList.isEmpty()) {
            CobblemonChallenge.LOGGER.debug(String.format("Cloned pokemon (%d) from challenges detected. Removing all before server shuts down", ChallengeBattleBuilder.clonedPokemonList.size()));
            ArrayList<PokemonEntity> clonedPokemonCopyList = new ArrayList<PokemonEntity>(ChallengeBattleBuilder.clonedPokemonList); // Create a copy since other list may be altered by despawn events
            clonedPokemonCopyList.forEach(pokemonEntity -> pokemonEntity.remove(Entity.RemovalReason.DISCARDED));
            clonedPokemonCopyList.clear();
        }
        CobblemonChallenge.LOGGER.debug("Finished performing Server Shutdown tasks for Cobblemon Challenge");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent tickEvent) {
        if (tickEvent.getServer().getTickCount() % 20 == 0) {
            long nowTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, ChallengeCommand.ChallengeRequest>> requestIterator = ChallengeCommand.CHALLENGE_REQUESTS.entrySet().iterator();
            while (requestIterator.hasNext()) {
                Map.Entry<String, ChallengeCommand.ChallengeRequest> requestMap = requestIterator.next();
                ChallengeCommand.ChallengeRequest request = requestMap.getValue();
                if (request.createdTime() + ChallengeConfig.REQUEST_EXPIRATION_MILLIS.get() < nowTime) {
                    if (ChallengeUtil.isPlayerOnline(request.challengedPlayer())) {
                        request.challengedPlayer().displayClientMessage(Component.literal(ChatFormatting.RED + String.format("Challenge from %s has expired", request.challengerPlayer().getDisplayName().getString())), false);
                    }
                    if (ChallengeUtil.isPlayerOnline(request.challengerPlayer())) {
                        request.challengerPlayer().displayClientMessage(Component.literal(ChatFormatting.RED + String.format("Challenge to %s has expired", request.challengedPlayer().getDisplayName().getString())), false);
                    }
                    requestIterator.remove();
                }
            }
        }
        // Once per 30 seconds, check for hanging cloned pokemon that are no longer part of a battle and remove them
        if (tickEvent.getServer().getTickCount() % 600 == 0) {
            Iterator<PokemonEntity> clonedPokemonIterator = ChallengeBattleBuilder.clonedPokemonList.iterator();
           while (clonedPokemonIterator.hasNext()) {
                PokemonEntity pokemonEntity = clonedPokemonIterator.next();
                if (pokemonEntity.getBattleId().get().isEmpty()) {
                    pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
                    clonedPokemonIterator.remove();
                    CobblemonChallenge.LOGGER.debug(String.format("Removed hanging duplicate pokemon %s", pokemonEntity.getDisplayName().getString()));
                }
            }
        }
    }

}