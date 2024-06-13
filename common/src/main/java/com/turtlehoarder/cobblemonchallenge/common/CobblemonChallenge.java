package com.turtlehoarder.cobblemonchallenge.common;

import com.mojang.datafixers.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobblemonChallenge {

    public static final Logger LOGGER = LoggerFactory.getLogger("cobblemonchallenge");
    public static final String MODID = "cobblemonchallenge";

    public static Boolean CHALLENGE_DISTANCE_RESTRICTION;
    public static int MAX_CHALLENGE_DISTANCE;
    public static int DEFAULT_CHALLENGE_LEVEL;
    public static int DEFAULT_HANDICAP;
    public static int REQUEST_EXPIRATION_MILLIS;
    public static int CHALLENGE_COOLDOWN_MILLIS;

    private static final String CHALLENGE_DISTANCE_CONFIG_NAME = "challengeDistanceRestriction";
    private static final String MAX_CHALLENGE_DISTANCE_CONFIG_NAME = "maxChallengeDistance";
    private static final String DEFAULT_CHALLENGE_LEVEL_CONFIG_NAME = "defaultChallengeLevel";
    private static final String DEFAULT_HANDICAP_CONFIG_NAME = "defaultHandicap";
    private static final String CHALLENGE_EXPIRATION_TIME_CONFIG_NAME = "challengeExpirationTime";
    private static final String CHALLENGE_COOLDOWN_CONFIG_NAME = "challengeCooldownTime";
    private CobblemonChallengeCommonInterface implementation;
    public CobblemonChallenge() {

    }

    public void initializeChallenge(CobblemonChallengeCommonInterface implementation) {
        this.implementation = implementation;
        CHALLENGE_DISTANCE_RESTRICTION = implementation.getBooleanConfig(CHALLENGE_DISTANCE_CONFIG_NAME);
        MAX_CHALLENGE_DISTANCE = implementation.getIntConfig(MAX_CHALLENGE_DISTANCE_CONFIG_NAME);
        DEFAULT_CHALLENGE_LEVEL = implementation.getIntConfig(DEFAULT_CHALLENGE_LEVEL_CONFIG_NAME);
        DEFAULT_HANDICAP = implementation.getIntConfig(DEFAULT_HANDICAP_CONFIG_NAME);
        REQUEST_EXPIRATION_MILLIS = implementation.getIntConfig(CHALLENGE_EXPIRATION_TIME_CONFIG_NAME);
        CHALLENGE_COOLDOWN_MILLIS = implementation.getIntConfig(CHALLENGE_COOLDOWN_CONFIG_NAME);

        implementation.registerCommands();
        implementation.registerEvents();
    }


}
