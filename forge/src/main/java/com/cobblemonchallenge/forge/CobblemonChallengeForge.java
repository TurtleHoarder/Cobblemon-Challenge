package com.cobblemonchallenge.forge;

import com.cobblemonchallenge.forge.config.ChallengeConfig;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallengeCommonInterface;
import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.common.event.ChallengeEventHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

@Mod(CobblemonChallenge.MODID)
public class CobblemonChallengeForge extends CobblemonChallengeCommonInterface {
    private static ChallengeConfig config;
    private static ForgeConfigSpec commonSpec;

    static {
        final Pair<ChallengeConfig, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(ChallengeConfig::new);
        config = common.getLeft();
        commonSpec = common.getRight();
    }

    public CobblemonChallengeForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, commonSpec);
        CobblemonChallenge challenge = new CobblemonChallenge();
        DistExecutor.safeCallWhenOn(Dist.DEDICATED_SERVER, () -> {
            System.out.println("Doing Safe Call");
            challenge.initializeChallenge(this);
            return null;
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
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }
}