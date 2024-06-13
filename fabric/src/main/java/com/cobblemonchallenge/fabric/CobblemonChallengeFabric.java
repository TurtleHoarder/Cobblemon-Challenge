package com.cobblemonchallenge.fabric;


import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.event.ChallengeEventHandler;
import com.cobblemonchallenge.fabric.config.*;
import com.turtlehoarder.cobblemonchallenge.common.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class CobblemonChallengeFabric extends CobblemonChallengeCommonInterface implements ModInitializer {

    public static String MODID = "cobblemonchallenge";
    @Override
    public void onInitialize() {
        CobblemonChallenge challenge = new CobblemonChallenge();
        challenge.initializeChallenge(this);
        ChallengeConfig.registerConfigs();
        ChallengeEventHandler.registerEvents();
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> ChallengeCommand.register(commandDispatcher));
    }
}