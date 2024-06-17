package com.cobblemontournament.api.tournament;

import com.cobblemontournament.api.round.TournamentRound;
import com.cobblemontournament.api.pokemon.PokemonEntry;
import com.cobblemontournament.api.pokemon.PokemonTeam;
import com.cobblemontournament.api.match.TournamentMatch;
import com.cobblemontournament.api.player.TournamentPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public final class Tournament
{
    public Tournament(
            @NotNull TournamentProperties properties,
            @NotNull UUID tournamentID,
            @NotNull ArrayList<TournamentRound> rounds
    ) {
        this.tournamentID = tournamentID;
        this.tournamentProperties = properties;
        // TODO register rounds to a HashMap<UUID,TournamentRound>
        this.rounds = rounds;
        // TODO register matches, players, pokemon teams, pokemon to hashmaps
        //      -> pokemon teams & pokemon temporary until a server wide database is implemented
        //          -> !! still confirm pokemon & teams are in database when implemented !!
    }

    @NotNull public final UUID tournamentID;
    @NotNull public final TournamentProperties tournamentProperties;
    @NotNull public final ArrayList<TournamentRound> rounds;
    private final HashMap<UUID,TournamentMatch> matches = new HashMap<>();
    private final HashMap<UUID,TournamentPlayer> players = new HashMap<>();
    private final HashMap<UUID,PokemonTeam> pokemonTeams = new HashMap<>(); // TODO: temporary until rental team functionality implemented
    private final HashMap<UUID,PokemonEntry> pokemonEntries = new HashMap<>(); // TODO: temporary until rental team functionality implemented

    @SuppressWarnings("unused")
    public void safeCloneOf(boolean reset){
        // TODO: make a deep copy of tournament
        //  if reset -> clone with clean start
    }

    @SuppressWarnings("unused")
    public void safeBuilderCloneOf(boolean reset){
        // TODO: make a deep copy of tournament back into tournament builder
        //  if reset -> clone with clean start
    }
}
