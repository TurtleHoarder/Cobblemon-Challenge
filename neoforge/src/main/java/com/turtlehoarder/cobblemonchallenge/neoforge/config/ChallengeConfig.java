package com.turtlehoarder.cobblemonchallenge.neoforge.config;

import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;

public class ChallengeConfig {

    public static ModConfigSpec.ConfigValue<Boolean> CHALLENGE_DISTANCE_RESTRICTION;
    public static ModConfigSpec.ConfigValue<Integer> MAX_CHALLENGE_DISTANCE;
    public static ModConfigSpec.ConfigValue<Integer> DEFAULT_CHALLENGE_LEVEL;
    public static ModConfigSpec.ConfigValue<Integer> REQUEST_EXPIRATION_MILLIS;
    public static ModConfigSpec.ConfigValue<Integer> CHALLENGE_COOLDOWN_MILLIS;
    public static ModConfigSpec.ConfigValue<Integer> DEFAULT_HANDICAP_LEVEL;
    public static ModConfigSpec.ConfigValue<String> LANGUAGE;

    public HashMap<String, ModConfigSpec.ConfigValue> configMap = new HashMap<>();

    public ChallengeConfig(ModConfigSpec.Builder builder){
        builder.push("cobblemonchallenge");
        CHALLENGE_DISTANCE_RESTRICTION = builder.comment("Set to false if you don't want a distance restriction on challenges").define("challengeDistanceRestriction", true);
        MAX_CHALLENGE_DISTANCE = builder.comment("Max distance of a challenge if challengeDistanceRestriction is set to true").define("maxChallengeDistance", 50);
        DEFAULT_CHALLENGE_LEVEL = builder.comment("The default level to set teams to if there is no challenge specified").define("defaultChallengeLevel", 100);
        REQUEST_EXPIRATION_MILLIS = builder.comment("Time in millis before a challenge request expires").define("challengeExpirationTime", 60000);
        CHALLENGE_COOLDOWN_MILLIS = builder.comment("Time in millis before a player can send a consecutive challenge").define("challengeCooldownTime", 5000);
        DEFAULT_HANDICAP_LEVEL = builder.comment("Default Handicap in levels of handicapped battles.").define("defaultHandicap", 0);
        LANGUAGE = builder.comment("Language file to read messages from, in config/cobblemonchallenge/lang/. Falls back to en_us.").define("language", com.turtlehoarder.cobblemonchallenge.common.ChallengeLang.DEFAULT_LANGUAGE);

        configMap.put(CobblemonChallenge.CHALLENGE_DISTANCE_CONFIG_NAME, CHALLENGE_DISTANCE_RESTRICTION);
        configMap.put(CobblemonChallenge.MAX_CHALLENGE_DISTANCE_CONFIG_NAME, MAX_CHALLENGE_DISTANCE);
        configMap.put(CobblemonChallenge.DEFAULT_CHALLENGE_LEVEL_CONFIG_NAME, DEFAULT_CHALLENGE_LEVEL);
        configMap.put(CobblemonChallenge.CHALLENGE_EXPIRATION_TIME_CONFIG_NAME, REQUEST_EXPIRATION_MILLIS);
        configMap.put(CobblemonChallenge.CHALLENGE_COOLDOWN_CONFIG_NAME, CHALLENGE_COOLDOWN_MILLIS);
        configMap.put(CobblemonChallenge.DEFAULT_HANDICAP_CONFIG_NAME, DEFAULT_HANDICAP_LEVEL);
        configMap.put(CobblemonChallenge.LANGUAGE_CONFIG_NAME, LANGUAGE);
    }

    public int getIntConfig(String name) {
        return Integer.parseInt(configMap.get(name).get().toString());
    }

    public boolean getBooleanConfig(String name) {
        return Boolean.parseBoolean(configMap.get(name).get().toString());
    }

    public String getStringConfig(String name) {
        return configMap.get(name).get().toString();
    }



}