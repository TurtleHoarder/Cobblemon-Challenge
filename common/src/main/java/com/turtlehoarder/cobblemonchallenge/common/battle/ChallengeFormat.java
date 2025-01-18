package com.turtlehoarder.cobblemonchallenge.common.battle;

import com.cobblemon.mod.common.battles.BattleType;
import com.cobblemon.mod.common.battles.BattleTypes;

public enum ChallengeFormat {
    STANDARD_6V6(6, 1, "6v6", BattleTypes.INSTANCE.getSINGLES()),
    STANDARD_5V5(5, 5, "5v5", BattleTypes.INSTANCE.getSINGLES()),
    STANDARD_4V4(4, 4, "4v4", BattleTypes.INSTANCE.getSINGLES()),
    STANDARD_3V3(3,3, "3v3", BattleTypes.INSTANCE.getSINGLES()),
    STANDARD_2V2(2, 2, "2v2", BattleTypes.INSTANCE.getSINGLES()),
    STANDARD_1V1(1,1, "1v1", BattleTypes.INSTANCE.getSINGLES()),
    STANDARD_DOUBLES_6V6(6,2, "6v6 Doubles", BattleTypes.INSTANCE.getDOUBLES());

    private final int maxPokemonSlots; // Total number of pokemon in the battle
    private final int numberSelected; // Number of pokemon selected for battle
    private String title = "";
    private BattleType battleType;

    ChallengeFormat(int pokemonSlots, int numberSelected, String title, BattleType battleType) {
        this.maxPokemonSlots = pokemonSlots;
        this.numberSelected = numberSelected;
        this.title = title;
        this.battleType = battleType;
    }

    public int getTotalPokemonSlots() {
        return maxPokemonSlots;
    }

    public int getTotalPokemonSelected() {
        return numberSelected;
    }

    public BattleType getBattleType() {
        return battleType;
    }

    public String getTitle() {
        return title;
    }
}