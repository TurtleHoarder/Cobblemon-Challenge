package com.turtlehoarder.cobblemonchallenge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ChallengeConfig{

    public static ForgeConfigSpec.ConfigValue<Boolean> CHALLENGE_DISTANCE_RESTRICTION;
    public static ForgeConfigSpec.ConfigValue<Integer> MAX_CHALLENGE_DISTANCE;
    public static ForgeConfigSpec.ConfigValue<Integer> DEFAULT_CHALLENGE_LEVEL;
    public static ForgeConfigSpec.ConfigValue<Integer> DEFAULT_HANDICAP;
    public static ForgeConfigSpec.ConfigValue<Integer> REQUEST_EXPIRATION_MILLIS;
    public static ForgeConfigSpec.ConfigValue<Integer> CHALLENGE_COOLDOWN_MILLIS;

    public ChallengeConfig(ForgeConfigSpec.Builder builder){
        builder.push("cobblemonchallenge");
        CHALLENGE_DISTANCE_RESTRICTION = builder.comment("Set to false if you don't want a distance restriction on challenges").define("challengeDistanceRestriction", true);
        MAX_CHALLENGE_DISTANCE = builder.comment("Max distance of a challenge if challengeDistanceRestriction is set to true").define("maxChallengeDistance", 50);
        DEFAULT_CHALLENGE_LEVEL = builder.comment("The default level to set teams to if there is no challenge specified").define("defaultChallengeLevel", 50);
        DEFAULT_HANDICAP = builder.comment("The default handicap to set teams to if there is no handicap specified").define("defaultHandicap", 0);
        REQUEST_EXPIRATION_MILLIS = builder.comment("Time in millis before a challenge request expires").define("challengeExpirationTime", 60000);
        CHALLENGE_COOLDOWN_MILLIS = builder.comment("Time in millis before a player can send a consecutive challenge").define("challengeCooldownTime", 5000);
    }
}
