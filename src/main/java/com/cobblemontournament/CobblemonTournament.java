package com.cobblemontournament;

import com.cobblemontournament.config.TournamentConfig;
import com.cobblemontournament.testing.command.TournamentCommandTest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobblemonTournament implements ModInitializer
{
    public static String MODID = "cobblemontournament";
    public static final Logger LOGGER = LoggerFactory.getLogger("cobblemontournament");

    @Override
    public void onInitialize()
    {
        TournamentConfig.registerConfigs();

        // for testing
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) ->
                TournamentCommandTest.register(commandDispatcher));
    }
}
