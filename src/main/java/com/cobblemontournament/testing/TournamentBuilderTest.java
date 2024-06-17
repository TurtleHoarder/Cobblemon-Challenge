package com.cobblemontournament.testing;

import com.cobblemontournament.api.builder.TournamentBuilder;
import com.cobblemontournament.api.builder.TournamentPropertiesBuilder;
import com.cobblemontournament.api.match.TournamentMatch;
import com.cobblemontournament.api.round.TournamentRound;
import com.cobblemontournament.api.tournament.Tournament;
import com.cobblemontournament.api.tournament.TournamentType;
import com.turtlehoarder.cobblemonchallenge.battle.ChallengeFormat;
import org.slf4j.helpers.Util; // Util.report("");

import java.util.UUID;

public class TournamentBuilderTest
{
    public static void buildTournamentDebug(int maxPlayers,boolean doPrint)
    {
        var propertiesBuilder = new TournamentPropertiesBuilder(
                TournamentType.SingleElimination,
                null,
                maxPlayers,
                ChallengeFormat.STANDARD_6V6,
                null,
                null,
                null
        );
        var tournamentBuilder = new TournamentBuilder(propertiesBuilder);

        for (int i = 0; i < maxPlayers; i++) {
            tournamentBuilder.addPlayer(UUID.randomUUID(),i);
        }

        var tournament = tournamentBuilder.toTournament();

        if (doPrint && tournament != null){
            printTournamentDebug(tournament);
        }
    }

    public static void printTournamentDebug(Tournament tournament)
    {
        // print Properties
        Util.report("Tournament Debug - ID: " + tournament.tournamentID);
        Util.report("   Properties:");
        Util.report("     - Tournament Type: " + tournament.tournamentProperties.getTournamentType());
        Util.report("     - Group Size: " + tournament.tournamentProperties.getGroupSize());
        Util.report("     - Max Players: " + tournament.tournamentProperties.getMaxPlayerCount());
        Util.report("     - Challenge Format: " + tournament.tournamentProperties.getChallengeFormat());
        Util.report("     - Min Level: " + tournament.tournamentProperties.getMinLevel());
        Util.report("     - Max Level: " + tournament.tournamentProperties.getMaxLevel());
        Util.report("     - Show Preview: " + tournament.tournamentProperties.getShowPreview());

        int roundCount = tournament.rounds.size();
        int matchCount = 0;
        for (int i = 0; i < roundCount; i++) {
            matchCount += tournament.rounds.get(i).matches.size();
        }

        Util.report("   Details:");
        Util.report("     - Rounds: " + roundCount);
        Util.report("     - Matches: " + matchCount);

        // print round details
        for (int i = 0; i < roundCount;i++) {
            printRoundDetails(tournament.rounds.get(i),true);
        }
    }

    public static void printRoundDetails(TournamentRound round,boolean includeMatches)
    {
        Util.report("Round Details - ID:" + round.roundUUID);
        Util.report("- Round Type: " + round.roundType);
        Util.report("- Round Index: " + round.roundIndex);
        Util.report("- Matches: " + round.matches.size());
        if (!includeMatches) {
            return;
        }
        var size = round.matches.size();
        for (int i = 0; i < size;i++) {
            printMatchDetails(round.matches.get(i));
        }
    }
    public static void printMatchDetails(TournamentMatch match)
    {
        Util.report("Match Details - ID:" + match.matchID);
        Util.report("- Tournament Match Index: " + match.tournamentMatchIndex);
        Util.report("- Round Match Index: " + match.roundMatchIndex);
        Util.report("- Player 1: " + match.player1());
        Util.report("- Player 2: " + match.player2());
        Util.report("- Victor: " + match.victorID());
        Util.report("- Status: " + match.getStatus());
    }
}
