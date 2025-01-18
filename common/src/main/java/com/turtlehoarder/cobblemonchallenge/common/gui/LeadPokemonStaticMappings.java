package com.turtlehoarder.cobblemonchallenge.common.gui;

import com.turtlehoarder.cobblemonchallenge.common.command.ChallengeCommand;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LeadPokemonStaticMappings {
    // Mappings of # pokemon selected and where in the menu to put it
    // Yes all of this is hardcoded and it makes me sad inside :(
    // Maps for Doubles / 2v2s
    private static final Map<Integer, Integer> allySlotToMenuIDDoubles = Map.of(1,20,2,21);
    private static final Map<Integer, Integer> rivalSlotToMenuIDDoubles = Map.of(1,23,2,24);
    // Locked-in Glass positions for 2v2s
    private static final List<Integer> allyGlassLockin2v2 = List.of(11,12,29,30);
    private static final List<Integer> rivalGlassLockin2v2 = List.of(14,15,32,33);
    // Maps for 3v3
    private static final Map<Integer, Integer> allySlotToMenuID3v3 = Map.of(1,12,2,21,3,30);
    private static final Map<Integer, Integer> rivalSlotToMenuID3v3 = Map.of(1,14,2,23,3,32);
    // Locked-in Glass positions for 3v3s
    private static final List<Integer> allyGlassLockin3v3 = List.of(3,11,20,29,39);
    private static final List<Integer> rivalGlassLockin3v3 = List.of(5,15,24,33,41);
    // Maps for 4v4
    private static final Map<Integer, Integer> allySlotToMenuID4v4 = Map.of(1,12,2,21,3,30, 4, 39);
    private static final Map<Integer, Integer> rivalSlotToMenuID4v4 = Map.of(1,14,2,23,3,32, 4, 41);
    // Locked-in Glass positions for 4v4s
    private static final List<Integer> allyGlassLockin4v4 = List.of(48,38,29,20,11,3);
    private static final List<Integer> rivalGlassLockin4v4 = List.of(5,15,24,33,42,50);
    // Maps for 5v5
    private static final Map<Integer, Integer> allySlotToMenuID5v5 = Map.of(1,12,2,21,3,30, 4, 39, 5, 48);
    private static final Map<Integer, Integer> rivalSlotToMenuID5v5 = Map.of(1,14,2,23,3,32, 4, 41, 5, 50);
    // Locked-in Glass positions for 5v5s
    private static final List<Integer> allyGlassLockin5v5 = List.of(2,3,11,20,29,38,47);
    private static final List<Integer> rivalGlassLockin5v5 = List.of(5,6,15,24,33,42,51);

    protected static List<Integer> getLockinGlassPositionAlly(ChallengeCommand.ChallengeRequest request) {
        return switch (request.format().getTotalPokemonSelected()) {
            case 5 -> LeadPokemonStaticMappings.allyGlassLockin5v5;
            case 4 -> LeadPokemonStaticMappings.allyGlassLockin4v4;
            case 3 -> LeadPokemonStaticMappings.allyGlassLockin3v3;
            case 2 -> LeadPokemonStaticMappings.allyGlassLockin2v2;
            default -> Collections.emptyList();
        };
    }

    protected static List<Integer> getLockinGlassPositionRival(ChallengeCommand.ChallengeRequest request) {
        return switch (request.format().getTotalPokemonSelected()) {
            case 5 -> LeadPokemonStaticMappings.rivalGlassLockin5v5;
            case 4 -> LeadPokemonStaticMappings.rivalGlassLockin4v4;
            case 3 -> LeadPokemonStaticMappings.rivalGlassLockin3v3;
            case 2 -> LeadPokemonStaticMappings.rivalGlassLockin2v2;
            default -> Collections.emptyList();
        };
    }

    protected static Map<Integer, Integer> getPositionAllyMap(ChallengeCommand.ChallengeRequest request) {
        return switch (request.format().getTotalPokemonSelected()) {
            case 5 -> LeadPokemonStaticMappings.allySlotToMenuID5v5;
            case 4 -> LeadPokemonStaticMappings.allySlotToMenuID4v4;
            case 3 -> LeadPokemonStaticMappings.allySlotToMenuID3v3;
            case 2 -> LeadPokemonStaticMappings.allySlotToMenuIDDoubles;
            default -> Collections.emptyMap();
        };
    }

    protected static Map<Integer, Integer> getPositionRivalMap(ChallengeCommand.ChallengeRequest request) {
        return switch (request.format().getTotalPokemonSelected()) {
            case 5 -> LeadPokemonStaticMappings.rivalSlotToMenuID5v5;
            case 4 -> LeadPokemonStaticMappings.rivalSlotToMenuID4v4;
            case 3 -> LeadPokemonStaticMappings.rivalSlotToMenuID3v3;
            case 2 -> LeadPokemonStaticMappings.rivalSlotToMenuIDDoubles;
            default -> Collections.emptyMap();
        };
    }
}
