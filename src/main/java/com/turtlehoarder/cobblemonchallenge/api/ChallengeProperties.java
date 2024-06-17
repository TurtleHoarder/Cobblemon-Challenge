package com.turtlehoarder.cobblemonchallenge.api;

import com.turtlehoarder.cobblemonchallenge.battle.ChallengeFormat;
import com.turtlehoarder.cobblemonchallenge.config.ChallengeConfig;
import org.jetbrains.annotations.Nullable;

public final class ChallengeProperties
{
    private static final ChallengeFormat DEFAULT_CHALLENGE_FORMAT = ChallengeConfig.DEFAULT_CHALLENGE_FORMAT;
    private static final int DEFAULT_CHALLENGE_LEVEL = ChallengeConfig.DEFAULT_CHALLENGE_LEVEL;
    private static final int DEFAULT_HANDICAP = ChallengeConfig.DEFAULT_HANDICAP;
    private static final Boolean DEFAULT_SHOW_PREVIEW = ChallengeConfig.DEFAULT_SHOW_PREVIEW;

    public ChallengeProperties(@Nullable ChallengeFormat format, Integer minLvl, Integer maxLvl, Integer handicapPlayer1, Integer handicapPlayer2, Boolean preview) {
        _challengeFormat = format != null ? format : DEFAULT_CHALLENGE_FORMAT;
        _maxLevel = clampIntNotNull(maxLvl, DEFAULT_CHALLENGE_LEVEL,0,100);
        _minLevel = clampIntNotNull(maxLvl,Math.min(DEFAULT_CHALLENGE_LEVEL,_maxLevel),0,Math.min(100,_maxLevel));
        _handicapP1 = handicapPlayer1 != null ? handicapPlayer1 : DEFAULT_HANDICAP;
        _handicapP2 = handicapPlayer2 != null ? handicapPlayer2 : DEFAULT_HANDICAP;
        _showPreview = preview != null ? preview : DEFAULT_SHOW_PREVIEW;
    }

    private final ChallengeFormat _challengeFormat;
    private final int _minLevel;
    private final int _maxLevel;
    private final int _handicapP1;
    private final int _handicapP2;
    private final boolean _showPreview;

    public ChallengeFormat getChallengeFormat() { return _challengeFormat; }
    public int getMinLevel() { return _minLevel; }
    public int getMaxLevel() { return _maxLevel; }
    public int getHandicapP1() { return _handicapP1; }
    public int getHandicapP2() { return _handicapP2; }
    public boolean getShowPreview() { return _showPreview; }

    private int clampIntNotNull(Integer value,int defaultValue,int min, int max) {
        if (value == null) {
            return defaultValue;
        } else if (value > max){
            return max;
        } else if(value < min){
            return min;
        }
        return value;
    }
}
