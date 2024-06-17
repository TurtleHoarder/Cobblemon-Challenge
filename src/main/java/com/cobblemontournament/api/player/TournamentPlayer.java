package com.cobblemontournament.api.player;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("unused")
public final class TournamentPlayer
{
    public TournamentPlayer(
            UUID player,
            int seed
    ) {
        this.player = player;
        this.seed = seed;
    }
    public TournamentPlayer(
            UUID player,
            int seed,
            @Nullable UUID pokemonSideUUID
    ) {
        this.player = player;
        this.seed = seed;
        this.pokemonSideUUID = pokemonSideUUID;
    }

    @Nullable private UUID pokemonSideUUID;
    private Integer finalPlacement;

    public final UUID player;
    public final int seed;

    @Nullable public UUID getPokemonSideUUID() { return pokemonSideUUID; }
    public boolean updatePokemonSideUUID(UUID pokemonSideUUID, boolean override) {
        if (this.pokemonSideUUID == pokemonSideUUID || (!override && this.pokemonSideUUID != null)){
            return false;
        }
        this.pokemonSideUUID = pokemonSideUUID;
        return true;
    }
    public int getFinalPlacement() { return finalPlacement != null ? finalPlacement : -1; }
    public boolean updateFinalPlacement(int finalPlacement) {
        if (this.finalPlacement == finalPlacement) {
            return false;
        }
        this.finalPlacement = finalPlacement;
        return true;
    }

}
