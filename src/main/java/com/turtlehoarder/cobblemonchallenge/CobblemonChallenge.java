package com.turtlehoarder.cobblemonchallenge;

import com.turtlehoarder.cobblemonchallenge.command.ChallengeCommand;
import com.turtlehoarder.cobblemonchallenge.config.ChallengeConfig;
import com.turtlehoarder.cobblemonchallenge.event.ChallengeEventHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CobblemonChallenge.MOD_ID)
public class CobblemonChallenge {
    public static final String MOD_ID = "cobblemonchallenge";
    public static final Logger LOGGER = LogManager.getLogger();
    public static CobblemonChallenge INSTANCE;
    private static ChallengeConfig config;
    private static ForgeConfigSpec commonSpec;

    static {
        final Pair<ChallengeConfig, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(ChallengeConfig::new);
        config = common.getLeft();
        commonSpec = common.getRight();
    }

    public CobblemonChallenge() {
        INSTANCE = this;
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, commonSpec);
        MinecraftForge.EVENT_BUS.register(ChallengeEventHandler.class);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        DistExecutor.safeCallWhenOn(Dist.DEDICATED_SERVER, () -> ChallengeEventHandler::registerCobblemonEvents);
    }

    public void commands(RegisterCommandsEvent e) {
        ChallengeCommand.register(e.getDispatcher());
    }
}