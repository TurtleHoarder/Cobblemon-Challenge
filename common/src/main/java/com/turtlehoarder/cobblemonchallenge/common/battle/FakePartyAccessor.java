package com.turtlehoarder.cobblemonchallenge.common.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.jvm.functions.Function1;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class FakePartyAccessor implements Function1<ServerPlayer, PartyStore> {
    private List<Integer> player1Selection, player2Selection; // Correspond to slots clicked on the lead selection GUI
    private ChallengeFormat format;

    public FakePartyAccessor(List<Integer> player1Selection, List<Integer> player2Selection, ChallengeFormat format) {
        this.player1Selection = player1Selection;
        this.player2Selection = player2Selection;
        this.format = format;
    }

    @Override
    public PartyStore invoke(ServerPlayer serverPlayer) {
        PartyStore originalPartyStore = Cobblemon.INSTANCE.getStorage().getParty(serverPlayer);
        PlayerPartyStore challengeBattleStore = new PlayerPartyStore(serverPlayer.getUUID());
        int max = 2;
        int current = 0;
        for (Pokemon o : originalPartyStore) {
            if (current++ == max)
                break;
            challengeBattleStore.add(o);
        }
        return challengeBattleStore;
    }
}
