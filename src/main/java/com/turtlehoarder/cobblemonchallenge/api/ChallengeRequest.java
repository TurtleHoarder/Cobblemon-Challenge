package com.turtlehoarder.cobblemonchallenge.api;

import net.minecraft.server.level.ServerPlayer;

public record ChallengeRequest (
        String id,
        ServerPlayer challengerPlayer,
        ServerPlayer challengedPlayer,
        int minLevel,
        int maxLevel,
        int handicapP1,
        int handicapP2,
        boolean preview,
        long createdTime
) {

}
