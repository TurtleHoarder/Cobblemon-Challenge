package com.turtlehoarder.cobblemonchallenge.common.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.jvm.functions.Function1;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FakePartyAccessor implements Function1<ServerPlayer, PartyStore> {
    private List<Integer> player1Selection, player2Selection; // Correspond to slots clicked on the lead selection GUI
    private ServerPlayer player1;
    private ServerPlayer player2;
    private ChallengeFormat format;

    public FakePartyAccessor(List<Integer> player1Selection, List<Integer> player2Selection, ServerPlayer player1, ServerPlayer player2, ChallengeFormat format) {
        this.player1Selection = player1Selection;
        this.player2Selection = player2Selection;
        this.player1 = player1;
        this.player2 = player2;
        this.format = format;
    }

    private List<Integer> getSelectionForThisPlayer(ServerPlayer player) {
        if (player == player1) {
            return player1Selection;
        } else {
            return player2Selection;
        }
    }
    @Override
    public PartyStore invoke(ServerPlayer serverPlayer) {
        PartyStore originalPartyStore = Cobblemon.INSTANCE.getStorage().getParty(serverPlayer);
        PartyStore challengeBattleStore = new PartyStore(UUID.randomUUID());
        challengeBattleStore.setObserverUUIDs(List.of(serverPlayer.getUUID())); // Add observer so that the player gets updates
        Set<Integer> store = IntStream.rangeClosed(0, 5).boxed().collect(Collectors.toSet()); // Simple set from 0 to 5 in a fancy way
        List<Integer> slotSelection = getSelectionForThisPlayer(serverPlayer);
        for (int slotSelected : slotSelection) {
            challengeBattleStore.add(originalPartyStore.get(slotSelected).clone(true)); // New UUID for the fake party store, or else cloning shenanigans can happen
            store.remove(slotSelected);
        }
        // If the format requires more pokemon than selected, add the rest
        if (format.getTotalPokemonSlots() > format.getTotalPokemonSelected()) {
            for (int remainingSlot : store) {
                if (originalPartyStore.get(remainingSlot) != null)
                    challengeBattleStore.add(originalPartyStore.get(remainingSlot).clone(true)); // New UUID for the fake party store, or else cloning shenanigans can happen
            }
        }

        return challengeBattleStore;
    }

}
