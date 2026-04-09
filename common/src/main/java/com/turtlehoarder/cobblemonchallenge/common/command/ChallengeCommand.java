package com.turtlehoarder.cobblemonchallenge.common.command;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.battle.BattlePeekTracker;
import com.turtlehoarder.cobblemonchallenge.common.battle.ChallengeFormat;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
import com.turtlehoarder.cobblemonchallenge.common.gui.BattlePeekMenuProvider;
import com.turtlehoarder.cobblemonchallenge.common.gui.LeadPokemonSelectionSession;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChallengeCommand {

    public record ChallengeRequest(String id, ServerPlayer challengerPlayer, ServerPlayer challengedPlayer, int level, boolean preview, long createdTime, ChallengeFormat format) {}
    public record LeadPokemonSelection(LeadPokemonSelectionSession selectionWrapper, long createdTime) {}

    /**
     * Holds parsed challenge options. Add new fields here to extend functionality.
     */
    public static class ChallengeOptions {
        private int level;
        private boolean preview = true;
        private String parseError = null;

        public ChallengeOptions(int defaultLevel) {
            this.level = defaultLevel;
        }

        public int getLevel() { return level; }
        public boolean hasPreview() { return preview; }
        public String getParseError() { return parseError; }
        public boolean hasError() { return parseError != null; }

        /**
         * Parse tokens like "level 50 nopreview" in any order.
         * Add new option keywords to the switch statement to extend.
         */
        public static ChallengeOptions parse(String input, int defaultLevel) {
            ChallengeOptions options = new ChallengeOptions(defaultLevel);
            if (input == null || input.isBlank()) return options;

            String[] tokens = input.trim().split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].toLowerCase();
                switch (token) {
                    case "nopreview" -> options.preview = false;
                    case "level" -> {
                        if (i + 1 < tokens.length) {
                            try {
                                int lvl = Integer.parseInt(tokens[++i]);
                                if (lvl < 1 || lvl > 100) {
                                    options.parseError = "Level must be between 1 and 100";
                                    return options;
                                }
                                options.level = lvl;
                            } catch (NumberFormatException e) {
                                options.parseError = "Invalid level number: " + tokens[i];
                                return options;
                            }
                        } else {
                            options.parseError = "Missing level value after 'level'";
                            return options;
                        }
                    }
                    // Add future options here:
                    // case "handicap" -> { ... }
                    // case "timed" -> { ... }
                    default -> {
                        // Ignore unknown tokens to allow for typos or future compatibility
                    }
                }
            }
            return options;
        }
    }

    /**
     * Provides context-aware autocomplete suggestions for challenge options.
     * Suggests keywords that haven't been used yet in the current input.
     */
    public static class ChallengeOptionsSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        private static final Set<String> KEYWORDS = Set.of("level", "nopreview");

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            String remaining = builder.getRemaining();
            String remainingLower = remaining.toLowerCase();

            // Determine which keywords have already been used via exact token matching
            String[] allTokens = remainingLower.trim().isEmpty() ? new String[0] : remainingLower.trim().split("\\s+");
            Set<String> usedKeywords = new HashSet<>();
            for (String keyword : KEYWORDS) {
                for (String token : allTokens) {
                    if (token.equals(keyword)) {
                        usedKeywords.add(keyword);
                    }
                }
            }

            // Create a builder offset to the last word boundary so suggestions only replace the current token
            int lastSpaceInInput = builder.getInput().lastIndexOf(' ');
            SuggestionsBuilder tokenBuilder = builder.createOffset(Math.max(lastSpaceInInput + 1, builder.getStart()));
            String currentToken = tokenBuilder.getRemaining().toLowerCase();

            // Determine the previous completed token
            boolean endsWithSpace = remaining.endsWith(" ");
            String prevToken = "";
            if (endsWithSpace && allTokens.length >= 1) {
                prevToken = allTokens[allTokens.length - 1];
            } else if (!endsWithSpace && allTokens.length >= 2) {
                prevToken = allTokens[allTokens.length - 2];
            }

            // If previous token was "level", suggest example level numbers
            if (prevToken.equals("level")) {
                tokenBuilder.suggest("100");
                tokenBuilder.suggest("50");
                tokenBuilder.suggest("1");
                return tokenBuilder.buildFuture();
            }

            // Suggest unused keywords matching current partial input
            for (String keyword : KEYWORDS) {
                if (!usedKeywords.contains(keyword) && keyword.startsWith(currentToken)) {
                    tokenBuilder.suggest(keyword);
                }
            }

            return tokenBuilder.buildFuture();
        }
    }

    private static final float MAX_DISTANCE = CobblemonChallenge.MAX_CHALLENGE_DISTANCE;
    private static final boolean USE_DISTANCE_RESTRICTION = CobblemonChallenge.CHALLENGE_DISTANCE_RESTRICTION;
    private static final int DEFAULT_LEVEL = CobblemonChallenge.DEFAULT_CHALLENGE_LEVEL;
    private static final int CHALLENGE_COOLDOWN = CobblemonChallenge.CHALLENGE_COOLDOWN_MILLIS;
    public static HashMap<String, ChallengeRequest> CHALLENGE_REQUESTS = new HashMap<>();
    public static final HashMap<UUID, LeadPokemonSelection> ACTIVE_SELECTIONS = new HashMap<>();
    private static final HashMap<UUID, Long> LAST_SENT_CHALLENGE = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerChallengeFormatCommand(dispatcher, "challenge", ChallengeFormat.STANDARD_6V6);
        registerChallengeFormatCommand(dispatcher, "challenge1v1", ChallengeFormat.STANDARD_1V1);
        registerChallengeFormatCommand(dispatcher, "challenge2v2", ChallengeFormat.STANDARD_2V2);
        registerChallengeFormatCommand(dispatcher, "challenge3v3", ChallengeFormat.STANDARD_3V3);
        registerChallengeFormatCommand(dispatcher, "challenge4v4", ChallengeFormat.STANDARD_4V4);
        registerChallengeFormatCommand(dispatcher, "challenge5v5", ChallengeFormat.STANDARD_5V5);
        registerChallengeFormatCommand(dispatcher, "challenge6v6", ChallengeFormat.STANDARD_6V6);
        registerChallengeFormatCommand(dispatcher, "challengedouble", ChallengeFormat.STANDARD_DOUBLES_6V6);
        registerChallengeFormatCommand(dispatcher, "challengedouble2v2", ChallengeFormat.STANDARD_DOUBLES_2v2);
        registerChallengeFormatCommand(dispatcher, "challengedouble3v3", ChallengeFormat.STANDARD_DOUBLES_3v3);
        registerChallengeFormatCommand(dispatcher, "challengedouble4v4", ChallengeFormat.STANDARD_DOUBLES_4v4);
        registerChallengeFormatCommand(dispatcher, "challengedouble5v5", ChallengeFormat.STANDARD_DOUBLES_5V5);
        registerChallengeFormatCommand(dispatcher, "challengedouble6v6", ChallengeFormat.STANDARD_DOUBLES_6V6);

        registerAcceptDenyCommands(dispatcher);

        dispatcher.register(Commands.literal("challengepeek")
                .executes(ChallengeCommand::peekOpponentTeam));
    }

    private static void registerAcceptDenyCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Command called to accept challenges
        dispatcher.register(Commands.literal("acceptchallenge")
                .then(Commands.argument("id", StringArgumentType.string())
                        .executes(c -> acceptChallenge(c, StringArgumentType.getString(c, "id")))));

        // Command called to deny challenges
        dispatcher.register(Commands.literal("rejectchallenge")
                .then(Commands.argument("id", StringArgumentType.string())
                        .executes(c -> rejectChallenge(c, StringArgumentType.getString(c, "id")))));
    }

    private static final SuggestionProvider<CommandSourceStack> OPTIONS_SUGGESTIONS = new ChallengeOptionsSuggestionProvider();

    private static void registerChallengeFormatCommand(CommandDispatcher<CommandSourceStack> dispatcher, String baseCommand, ChallengeFormat resultingFormat) {
        dispatcher.register(Commands.literal(baseCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        // No options - use defaults
                        .executes(c -> challengePlayer(c, ChallengeOptions.parse(null, DEFAULT_LEVEL), resultingFormat))
                        // Optional greedy string for options in any order (e.g., "level 50 nopreview")
                        .then(Commands.argument("options", StringArgumentType.greedyString())
                                .suggests(OPTIONS_SUGGESTIONS)
                                .executes(c -> {
                                    String optionsStr = StringArgumentType.getString(c, "options");
                                    ChallengeOptions options = ChallengeOptions.parse(optionsStr, DEFAULT_LEVEL);
                                    return challengePlayer(c, options, resultingFormat);
                                })
                        )
                )
        );
    }

    public static int challengePlayer(CommandContext<CommandSourceStack> c, ChallengeOptions options, ChallengeFormat format) {
        try {
            // Check for parsing errors first
            if (options.hasError()) {
                c.getSource().sendFailure(Component.literal(options.getParseError()));
                return 0;
            }

            int level = options.getLevel();
            boolean preview = options.hasPreview();

            ServerPlayer challengerPlayer = c.getSource().getPlayer();
            ServerPlayer challengedPlayer = c.getArgument("player", EntitySelector.class).findSinglePlayer(c.getSource());

            if (LAST_SENT_CHALLENGE.containsKey(challengerPlayer.getUUID())) {
                if (System.currentTimeMillis() - LAST_SENT_CHALLENGE.get(challengerPlayer.getUUID()) < CHALLENGE_COOLDOWN) {
                    c.getSource().sendFailure(Component.literal(String.format("You must wait at least %d second(s) before sending another challenge", (int)Math.ceil(CHALLENGE_COOLDOWN / 1000f))));
                    return 0;
                }
            }

            for (ChallengeRequest request : CHALLENGE_REQUESTS.values()) {
                if (request.challengerPlayer.getUUID().equals(challengerPlayer.getUUID())) {
                    c.getSource().sendFailure(Component.literal(String.format("You already have a pending challenge to %s", request.challengedPlayer.getDisplayName().getString())));
                    return 0;
                }
            }

            BattleRegistry br = Cobblemon.INSTANCE.getBattleRegistry();
            if (br.getBattleByParticipatingPlayer(challengerPlayer) != null) {
                c.getSource().sendFailure(Component.literal("Cannot send challenge while in-battle"));
                return 0;
            }

            if (Cobblemon.INSTANCE.getStorage().getParty(challengerPlayer).occupied() < format.getTotalPokemonSelected()) {
                c.getSource().sendFailure(Component.literal("You don't have enough pokemon for this format!"));
                return 0;
            }

            if (Cobblemon.INSTANCE.getStorage().getParty(challengerPlayer).occupied() == 0) {
                c.getSource().sendFailure(Component.literal("Cannot send challenge while you have no pokemon!"));
                return 0;
            }


            float distance = challengedPlayer.distanceTo(challengerPlayer);
            if (USE_DISTANCE_RESTRICTION && (distance > MAX_DISTANCE || challengedPlayer.level() != challengerPlayer.level())) {
                c.getSource().sendFailure(Component.literal(String.format("Target must be less than %d blocks away to initiate a challenge", (int)MAX_DISTANCE)));
                return 0;
            }

            if (challengerPlayer == challengedPlayer) {
                c.getSource().sendFailure(Component.literal("You may not challenge yourself"));
                return 0;
            }

            ChallengeRequest request = ChallengeUtil.createChallengeRequest(challengerPlayer, challengedPlayer, level, preview, format);
            CHALLENGE_REQUESTS.put(request.id, request);

            String optionsText = "";
            if (!request.preview()) {
                optionsText = ChatFormatting.GOLD + " [NoTeamPreview]";
            }
            MutableComponent notificationComponent = Component.literal(ChatFormatting.YELLOW + String.format("You have been challenged to a " + ChatFormatting.BOLD + "level %d %s Pokemon battle" + ChatFormatting.RESET + ChatFormatting.YELLOW + " by %s!" + optionsText, level, request.format.getTitle(), challengerPlayer.getDisplayName().getString()));
            MutableComponent interactiveComponent = Component.literal("Click to accept or deny: ");
            interactiveComponent.append(Component.literal(ChatFormatting.GREEN + "Battle!").setStyle(Style.EMPTY.withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/acceptchallenge %s", request.id)))));
            interactiveComponent.append(Component.literal(" or "));
            interactiveComponent.append(Component.literal(ChatFormatting.RED + "Reject").setStyle(Style.EMPTY.withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/rejectchallenge %s", request.id)))));
            challengedPlayer.displayClientMessage(notificationComponent, false);
            challengedPlayer.displayClientMessage(interactiveComponent, false);
            challengerPlayer.displayClientMessage(Component.literal(ChatFormatting.YELLOW + String.format("Challenge has been sent to %s", challengedPlayer.getDisplayName().getString())), false);
            LAST_SENT_CHALLENGE.put(challengerPlayer.getUUID(), System.currentTimeMillis());
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            c.getSource().sendFailure(Component.literal("An unexpected error has occurred: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    public static int rejectChallenge(CommandContext<CommandSourceStack> c, String challengeId) {
        try {
            ChallengeRequest request = CHALLENGE_REQUESTS.get(challengeId);
            if (request == null) {
                c.getSource().sendFailure(Component.literal("Challenge request is not valid"));
                return 0;
            }
            CHALLENGE_REQUESTS.remove(request.id);
            request.challengedPlayer.displayClientMessage(Component.literal(ChatFormatting.RED + "Challenge has been rejected"), false);

            if (ChallengeUtil.isPlayerOnline(request.challengerPlayer)) {
                request.challengerPlayer.displayClientMessage(Component.literal(ChatFormatting.RED + String.format("%s has rejected your challenge.", request.challengedPlayer.getDisplayName().getString())), false);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            c.getSource().sendFailure(Component.literal("An unexpected error has occurred: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    public static int acceptChallenge(CommandContext<CommandSourceStack> c, String challengeId) {
        try {
            ChallengeRequest request = CHALLENGE_REQUESTS.get(challengeId);
            if (request == null) {
                c.getSource().sendFailure(Component.literal("Challenge request is not valid"));
                return 0;
            }

            BattleRegistry br = Cobblemon.INSTANCE.getBattleRegistry();
            if (br.getBattleByParticipatingPlayer(request.challengedPlayer) != null) {
                c.getSource().sendFailure(Component.literal("Cannot accept challenge: you are already in a battle"));
                return 0;
            }
            else if (br.getBattleByParticipatingPlayer(request.challengerPlayer) != null) {
                c.getSource().sendFailure(Component.literal(String.format("Cannot accept challenge: %s is already in a battle", request.challengerPlayer.getDisplayName().getString())));
                return 0;
            }

            if (Cobblemon.INSTANCE.getStorage().getParty(request.challengedPlayer).occupied() == 0) {
                c.getSource().sendFailure(Component.literal("Cannot accept challenge: You have no pokemon!"));
                return 0;
            }

            if (Cobblemon.INSTANCE.getStorage().getParty(request.challengerPlayer).occupied() == 0) {
                c.getSource().sendFailure(Component.literal(String.format("Cannot accept challenge: %s has no pokemon... somehow!", request.challengerPlayer.getDisplayName().getString())));
                return 0;
            }

            if (Cobblemon.INSTANCE.getStorage().getParty(request.challengedPlayer).occupied() < request.format.getTotalPokemonSelected()) {
                c.getSource().sendFailure(Component.literal("Cannot accept challenge: You don't have enough pokemon for this format!"));
                return 0;
            }

            float distance = request.challengerPlayer.distanceTo(request.challengedPlayer);
            if (USE_DISTANCE_RESTRICTION && (distance > MAX_DISTANCE || request.challengerPlayer.level() != request.challengedPlayer.level())) {
                c.getSource().sendFailure(Component.literal(String.format("Target must be less than %d blocks away to accept a challenge", (int)MAX_DISTANCE)));
                return 0;
            }
            ChallengeRequest challengeRequestRemoved = CHALLENGE_REQUESTS.remove(challengeId);
            ServerPlayer challengerPlayer = request.challengerPlayer;

            if (!ChallengeUtil.isPlayerOnline(challengerPlayer)) {
                c.getSource().sendFailure(Component.literal(String.format("%s is no longer online", challengerPlayer.getDisplayName().getString())));
                return 0;
            }
            setupLeadPokemonFlow(challengeRequestRemoved);
            return Command.SINGLE_SUCCESS;
        } catch (Exception exc) {
            c.getSource().sendFailure(Component.literal("Unexpected exception when accepting challenge: " + exc.getMessage()));
            exc.printStackTrace();
            return 1;
        }
    }

    private static void setupLeadPokemonFlow(ChallengeRequest request) {
        // Register the selection process for tracking purposes
        UUID selectionId = UUID.randomUUID();
        long creationTime = System.currentTimeMillis();
        LeadPokemonSelectionSession selectionWrapper = new LeadPokemonSelectionSession(selectionId, creationTime, request);
        ACTIVE_SELECTIONS.put(selectionId, new LeadPokemonSelection(selectionWrapper, creationTime));
        selectionWrapper.openPlayerMenus(); // Force both players to open their menus
    }

    public static int peekOpponentTeam(CommandContext<CommandSourceStack> c) {
        try {
            ServerPlayer player = c.getSource().getPlayer();
            if (player == null) {
                c.getSource().sendFailure(Component.literal("This command can only be used by a player"));
                return 0;
            }

            BattleRegistry br = Cobblemon.INSTANCE.getBattleRegistry();
            PokemonBattle battle = br.getBattleByParticipatingPlayer(player);
            if (battle == null) {
                c.getSource().sendFailure(Component.literal("You are not currently in a battle"));
                return 0;
            }

            if (!ChallengeUtil.isBattleChallenge(battle.getBattleId())) {
                c.getSource().sendFailure(Component.literal("This command can only be used during a challenge battle"));
                return 0;
            }

            // Find the opponent actor
            BattleActor opponentActor = null;
            for (BattleActor actor : battle.getActors()) {
                boolean isPlayerActor = false;
                for (UUID uuid : actor.getPlayerUUIDs()) {
                    if (uuid.equals(player.getUUID())) {
                        isPlayerActor = true;
                        break;
                    }
                }
                if (!isPlayerActor) {
                    opponentActor = actor;
                    break;
                }
            }

            List<BattlePeekTracker.SeenPokemon> seenPokemon = BattlePeekTracker.getSeenPokemon(battle.getBattleId(), player.getUUID());
            if (seenPokemon.isEmpty()) {
                c.getSource().sendFailure(Component.literal("No opponent pokemon have been revealed yet"));
                return 0;
            }

            player.openMenu(new BattlePeekMenuProvider(seenPokemon, opponentActor));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            c.getSource().sendFailure(Component.literal("An unexpected error has occurred: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

}