package com.turtlehoarder.cobblemonchallenge.common;

public abstract class CobblemonChallengeCommonInterface {

    public abstract int getIntConfig(String configName);
    public abstract boolean getBooleanConfig(String configName);
    public abstract String getStringConfig(String configName);

    /** Where this platform keeps its config files, so the language files sit next to them. */
    public abstract java.nio.file.Path getConfigDirectory();

    public abstract void registerEvents();

    public abstract void registerCommands();

}