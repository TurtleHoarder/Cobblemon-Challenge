package com.cobblemontournament.api.builder;

import com.cobblemontournament.api.tournament.TournamentProperties;
import com.cobblemontournament.api.tournament.TournamentType;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeFormat;
import org.jetbrains.annotations.NotNull;

public final class TournamentPropertiesBuilder
{
    public TournamentPropertiesBuilder() {
        tournamentType = TournamentProperties.DEFAULT_TOURNAMENT_TYPE;
        groupSize = TournamentProperties.DEFAULT_GROUP_SIZE;
        maxPlayerCount = TournamentProperties.DEFAULT_MAX_PLAYER_COUNT;
        challengeFormat = TournamentProperties.DEFAULT_CHALLENGE_FORMAT;
        minLevel = TournamentProperties.DEFAULT_CHALLENGE_MIN_LEVEL;
        maxLevel = TournamentProperties.DEFAULT_CHALLENGE_MAX_LEVEL;
        showPreview = TournamentProperties.DEFAULT_SHOW_PREVIEW;
    }

    public TournamentPropertiesBuilder(
            TournamentType tournamentType,
            Integer groupSize,
            Integer maxPlayerCount,
            ChallengeFormat challengeFormat,
            Integer minLevel,
            Integer maxLevel,
            Boolean showPreview
    ) {
        this.tournamentType = tournamentType != null ? tournamentType : TournamentProperties.DEFAULT_TOURNAMENT_TYPE;
        this.groupSize = groupSize != null ? groupSize : TournamentProperties.DEFAULT_GROUP_SIZE;
        this.maxPlayerCount = maxPlayerCount != null ? maxPlayerCount : TournamentProperties.DEFAULT_MAX_PLAYER_COUNT;
        this.challengeFormat = challengeFormat != null ? challengeFormat : TournamentProperties.DEFAULT_CHALLENGE_FORMAT;
        this.minLevel = minLevel != null ? minLevel : TournamentProperties.DEFAULT_CHALLENGE_MIN_LEVEL;
        this.maxLevel = maxLevel != null ? maxLevel : TournamentProperties.DEFAULT_CHALLENGE_MAX_LEVEL;
        this.showPreview = showPreview != null ? showPreview : TournamentProperties.DEFAULT_SHOW_PREVIEW;
    }

    @NotNull private TournamentType tournamentType;
    private int groupSize;
    private int maxPlayerCount;
    @NotNull ChallengeFormat challengeFormat;
    private int minLevel;
    private int maxLevel;
    private boolean showPreview;

    @NotNull public TournamentType getTournamentType() { return tournamentType; }
    public boolean setTournamentType(TournamentType type) {
        if (type != null && tournamentType != type) {
            tournamentType = type;
            return true;
        }
        return false;
    }
    public int getGroupSize() { return groupSize; }
    public boolean setGroupSize(int groupSize) {
        if (this.groupSize != groupSize) {
            this.groupSize = groupSize;
            return true;
        }
        return false;
    }
    public int getMaxPlayerCount() { return maxPlayerCount; }
    public boolean setMaxPlayerCount(int maxPlayerCount) {
        if (this.maxPlayerCount != maxPlayerCount) {
            this.maxPlayerCount = maxPlayerCount;
            return true;
        }
        return false;
    }
    @NotNull public ChallengeFormat getChallengeFormat() { return challengeFormat; }
    public boolean setChallengeFormat(ChallengeFormat format) {
        if (format != null && challengeFormat != format) {
            challengeFormat = format;
            return true;
        }
        return false;
    }
    public int getMinLevel() { return minLevel; }
    public boolean setMinLevel(int min) {
        if (minLevel != min) {
            minLevel = min;
            return true;
        }
        return false;
    }
    public int getMaxLevel() { return maxLevel; }
    public boolean setMaxLevel(int max) {
        if (maxLevel != max) {
            maxLevel = max;
            return true;
        }
        return false;
    }
    public boolean getShowPreview() { return showPreview; }
    public boolean setShowPreview(boolean showPreview) {
        if (this.showPreview != showPreview) {
            this.showPreview = showPreview;
            return true;
        }
        return false;
    }

    public TournamentProperties toTournamentProperties() {
        return new TournamentProperties(
                getTournamentType(),
                getGroupSize(),
                getMaxPlayerCount(),
                getChallengeFormat(),
                getMinLevel(),
                getMaxLevel(),
                getShowPreview()
        );
    }
}
