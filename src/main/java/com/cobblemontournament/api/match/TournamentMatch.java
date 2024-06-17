package com.cobblemontournament.api.match;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TournamentMatch
{
    public TournamentMatch(
            @NotNull UUID tournamentID,
            @NotNull UUID roundID,
            UUID matchID,
            int tournamentMatchIndex,
            int roundMatchIndex
    ) {
        this.tournamentID = tournamentID;
        this.roundID = roundID;
        this.matchID = matchID != null ? matchID : UUID.randomUUID();
        this.tournamentMatchIndex = tournamentMatchIndex;
        this.roundMatchIndex = roundMatchIndex;
    }

    public TournamentMatch(
            @NotNull UUID tournamentID,
            @NotNull UUID roundID,
            UUID matchUUID,
            int tournamentMatchIndex,
            int roundMatchIndex,
            @Nullable UUID player1id,
            @Nullable UUID player2ID
    ) {
        this.tournamentID = tournamentID;
        this.roundID = roundID;
        this.matchID = matchUUID != null ? matchUUID : UUID.randomUUID();
        this.tournamentMatchIndex = tournamentMatchIndex;
        this.roundMatchIndex = roundMatchIndex;
        this.player1ID = player1id;
        this.player2ID = player2ID;
        updateStatus();
    }

    public final UUID tournamentID;
    public final UUID roundID;
    public final UUID matchID;
    public final int tournamentMatchIndex;
    public final int roundMatchIndex;
    private MatchStatus status = MatchStatus.Empty;

    @Nullable private UUID player1ID;
    @Nullable private UUID player2ID;
    @Nullable private UUID victorID;

    @Nullable public UUID player1()
    {
        return player1ID;
    }
    @Nullable public UUID player2()
    {
        return player2ID;
    }
    @Nullable public UUID victorID()
    {
        return victorID;
    }

    // TODO: create data class to hold & serialize finalized match details
    //      > possibly just use matchUUID to search a database

    public boolean trySetPlayer1(@NotNull UUID id)
    {
        if (player1ID != null) {
            return false;
        }
        player1ID = id;
        return true;
    }
    public boolean trySetPlayer2(@NotNull UUID id)
    {
        if (player2ID != null) {
            return false;
        }
        player2ID = id;
        return true;
    }

    public boolean updatePlayer(@NotNull UUID id)
    {
        if (player1ID != null && player2ID != null) {
            return false;
        }
        if (player1ID == id || player2ID == id) {
            return false;
        }
        if (player1ID == null) {
            player1ID = id;
            return true;
        }
        player2ID = id;
        return true;
    }
    public boolean updateResult(@NotNull UUID victorID)
    {
        if (player1ID != null && player2ID != null) {
            return false;
        }
        if (player1ID != victorID && player2ID != victorID) {
            return false;
        }
        this.victorID = victorID;
        return true;
    }

    public MatchStatus getStatus()
    {
        return updateStatus();
    }
    public MatchStatus updateStatus()
    {
        return switch (status) {
            case Error,Empty -> updateEmptyStatus(); // recycle error status for now to reset & possibly resolve issue
            case NotReady -> updateNotReadyStatus();
            case Ready -> updateReadyStatus();
            case InProgress -> updateInProgressStatus();
            case Complete -> updateCompleteStatus();
            case Finalized -> status;
        };
    }
    private MatchStatus updateEmptyStatus()
    {
        if (player1ID == null && player2ID == null) {
            if (status != MatchStatus.Empty) {
                status = MatchStatus.Empty;
            }
            return status;
        }
        return updateNotReadyStatus();
    }
    private MatchStatus updateNotReadyStatus()
    {
        if (player1ID == null || player2ID == null) {
            if (status != MatchStatus.NotReady) {
                status = MatchStatus.NotReady;
            }
            return status;
        }
        return updateReadyStatus();
    }
    private MatchStatus updateReadyStatus()
    {
        if (player1ID != null && player2ID != null) {
            if (status != MatchStatus.Ready) {
                status = MatchStatus.Ready;
            }
            return status;
        }
        // TODO: implement check for matches in progress on the server -> progress to inProgress
        return MatchStatus.Ready;
    }
    private MatchStatus updateInProgressStatus()
    {
        // TODO: implement check for matches in progress on the server -> if not progress to Complete
        return MatchStatus.InProgress;
    }
    private MatchStatus updateCompleteStatus()
    {
        // TODO: implement check for matches in progress on the server ->
        return MatchStatus.Complete;
    }

}
