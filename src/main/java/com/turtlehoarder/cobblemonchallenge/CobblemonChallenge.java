package com.turtlehoarder.cobblemonchallenge;

import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.config.ChallengeConfig;
import com.turtlehoarder.cobblemonchallenge.config.UniversalDifficultyConfig;
import com.turtlehoarder.cobblemonchallenge.event.ChallengeEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CobblemonChallenge implements ModInitializer {

    public static String MODID = "cobblemonchallenge";
    public static final Logger LOGGER = LoggerFactory.getLogger("cobblemonchallenge");

    @Override
    public void onInitialize() {
        ChallengeConfig.registerConfigs();
        UniversalDifficultyConfig.registerConfigs();
        ChallengeEventHandler.registerEvents();
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> ChallengeCommand.register(commandDispatcher));
    }
}