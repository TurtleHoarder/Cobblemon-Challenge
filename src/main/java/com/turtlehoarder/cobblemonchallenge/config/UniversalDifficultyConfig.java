package com.turtlehoarder.cobblemonchallenge.config;

import com.turtlehoarder.cobblemonchallenge.CobblemonChallenge;
import com.mojang.datafixers.util.Pair;

// temp class for possible configs while testing new features
public class UniversalDifficultyConfig {

    public static SimpleConfig CONFIG;
    private static ConfigProvider configs;
    public static Boolean USE_UNIVERSAL_LEVEL;
    public static Boolean USE_UNIVERSAL_LEVEL_RANGE;
    public static Boolean USE_UNIVERSAL_HANDICAP;
    public static int DEFAULT_UNIVERSAL_LEVEL;
    public static int DEFAULT_UNIVERSAL_MIN_LEVEL;
    public static int DEFAULT_UNIVERSAL_MAX_LEVEL;
    public static int DEFAULT_UNIVERSAL_HANDICAP;
    public static Boolean DEFAULT_SHOW_PREVIEW;

    public static void registerConfigs() {
        CobblemonChallenge.LOGGER.info("Loading Universal Difficulty Configs");
        configs = new ConfigProvider();
        createConfigs();
        CONFIG = SimpleConfig.of(CobblemonChallenge.MODID + "-universal-difficulty-config").provider(configs).request();
        assignConfigs();
    }
    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("useUniversalLevel", false));
        configs.addKeyValuePair(new Pair<>("useUniversalLevelRange", true));
        configs.addKeyValuePair(new Pair<>("useUniversalHandicap", true));
        configs.addKeyValuePair(new Pair<>("defaultUniversalLevel", 50));
        configs.addKeyValuePair(new Pair<>("defaultUniversalMinLevel", 50));
        configs.addKeyValuePair(new Pair<>("defaultUniversalMinLevel", 0));
        configs.addKeyValuePair(new Pair<>("defaultUniversalHandicap", 0));
        configs.addKeyValuePair(new Pair<>("defaultShowPreview", true));
    }

    private static void assignConfigs() {
        USE_UNIVERSAL_LEVEL = CONFIG.getOrDefault("useUniversalLevel", false);
        USE_UNIVERSAL_LEVEL_RANGE = CONFIG.getOrDefault("useUniversalLevelRange", true);
        USE_UNIVERSAL_HANDICAP = CONFIG.getOrDefault("useUniversalHandicap", true);
        DEFAULT_UNIVERSAL_LEVEL = CONFIG.getOrDefault("defaultUniversalLevel", 50);
        DEFAULT_UNIVERSAL_MIN_LEVEL = CONFIG.getOrDefault("defaultUniversalMinLevel", 50);
        DEFAULT_UNIVERSAL_MAX_LEVEL = CONFIG.getOrDefault("defaultUniversalMinLevel", 0);
        DEFAULT_UNIVERSAL_HANDICAP = CONFIG.getOrDefault("defaultUniversalHandicap", 0);
        DEFAULT_SHOW_PREVIEW = CONFIG.getOrDefault("defaultShowPreview", true);
    }
}
