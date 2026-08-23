package com.turtlehoarder.cobblemonchallenge.fabric;


import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.event.ChallengeEventHandler;
import com.turtlehoarder.cobblemonchallenge.fabric.config.*;
import com.turtlehoarder.cobblemonchallenge.common.*;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.fabricmc.api.ModInitializer;

public class CobblemonChallengeFabric extends CobblemonChallengeCommonInterface implements ModInitializer {

    public static String MODID = CobblemonChallenge.MODID;

    @Override
    public void onInitialize() {
        // Before the config is read: the file has to be at the new path by then.
        ConfigMigration.moveIntoModDirectory(getConfigDirectory(),
                CobblemonChallenge.MODID + "-config.properties",
                CobblemonChallenge.CONFIG_NAME + ".properties");
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
    public String getStringConfig(String configName) {
        return ChallengeConfig.CONFIG.get(configName);
    }

    @Override
    public java.nio.file.Path getConfigDirectory() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void registerEvents() {
        ChallengeEventHandler.registerEvents();
    }

    @Override
    public void registerCommands() {
        CobblemonChallenge.registerCommands();
    }
}