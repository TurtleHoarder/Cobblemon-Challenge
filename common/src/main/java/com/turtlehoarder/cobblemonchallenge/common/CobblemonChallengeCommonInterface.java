package com.turtlehoarder.cobblemonchallenge.common;

public abstract class CobblemonChallengeCommonInterface {

    public abstract int getIntConfig(String configName);
    public abstract boolean getBooleanConfig(String configName);

    public abstract void registerEvents();

    public abstract void registerCommands();

}
