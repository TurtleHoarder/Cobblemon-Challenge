package com.cobblemontournament.api.tournament;

import com.cobblemontournament.config.TournamentConfig;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeFormat;
import org.jetbrains.annotations.NotNull;

@NotNull
public final class TournamentProperties
{
    public static final TournamentType DEFAULT_TOURNAMENT_TYPE = TournamentConfig.DEFAULT_TOURNAMENT_TYPE;
    public static final int DEFAULT_GROUP_SIZE = TournamentConfig.DEFAULT_GROUP_SIZE;
    public static final int DEFAULT_MAX_PLAYER_COUNT = TournamentConfig.DEFAULT_MAX_PLAYER_COUNT;
    public static final ChallengeFormat DEFAULT_CHALLENGE_FORMAT = TournamentConfig.DEFAULT_CHALLENGE_FORMAT;
    public static final int DEFAULT_CHALLENGE_MIN_LEVEL = TournamentConfig.DEFAULT_CHALLENGE_MIN_LEVEL;
    public static final int DEFAULT_CHALLENGE_MAX_LEVEL = TournamentConfig.DEFAULT_CHALLENGE_MAX_LEVEL;
    public static final boolean DEFAULT_SHOW_PREVIEW = TournamentConfig.DEFAULT_SHOW_PREVIEW;

    public TournamentProperties() {
        tournamentType = DEFAULT_TOURNAMENT_TYPE;
        groupSize = DEFAULT_GROUP_SIZE;
        maxPlayerCount = DEFAULT_MAX_PLAYER_COUNT;
        challengeFormat = DEFAULT_CHALLENGE_FORMAT;
        minLevel = DEFAULT_CHALLENGE_MIN_LEVEL;
        maxLevel = DEFAULT_CHALLENGE_MAX_LEVEL;
        showPreview = DEFAULT_SHOW_PREVIEW;
    }
    public TournamentProperties (
            TournamentType type,
            Integer groupSize,
            Integer maxPlayerCount,
            ChallengeFormat format,
            Integer minLevel,
            Integer maxLevel,
            Boolean showPreview
    ) {
        this.tournamentType = (type != null) ? type : DEFAULT_TOURNAMENT_TYPE;
        this.groupSize = groupSize != null ? groupSize : DEFAULT_GROUP_SIZE;
        this.maxPlayerCount = maxPlayerCount != null ? maxPlayerCount : DEFAULT_MAX_PLAYER_COUNT;
        this.challengeFormat = format != null ? format : DEFAULT_CHALLENGE_FORMAT;
        this.minLevel = minLevel != null ? minLevel : DEFAULT_CHALLENGE_MIN_LEVEL;
        this.maxLevel = maxLevel != null ? maxLevel : DEFAULT_CHALLENGE_MAX_LEVEL;
        this.showPreview = showPreview != null ? showPreview : DEFAULT_SHOW_PREVIEW;
    }

    private final TournamentType tournamentType;
    private final Integer groupSize;
    private final Integer maxPlayerCount;
    private final ChallengeFormat challengeFormat;
    private final int minLevel;
    private final int maxLevel;
    private final boolean showPreview;

    @NotNull public TournamentType getTournamentType(){ return tournamentType; }
    public int getGroupSize(){ return groupSize; }
    public int getMaxPlayerCount(){ return maxPlayerCount; }
    @NotNull public ChallengeFormat getChallengeFormat(){ return challengeFormat; }
    public int getMinLevel(){ return minLevel; }
    public int getMaxLevel(){ return maxLevel; }
    public boolean getShowPreview(){ return showPreview; }
}
