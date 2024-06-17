package com.cobblemontournament.api.round;

import com.cobblemontournament.api.match.TournamentMatch;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.ArrayList;

public final class TournamentRound
{
    public TournamentRound(
            @NotNull UUID tournamentID,
            UUID roundUUID,
            int roundIndex,
            @NotNull TournamentRoundType roundType,
            ArrayList<TournamentMatch> matches
    ) {
        this.tournamentID = tournamentID;
        this.roundUUID = roundUUID != null ? roundUUID : UUID.randomUUID();
        this.roundIndex = roundIndex;
        this.roundType = roundType;
        this.matches = matches;
    }

    public final UUID tournamentID;
    public final UUID roundUUID;
    public final int roundIndex;
    public final TournamentRoundType roundType;
    public final ArrayList<TournamentMatch> matches;

    public void updateMatch(TournamentMatch match)
    {
        matches.add(match);
    }
}
