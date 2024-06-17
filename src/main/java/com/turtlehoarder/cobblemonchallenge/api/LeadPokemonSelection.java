package com.turtlehoarder.cobblemonchallenge.api;

import com.turtlehoarder.cobblemonchallenge.gui.LeadPokemonSelectionSession;

public record LeadPokemonSelection(
        LeadPokemonSelectionSession selectionWrapper,
        long createdTime
) {

}
