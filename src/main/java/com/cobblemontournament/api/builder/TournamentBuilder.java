package com.cobblemontournament.api.builder;

import com.cobblemontournament.api.round.TournamentRoundType;
import com.cobblemontournament.api.tournament.Tournament;
import com.cobblemontournament.api.match.TournamentMatch;
import com.cobblemontournament.api.player.SeededPlayer;
import com.cobblemontournament.api.round.TournamentRound;
import com.cobblemontournament.util.IndexedSeedArray;
import com.cobblemontournament.util.IndexedSeedSortType;
import com.cobblemontournament.util.SeedUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public final class TournamentBuilder
{
    public TournamentBuilder(TournamentPropertiesBuilder builder)
    {
        propertiesBuilder = builder != null ? builder : new TournamentPropertiesBuilder();
    }

    @NotNull public final TournamentPropertiesBuilder propertiesBuilder;
    @NotNull private final ArrayList<SeededPlayer> seededPlayers = new ArrayList<>();
    @NotNull private final ArrayList<SeededPlayer> unseededPlayers = new ArrayList<>();

    // player registration & management
    public int getPlayerCount()
    {
        return seededPlayers.size() + unseededPlayers.size();
    }
    public boolean addPlayer(UUID playerID, Integer seed)
    {
        Predicate<? super SeededPlayer> predicate = sp -> sp.id() == playerID;
        if (containsPlayerWith(seededPlayers,predicate) || containsPlayerWith(unseededPlayers,predicate)) {
            return false;
        }
        return seededPlayers.add(new SeededPlayer(playerID, seed));
    }
    public boolean updateSeededPlayer(UUID playerID, Integer seed)
    {
        int notNullSeed = (seed != null && seed > 0) ? seed : -1;
        Predicate<? super SeededPlayer> removePredicate = sp -> sp.id() == playerID && sp.seed() != notNullSeed;
        removePlayerIf(seededPlayers,removePredicate);
        removePlayerIf(unseededPlayers,removePredicate);
        return addPlayer(playerID, seed);
    }
    public boolean removePlayer(@NotNull UUID playerID)
    {
        boolean removed = removePlayerIf(seededPlayers, sp -> sp.id() == playerID);
        return removePlayerIf(unseededPlayers, sp -> sp.id() == playerID) || removed;
    }
    private boolean containsPlayerWith(ArrayList<SeededPlayer> collection,Predicate<? super SeededPlayer> predicate)
    {
        return collection.stream().anyMatch(predicate);
    }
    private boolean removePlayerIf(ArrayList<SeededPlayer> collection,Predicate<? super SeededPlayer> predicate)
    {
        return collection.removeIf(predicate);
    }

    public int getRoundCount()
    {
        return switch (propertiesBuilder.getTournamentType()) {
            case SingleElimination -> getRoundCountSingleElimination();
            case DoubleElimination -> getRoundCountDoubleElimination();
            case RoundRobin -> getRoundCountRoundRobin();
            case VGC -> getRoundCountVGC();
        };
    }
    private int getRoundCountSingleElimination()
    {
        int playerCount = getPlayerCount();
        int bracketSlots = SeedUtil.ceilToPowerOfTwo(playerCount);
        int rounds = 0;
        while (bracketSlots > 0) {
            bracketSlots = bracketSlots >> 1;
            rounds++;
        }
        return rounds;
    }
    private int getRoundCountDoubleElimination()
    {
        double playersRemaining = getPlayerCount();
        int rounds = 0;
        while (playersRemaining > 1) {
            playersRemaining = Math.ceil(playersRemaining * 0.5f);
        }

        // TODO lower bracket

        return rounds;
    }
    private int getRoundCountRoundRobin()
    {
        var playersRemaining = getPlayerCount();
        int rounds = 0;

        // TODO

        return rounds;
    }
    private int getRoundCountVGC()
    {
        double playersRemaining = getPlayerCount();
        int rounds = 0;

        // TODO

        return rounds;
    }

    /** Construct a finalized tournament */
    public Tournament toTournament()
    {
        var playerCount = getPlayerCount();
        if (playerCount < 2) {
            // TODO log not enough players for tournament
            return null;
        }

        return switch (propertiesBuilder.getTournamentType()){
            case SingleElimination -> handleSingleElimination(UUID.randomUUID(),playerCount);
            case DoubleElimination -> handleDoubleElimination(UUID.randomUUID(),playerCount);
            case RoundRobin -> handleRoundRobin(UUID.randomUUID(),playerCount);
            case VGC -> handleVGC(UUID.randomUUID(),playerCount);
        };
    }

    private Tournament handleSingleElimination(UUID tournamentID,int playerCount)
    {
        var properties = propertiesBuilder.toTournamentProperties();
        var roundCount = getRoundCount();

        // get total matches in first -> always power of 2 for single & double elimination brackets
        var matchCount = 1 << (roundCount - 1); // remove championship b/c... fml

        ArrayList<TournamentRound> rounds = new ArrayList<>(roundCount);
        rounds.add(getFirstEliminationRound(tournamentID));

        int totalMatches = matchCount;
        for(int i = 1; i < roundCount; i++) {
            // size of indexed seeds should always be power of 2 -> this cuts it in half 'safely'
            matchCount = matchCount >> 1;
            rounds.add(getInitializedRound(tournamentID,TournamentRoundType.Primary,i,totalMatches - 1,matchCount));
            totalMatches += matchCount;
        }

        return new Tournament(properties,tournamentID,rounds);
    }
    private Tournament handleDoubleElimination(UUID tournamentID,int playerCount)
    {
        // TODO log || implement
        return null;
    }
    private Tournament handleRoundRobin(UUID tournamentID,int playerCount)
    {
        // TODO log || implement
        return null;
    }
    private Tournament handleVGC(UUID tournamentID,int playerCount)
    {
        // TODO log || implement
        return null;
    }


    private TournamentRound getFirstEliminationRound(UUID tournamentID)
    {
        var roundID = UUID.randomUUID();
        var orderedPlayers = sortAndSyncSeededPlayers(seededPlayers);
        var indexedSeeds = SeedUtil.getIndexedSeedArray(getPlayerCount(), IndexedSeedSortType.INDEX_ASCENDING);
        ArrayList<TournamentMatch> matches = getSeedOrderedMatches(tournamentID,roundID,orderedPlayers,indexedSeeds);
        fillWithUnseededPlayers(matches,indexedSeeds);
        return new TournamentRound(tournamentID,roundID,0,TournamentRoundType.Primary,matches);
    }

    private ArrayList<SeededPlayer> sortAndSyncSeededPlayers(ArrayList<SeededPlayer> seededPlayers)
    {
        var orderedPlayers = new ArrayList<>(seededPlayers.stream().toList());
        orderedPlayers.sort(Comparator.comparing(SeededPlayer::seed)); // ascending

        Random random = new Random();
        var sameSeededPlayers = new ArrayList<SeededPlayer>();
        int size = orderedPlayers.size();
        for (int i = 0; i < size; i++) {
            SeededPlayer nextPlayer;
            if (!sameSeededPlayers.isEmpty()) {
                var index = random.ints(0, sameSeededPlayers.size())
                        .findFirst()
                        .orElse(0);
                nextPlayer = sameSeededPlayers.remove(index);
            } else if (i + 1 != size && Objects.equals(orderedPlayers.get(i).seed(), orderedPlayers.get(i + 1).seed())) { // 'i + 1 != size' to catch out of bounds error on last iteration
                // multiple players with same seed -> create collection to pull players from at random
                var lastIndex = i;
                sameSeededPlayers.add(orderedPlayers.get(lastIndex));
                while (Objects.equals(orderedPlayers.get(lastIndex).seed(), orderedPlayers.get(lastIndex + 1).seed()))
                {
                    sameSeededPlayers.add(orderedPlayers.get(++lastIndex));
                }
                var index = random.ints(0, sameSeededPlayers.size())
                        .findFirst()
                        .orElse(0);
                nextPlayer = sameSeededPlayers.remove(index);
            } else {
                // just add the next player in order with new instance containing synced seed
                nextPlayer = orderedPlayers.get(i);
            }

            orderedPlayers.remove(i);
            orderedPlayers.add(i,new SeededPlayer(nextPlayer.id(), i + 1));
        }
        return orderedPlayers;
    }

    private ArrayList<TournamentMatch> getSeedOrderedMatches(
            UUID tournamentID,
            UUID roundID,
            ArrayList<SeededPlayer> players,
            @NotNull IndexedSeedArray indexedSeeds
    ) {
        if (indexedSeeds.sortStatus() != IndexedSeedSortType.INDEX_ASCENDING) {
            indexedSeeds.sortBySeedAscending();
        }
        var size = indexedSeeds.size();
        int matchCount = size >> 1; // size of indexed seeds should always be power of 2 -> cuts in half 'safely'...
        var matches = new ArrayList<TournamentMatch>(matchCount);
        int seedIndex = 0;
        for (int i = 0; i < matchCount; i++) {
            var seed1 = indexedSeeds.collection.get(seedIndex++).seed();
            var seed2 = indexedSeeds.collection.get(seedIndex++).seed();
            var player1 = players.stream()
                    .filter(p -> p.seed().equals(seed1))
                    .findFirst()
                    .orElse(null);
            var player2 = players.stream()
                    .filter(p -> p.seed().equals(seed2))
                    .findFirst()
                    .orElse(null);
            UUID player1ID = player1 != null ? player1.id() : null;
            UUID player2ID = player2 != null ? player2.id() : null;
            matches.add(new TournamentMatch(tournamentID,roundID,UUID.randomUUID(),i,i,player1ID,player2ID));
        }
        return matches;
    }

    private void fillWithUnseededPlayers(
            @NotNull ArrayList<TournamentMatch> matches,
            @NotNull IndexedSeedArray indexedSeeds
    ) {
        indexedSeeds.sortBySeedAscending();
        var unseededCount = unseededPlayers.size();
        var unseededIndex = 0;
        for (int i = 0; i < indexedSeeds.size();i++) {
            var seedIndex = indexedSeeds.get(i).index();
            var matchIndex = seedIndex/2;
            var remainder = seedIndex%2; // used to place player correctly into player1 or player2 -> always -> 1 | 0
            var match = matches.get(matchIndex);

            if (remainder == 0) {
                // is player 1 slot
                if (match.player1() == null){
                    // available slot
                    var player = unseededPlayers.get(unseededIndex);
                    if (match.trySetPlayer1(player.id())){
                        unseededIndex++;
                    } //else { // TODO log if fail }
                }
            } else {
                // is player 2 slot
                if (match.player1() == null){
                    // available slot
                    var player = unseededPlayers.get(unseededIndex);
                    if (match.trySetPlayer2(player.id())) {
                        unseededIndex++;
                    } //else { // TODO log if fail }
                }
            }

            if (unseededIndex == unseededCount) {
                break;
            }
        }
    }

    private TournamentRound getInitializedRound(
            UUID tournamentID,
            TournamentRoundType type, // ignore warning -> will go away when other types implemented
            int roundIndex,
            int lastMatchIndex,
            int matchCount
    ) {
        UUID roundID = UUID.randomUUID();
        var matches = new ArrayList<TournamentMatch>(matchCount);
        for (var i = 0; i < matchCount; i++) {
            matches.add(new TournamentMatch(tournamentID,roundID,UUID.randomUUID(),lastMatchIndex++,i));
        }
        return new TournamentRound(tournamentID,roundID,roundIndex,type,matches);
    }

}
