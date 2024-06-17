package com.cobblemontournament.api.pokemon;

import java.util.UUID;
import java.util.Vector;

public record PokemonTeam (UUID id,Vector<PokemonEntry> pokemon) { }
