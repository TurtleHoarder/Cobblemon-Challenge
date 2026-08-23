package com.turtlehoarder.cobblemonchallenge.fabric.config;

import com.turtlehoarder.cobblemonchallenge.fabric.CobblemonChallengeFabric;
import com.mojang.datafixers.util.Pair;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;

public class ChallengeConfig {
    public static SimpleConfig CONFIG;
    private static ChallengeConfigProvider configs;
    public static Boolean CHALLENGE_DISTANCE_RESTRICTION;
    public static int MAX_CHALLENGE_DISTANCE;
    public static int DEFAULT_CHALLENGE_LEVEL;
    public static int DEFAULT_HANDICAP;
    public static int REQUEST_EXPIRATION_MILLIS;
    public static int CHALLENGE_COOLDOWN_MILLIS;
    public static String LANGUAGE;

    public static void registerConfigs() {
        CobblemonChallenge.LOGGER.info("Loading Challenge Configs");
        configs = new ChallengeConfigProvider();
        createConfigs();
        CONFIG = SimpleConfig.of(CobblemonChallenge.CONFIG_NAME).provider(configs).request();
        assignConfigs();
    }
    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("challengeDistanceRestriction", true));
        configs.addKeyValuePair(new Pair<>("maxChallengeDistance", 50));
        configs.addKeyValuePair(new Pair<>("defaultChallengeLevel", 100));
        configs.addKeyValuePair(new Pair<>("defaultHandicap", 0));
        configs.addKeyValuePair(new Pair<>("challengeExpirationTime", 60000));
        configs.addKeyValuePair(new Pair<>("challengeCooldownTime", 5000));
        configs.addKeyValuePair(new Pair<>("language", com.turtlehoarder.cobblemonchallenge.common.ChallengeLang.DEFAULT_LANGUAGE));
    }

    private static void assignConfigs() {
        CHALLENGE_COOLDOWN_MILLIS = CONFIG.getOrDefault("challengeCooldownTime", 5000);
        CHALLENGE_DISTANCE_RESTRICTION = CONFIG.getOrDefault("challengeDistanceRestriction", true);
        DEFAULT_CHALLENGE_LEVEL = CONFIG.getOrDefault("defaultChallengeLevel", 50);
        DEFAULT_HANDICAP = CONFIG.getOrDefault("defaultHandicap", 0);
        MAX_CHALLENGE_DISTANCE = CONFIG.getOrDefault("maxChallengeDistance", 50);
        REQUEST_EXPIRATION_MILLIS = CONFIG.getOrDefault("challengeExpirationTime", 60000);
        LANGUAGE = CONFIG.getOrDefault("language", com.turtlehoarder.cobblemonchallenge.common.ChallengeLang.DEFAULT_LANGUAGE);
    }
}