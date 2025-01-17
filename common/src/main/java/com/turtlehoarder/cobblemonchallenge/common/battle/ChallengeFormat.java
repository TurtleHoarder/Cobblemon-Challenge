package com.turtlehoarder.cobblemonchallenge.common.battle;

public enum ChallengeFormat {
    STANDARD_6V6(6, 1, "6v6"),
    STANDARD_3V3(3,3, "3v3"),
    STANDARD_DOUBLES_6V6(6,2, "Doubles");

    private int maxPokemonSlots; // Total number of pokemon in the battle
    private int numberSelected; // Number of pokemon selected for battle
    private String title = "";
    ChallengeFormat(int pokemonSlots, int numberSelected, String title) {
        this.maxPokemonSlots = pokemonSlots;
        this.numberSelected = numberSelected;
        this.title = title;
    }

    public int getTotalPokemonSlots() {
        return maxPokemonSlots;
    }

    public int getTotalPokemonSelected() {
        return numberSelected;
    }

    public String getTitle() {
        return title;
    }
}