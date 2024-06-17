package com.cobblemontournament.config;

import com.cobblemontournament.CobblemonTournament;
import com.cobblemontournament.api.tournament.TournamentType;
import com.mojang.datafixers.util.Pair;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeFormat;
import com.turtlehoarder.cobblemonchallenge.config.ConfigProvider;
import com.turtlehoarder.cobblemonchallenge.config.SimpleConfig;

@SuppressWarnings("unused")
public final class TournamentConfig
{
    public static SimpleConfig CONFIG;
    private static ConfigProvider configs;

    public static TournamentType DEFAULT_TOURNAMENT_TYPE;
    public static int DEFAULT_GROUP_SIZE;
    public static int DEFAULT_MAX_PLAYER_COUNT;
    // challenge properties
    public static ChallengeFormat DEFAULT_CHALLENGE_FORMAT;
    public static int DEFAULT_CHALLENGE_MIN_LEVEL;
    public static int DEFAULT_CHALLENGE_MAX_LEVEL;
    public static boolean DEFAULT_SHOW_PREVIEW;

    public static void registerConfigs() {
        CobblemonTournament.LOGGER.info("Loading Tournament Configs");
        configs = new ConfigProvider();
        createConfigs();
        CONFIG = SimpleConfig.of(CobblemonTournament.MODID + "-config").provider(configs).request();
        assignConfigs();
    }
    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("defaultTournamentType", TournamentType.SingleElimination));
        configs.addKeyValuePair(new Pair<>("defaultGroupSize", 4));
        configs.addKeyValuePair(new Pair<>("defaultMaxPlayerCount", 32));
        configs.addKeyValuePair(new Pair<>("defaultChallengeFormat", ChallengeFormat.STANDARD_6V6));
        configs.addKeyValuePair(new Pair<>("defaultMinLevel", 50));
        configs.addKeyValuePair(new Pair<>("defaultMaxLevel", 50));
        configs.addKeyValuePair(new Pair<>("defaultShowPreview", true));
    }

    private static void assignConfigs() {
        DEFAULT_TOURNAMENT_TYPE = CONFIG.getOrDefault("defaultTournamentType", TournamentType.SingleElimination);
        DEFAULT_GROUP_SIZE = CONFIG.getOrDefault("defaultGroupSize", 4);
        DEFAULT_MAX_PLAYER_COUNT = CONFIG.getOrDefault("defaultMaxPlayerCount", 32);
        DEFAULT_CHALLENGE_FORMAT = CONFIG.getOrDefault("defaultChallengeFormat", ChallengeFormat.STANDARD_6V6);
        DEFAULT_CHALLENGE_MIN_LEVEL = CONFIG.getOrDefault("defaultChallengeMinLevel", 50);
        DEFAULT_CHALLENGE_MAX_LEVEL = CONFIG.getOrDefault("defaultChallengeMaxLevel", 50);
        DEFAULT_SHOW_PREVIEW = CONFIG.getOrDefault("", true);
    }
}
