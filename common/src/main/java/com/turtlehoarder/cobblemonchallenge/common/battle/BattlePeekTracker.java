package com.turtlehoarder.cobblemonchallenge.common.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BattlePeekTracker {

    private static final Map<String, String> STATUS_DISPLAY_NAMES = Map.of(
            "par", "Paralyzed",
            "brn", "Burned",
            "psn", "Poisoned",
            "tox", "Badly Poisoned",
            "slp", "Asleep",
            "frz", "Frozen"
    );

    public static class SeenPokemon {
        private final String nickname;
        private final String species;
        private final int level;
        private int healthPercent = 100;
        private String statusCondition = null;
        private final LinkedHashSet<String> seenMoves = new LinkedHashSet<>();

        public SeenPokemon(String nickname, String species, int level) {
            this.nickname = nickname;
            this.species = species;
            this.level = level;
        }

        public String getNickname() { return nickname; }
        public String getSpecies() { return species; }
        public int getLevel() { return level; }
        public int getHealthPercent() { return healthPercent; }
        public String getStatusCondition() { return statusCondition; }
        public LinkedHashSet<String> getSeenMoves() { return seenMoves; }

        public String getStatusDisplayName() {
            if (statusCondition == null) return null;
            return STATUS_DISPLAY_NAMES.getOrDefault(statusCondition, statusCondition.toUpperCase());
        }

        public void addMove(String moveName) {
            seenMoves.add(moveName);
        }

        public void setHealthPercent(int healthPercent) {
            this.healthPercent = Math.max(0, Math.min(100, healthPercent));
        }

        public void setStatusCondition(String statusCondition) {
            this.statusCondition = statusCondition;
        }

        public void clearStatusCondition() {
            this.statusCondition = null;
        }
    }

    // battleId -> (playerUUID -> list of seen opponent pokemon)
    private static final ConcurrentHashMap<UUID, Map<UUID, List<SeenPokemon>>> battleTracking = new ConcurrentHashMap<>();

    public static void initBattle(UUID battleId) {
        battleTracking.put(battleId, new ConcurrentHashMap<>());
    }

    public static void cleanupBattle(UUID battleId) {
        battleTracking.remove(battleId);
    }

    public static List<SeenPokemon> getSeenPokemon(UUID battleId, UUID playerUUID) {
        Map<UUID, List<SeenPokemon>> battleData = battleTracking.get(battleId);
        if (battleData == null) return Collections.emptyList();
        List<SeenPokemon> seen = battleData.get(playerUUID);
        return seen != null ? seen : Collections.emptyList();
    }

    public static void processRawMessage(PokemonBattle battle, String rawMessage) {
        if (!ChallengeUtil.isBattleChallenge(battle.getBattleId())) {
            return;
        }

        // Lazily init tracking if not already done
        battleTracking.computeIfAbsent(battle.getBattleId(), k -> new ConcurrentHashMap<>());

        String[] lines = rawMessage.split("\n");
        for (String line : lines) {
            if (line.startsWith("|switch|") || line.startsWith("|drag|")) {
                processSwitch(battle, line);
            } else if (line.startsWith("|move|")) {
                processMove(battle, line);
            } else if (line.startsWith("|-damage|") || line.startsWith("|-heal|")) {
                processHealthChange(battle, line);
            } else if (line.startsWith("|faint|")) {
                processFaint(battle, line);
            } else if (line.startsWith("|-status|")) {
                processStatus(battle, line);
            } else if (line.startsWith("|-curestatus|")) {
                processCureStatus(battle, line);
            }
        }
    }

    private static void processSwitch(PokemonBattle battle, String line) {
        // Format: |switch|p1a: Pikachu|Pikachu, L50, M|100/100
        // or:     |drag|p2a: Dragonite|Dragonite, L50, F|90/100
        String[] parts = line.split("\\|");
        // parts[0] = "", parts[1] = "switch"/"drag", parts[2] = "p1a: Pikachu", parts[3] = "Pikachu, L50, M", ...
        if (parts.length < 4) return;

        String pokemonIdent = parts[2].trim(); // e.g. "p1a: Pikachu"
        String details = parts[3].trim();      // e.g. "Pikachu, L50, M"

        String side = extractSide(pokemonIdent); // "p1" or "p2"
        if (side == null) return;

        String nickname = extractNickname(pokemonIdent); // "Pikachu"

        // Parse details for species and level
        // Note: Showdown protocol omits "L100" when level is 100 (the default)
        String[] detailParts = details.split(",");
        String species = detailParts[0].trim();
        int level = 100;
        for (int i = 1; i < detailParts.length; i++) {
            String part = detailParts[i].trim();
            if (part.startsWith("L")) {
                try {
                    level = Integer.parseInt(part.substring(1));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Determine which player sees this as their opponent's pokemon
        UUID observerUUID = getOpponentPlayerUUID(battle, side);
        if (observerUUID == null) return;

        Map<UUID, List<SeenPokemon>> battleData = battleTracking.get(battle.getBattleId());
        if (battleData == null) return;

        List<SeenPokemon> seenList = battleData.computeIfAbsent(observerUUID, k -> new ArrayList<>());

        // Parse HP from the switch line if present (e.g. "100/100" or "90/100 par")
        int healthPercent = 100;
        if (parts.length >= 5) {
            healthPercent = parseHealthPercent(parts[4].trim());
        }

        // Check if we've already seen this pokemon (by species + nickname combo)
        SeenPokemon existing = null;
        for (SeenPokemon sp : seenList) {
            if (sp.getSpecies().equalsIgnoreCase(species) && sp.getNickname().equals(nickname)) {
                existing = sp;
                break;
            }
        }
        if (existing != null) {
            // Update health on re-switch
            existing.setHealthPercent(healthPercent);
        } else {
            SeenPokemon newPokemon = new SeenPokemon(nickname, species, level);
            newPokemon.setHealthPercent(healthPercent);
            seenList.add(newPokemon);
        }
    }

    private static void processMove(PokemonBattle battle, String line) {
        // Format: |move|p1a: Pikachu|Thunderbolt|p2a: Bulbasaur
        String[] parts = line.split("\\|");
        // parts[0] = "", parts[1] = "move", parts[2] = "p1a: Pikachu", parts[3] = "Thunderbolt", ...
        if (parts.length < 4) return;

        String pokemonIdent = parts[2].trim(); // e.g. "p1a: Pikachu"
        String moveName = parts[3].trim();     // e.g. "Thunderbolt"

        String side = extractSide(pokemonIdent);
        if (side == null) return;

        String nickname = extractNickname(pokemonIdent);

        // The opponent of the pokemon using the move observes it
        UUID observerUUID = getOpponentPlayerUUID(battle, side);
        if (observerUUID == null) return;

        Map<UUID, List<SeenPokemon>> battleData = battleTracking.get(battle.getBattleId());
        if (battleData == null) return;

        List<SeenPokemon> seenList = battleData.get(observerUUID);
        if (seenList == null) return;

        // Find the matching SeenPokemon and add the move
        for (SeenPokemon sp : seenList) {
            if (sp.getNickname().equals(nickname)) {
                sp.addMove(moveName);
                break;
            }
        }
    }

    private static void processHealthChange(PokemonBattle battle, String line) {
        // Format: |-damage|p1a: Pikachu|48/100 par  or  |-heal|p1a: Pikachu|75/100
        String[] parts = line.split("\\|");
        // parts[0] = "", parts[1] = "-damage"/"-heal", parts[2] = "p1a: Pikachu", parts[3] = "48/100 par"
        if (parts.length < 4) return;

        String pokemonIdent = parts[2].trim();
        String hpStatus = parts[3].trim();

        String side = extractSide(pokemonIdent);
        if (side == null) return;
        String nickname = extractNickname(pokemonIdent);

        int healthPercent = parseHealthPercent(hpStatus);
        String status = parseStatusFromHpString(hpStatus);

        // The opponent of this pokemon observes the health change
        UUID observerUUID = getOpponentPlayerUUID(battle, side);
        if (observerUUID == null) return;

        updateHealth(battle.getBattleId(), observerUUID, nickname, healthPercent);
        if (status != null) {
            updateStatus(battle.getBattleId(), observerUUID, nickname, status);
        }
    }

    private static void processFaint(PokemonBattle battle, String line) {
        // Format: |faint|p1a: Pikachu
        String[] parts = line.split("\\|");
        if (parts.length < 3) return;

        String pokemonIdent = parts[2].trim();
        String side = extractSide(pokemonIdent);
        if (side == null) return;
        String nickname = extractNickname(pokemonIdent);

        UUID observerUUID = getOpponentPlayerUUID(battle, side);
        if (observerUUID == null) return;

        updateHealth(battle.getBattleId(), observerUUID, nickname, 0);
    }

    private static void processStatus(PokemonBattle battle, String line) {
        // Format: |-status|p1a: Pikachu|par
        String[] parts = line.split("\\|");
        if (parts.length < 4) return;

        String pokemonIdent = parts[2].trim();
        String status = parts[3].trim();

        String side = extractSide(pokemonIdent);
        if (side == null) return;
        String nickname = extractNickname(pokemonIdent);

        UUID observerUUID = getOpponentPlayerUUID(battle, side);
        if (observerUUID == null) return;

        updateStatus(battle.getBattleId(), observerUUID, nickname, status);
    }

    private static void processCureStatus(PokemonBattle battle, String line) {
        // Format: |-curestatus|p1a: Pikachu|par
        String[] parts = line.split("\\|");
        if (parts.length < 3) return;

        String pokemonIdent = parts[2].trim();

        String side = extractSide(pokemonIdent);
        if (side == null) return;
        String nickname = extractNickname(pokemonIdent);

        UUID observerUUID = getOpponentPlayerUUID(battle, side);
        if (observerUUID == null) return;

        updateStatus(battle.getBattleId(), observerUUID, nickname, null);
    }

    private static void updateStatus(UUID battleId, UUID observerUUID, String nickname, String status) {
        Map<UUID, List<SeenPokemon>> battleData = battleTracking.get(battleId);
        if (battleData == null) return;
        List<SeenPokemon> seenList = battleData.get(observerUUID);
        if (seenList == null) return;

        for (SeenPokemon sp : seenList) {
            if (sp.getNickname().equals(nickname)) {
                if (status == null) {
                    sp.clearStatusCondition();
                } else {
                    sp.setStatusCondition(status);
                }
                break;
            }
        }
    }

    private static void updateHealth(UUID battleId, UUID observerUUID, String nickname, int healthPercent) {
        Map<UUID, List<SeenPokemon>> battleData = battleTracking.get(battleId);
        if (battleData == null) return;
        List<SeenPokemon> seenList = battleData.get(observerUUID);
        if (seenList == null) return;

        for (SeenPokemon sp : seenList) {
            if (sp.getNickname().equals(nickname)) {
                sp.setHealthPercent(healthPercent);
                break;
            }
        }
    }

    /**
     * Parse health percentage from a Showdown HP string like "48/100", "0 fnt", or "75/100 par".
     * Returns a percentage 0-100.
     */
    private static int parseHealthPercent(String hpString) {
        if (hpString.startsWith("0 ") || hpString.equals("0")) {
            return 0;
        }
        // Strip status condition (e.g. "48/100 par" -> "48/100")
        String hpPart = hpString.split(" ")[0];
        String[] hpParts = hpPart.split("/");
        if (hpParts.length == 2) {
            try {
                int current = Integer.parseInt(hpParts[0]);
                int max = Integer.parseInt(hpParts[1]);
                if (max > 0) {
                    return (current * 100) / max;
                }
            } catch (NumberFormatException ignored) {}
        }
        return 100;
    }

    /**
     * Extract status condition from a Showdown HP string like "48/100 par" -> "par".
     * Returns null if no status is present.
     */
    private static String parseStatusFromHpString(String hpString) {
        String[] spaceParts = hpString.split(" ");
        if (spaceParts.length >= 2) {
            String possibleStatus = spaceParts[1].trim();
            if (STATUS_DISPLAY_NAMES.containsKey(possibleStatus)) {
                return possibleStatus;
            }
        }
        return null;
    }

    /**
     * Extract side prefix ("p1" or "p2") from a pokemon identifier like "p1a: Pikachu"
     */
    private static String extractSide(String pokemonIdent) {
        if (pokemonIdent.length() >= 2 && pokemonIdent.startsWith("p")) {
            return pokemonIdent.substring(0, 2); // "p1" or "p2"
        }
        return null;
    }

    /**
     * Extract nickname from a pokemon identifier like "p1a: Pikachu" -> "Pikachu"
     */
    private static String extractNickname(String pokemonIdent) {
        int colonIndex = pokemonIdent.indexOf(':');
        if (colonIndex >= 0 && colonIndex + 1 < pokemonIdent.length()) {
            return pokemonIdent.substring(colonIndex + 1).trim();
        }
        return pokemonIdent;
    }

    /**
     * Given a side prefix (p1 or p2), return the UUID of the OPPONENT player.
     * If p1's pokemon is acting, p2 is observing (and vice versa).
     */
    private static UUID getOpponentPlayerUUID(PokemonBattle battle, String actorSide) {
        String opponentSide = actorSide.equals("p1") ? "p2" : "p1";
        for (BattleActor actor : battle.getActors()) {
            if (actor.getShowdownId().startsWith(opponentSide)) {
                // Return the first player UUID from this actor
                Iterator<UUID> it = actor.getPlayerUUIDs().iterator();
                if (it.hasNext()) return it.next();
            }
        }
        return null;
    }

    /**
     * Given a side prefix (p1 or p2), return the BattleActor for that side.
     */
    public static BattleActor getActorForSide(PokemonBattle battle, String side) {
        for (BattleActor actor : battle.getActors()) {
            if (actor.getShowdownId().startsWith(side)) {
                return actor;
            }
        }
        return null;
    }
}
