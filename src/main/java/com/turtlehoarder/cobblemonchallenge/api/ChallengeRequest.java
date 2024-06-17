package com.turtlehoarder.cobblemonchallenge.api;

import net.minecraft.server.level.ServerPlayer;

public record ChallengeRequest (
        String id,
        ServerPlayer challengerPlayer,
        ServerPlayer challengedPlayer,
        ChallengeProperties properties,
        long createdTime
) {

}
