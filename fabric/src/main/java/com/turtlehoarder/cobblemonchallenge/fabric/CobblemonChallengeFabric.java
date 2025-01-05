package com.cobblemonchallenge.fabric;


import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.event.ChallengeEventHandler;
import com.cobblemonchallenge.fabric.config.*;
import com.turtlehoarder.cobblemonchallenge.common.*;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.fabricmc.api.ModInitializer;

public class CobblemonChallengeFabric extends CobblemonChallengeCommonInterface implements ModInitializer {

    public static String MODID = CobblemonChallenge.MODID;

    @Override
    public void onInitialize() {
        ChallengeConfig.registerConfigs();
        CobblemonChallenge challenge = new CobblemonChallenge();
        challenge.initializeChallenge(this);
    }

    @Override
    public int getIntConfig(String configName) {
        return Integer.parseInt(ChallengeConfig.CONFIG.get(configName));
    }

    @Override
    public boolean getBooleanConfig(String configName) {
        return Boolean.parseBoolean(ChallengeConfig.CONFIG.get(configName));
    }

    @Override
    public void registerEvents() {
        ChallengeEventHandler.registerEvents();
    }

    @Override
    public void registerCommands() {
        CommandRegistrationEvent.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> ChallengeCommand.register(commandDispatcher));
    }
}