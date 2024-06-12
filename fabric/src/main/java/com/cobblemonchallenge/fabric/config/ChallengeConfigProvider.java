package com.turtlehoarder.cobblemonchallenge.fabric.config;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
public class ChallengeConfigProvider implements SimpleConfig.DefaultConfig {

    private String configContents = "";
    private final List<Pair> configsList = new ArrayList<>();

    public void addKeyValuePair(Pair<String, ?> keyValuePair) {
        configsList.add(keyValuePair);
        configContents += keyValuePair.getFirst() + "=" + keyValuePair.getSecond() + "\n";
    }

    @Override
    public String get(String namespace) {
        return configContents;
    }
}
