package com.turtlehoarder.cobblemonchallenge.neoforge;

import com.turtlehoarder.cobblemonchallenge.neoforge.config.ChallengeConfig;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallengeCommonInterface;
import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.event.ChallengeEventHandler;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.commons.lang3.tuple.Pair;

@Mod(CobblemonChallenge.MODID)
public class CobblemonChallengeForge extends CobblemonChallengeCommonInterface {
    private static ChallengeConfig config;
    private static ModConfigSpec commonSpec;

    static {
        final Pair<ChallengeConfig, ModConfigSpec> common = new ModConfigSpec.Builder().configure(ChallengeConfig::new);
        config = common.getLeft();
        commonSpec = common.getRight();

    }

    public CobblemonChallengeForge(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, commonSpec);
        NeoForge.EVENT_BUS.addListener(this::commands);
        container.getEventBus().addListener(this::serverInitialize);
    }

    public void serverInitialize(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CobblemonChallenge challenge = new CobblemonChallenge();
            challenge.initializeChallenge(this);
        });
    }

    public void commands(RegisterCommandsEvent e) {
        ChallengeCommand.register(e.getDispatcher());
    }

    @Override
    public int getIntConfig(String configName) {
        return config.getIntConfig(configName);
    }

    @Override
    public boolean getBooleanConfig(String configName) {
        return config.getBooleanConfig(configName);
    }

    @Override
    public void registerEvents() {
        ChallengeEventHandler.registerEvents();
        //DistExecutor.safeCallWhenOn(Dist.DEDICATED_SERVER, () -> ChallengeEventHandler::registerEvents);
    }

    @Override
    public void registerCommands() {
        CobblemonChallenge.registerCommands();
    }
}