package com.turtlehoarder.cobblemonchallenge.command;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.turtlehoarder.cobblemonchallenge.config.ChallengeConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.turtlehoarder.cobblemonchallenge.gui.LeadPokemonMenuProvider;
import com.turtlehoarder.cobblemonchallenge.util.ChallengeUtil;
import com.turtlehoarder.cobblemonchallenge.gui.LeadPokemonSelectionSession;
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
import java.util.UUID;

public class ChallengeCommand {

    public record ChallengeRequest(String id, ServerPlayer challengerPlayer, ServerPlayer challengedPlayer, int minLevel, int maxLevel, int handicapP1, int handicapP2, boolean preview, long createdTime) {}
    public record LeadPokemonSelection(LeadPokemonSelectionSession selectionWrapper, long createdTime) {}
    private static final float MAX_DISTANCE = ChallengeConfig.MAX_CHALLENGE_DISTANCE;
    private static final boolean USE_DISTANCE_RESTRICTION = ChallengeConfig.CHALLENGE_DISTANCE_RESTRICTION;
    private static final int DEFAULT_LEVEL = ChallengeConfig.DEFAULT_CHALLENGE_LEVEL;
    private static final int DEFAULT_HANDICAP = ChallengeConfig.DEFAULT_HANDICAP;
    private static final int CHALLENGE_COOLDOWN = ChallengeConfig.CHALLENGE_COOLDOWN_MILLIS;
    public static HashMap<String, ChallengeRequest> CHALLENGE_REQUESTS = new HashMap<>();
    public static final HashMap<UUID, LeadPokemonSelection> ACTIVE_SELECTIONS = new HashMap<>();
    private static final HashMap<UUID, Long> LAST_SENT_CHALLENGE = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Overview:
        //      > always player name with level or min/maxLevel are mutually exclusive
        //      > 12 command trees
        //      > further additions may need a UI implementation to refine/make more user friendly

        // (default everything)
        // handicap
        // no preview
        // handicap + no preview

        // level
        // level + handicap
        // level + no preview
        // level + handicap + no preview

        // min/max
        // min/max + handicap
        // min/max + no preview
        // min/max + handicap + no preview

        // (default everything)
        LiteralArgumentBuilder<CommandSourceStack> defaultChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> challengePlayer(c, DEFAULT_LEVEL, DEFAULT_LEVEL, DEFAULT_HANDICAP, DEFAULT_HANDICAP, true))
                );

        // handicap
        LiteralArgumentBuilder<CommandSourceStack> handicapChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("handicapP1")
                                .then(Commands.argument("setP1HandicapTo", IntegerArgumentType.integer(-99,99))
                                        .then(Commands.literal("handicapP2")
                                                .then(Commands.argument("setP2HandicapTo", IntegerArgumentType.integer(-99,99))
                                                        .executes(c -> challengePlayer(c,  DEFAULT_LEVEL, DEFAULT_LEVEL, IntegerArgumentType.getInteger(c, "setP1HandicapTo"), IntegerArgumentType.getInteger(c, "setP2HandicapTo"), true))
                                                )

                                        )
                                )
                        )
                );

        // no preview
        LiteralArgumentBuilder<CommandSourceStack> noPreviewChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("nopreview")
                                .executes(c -> challengePlayer(c, DEFAULT_LEVEL, DEFAULT_LEVEL, DEFAULT_HANDICAP, DEFAULT_HANDICAP, false))
                        )
                );

        // handicap + no preview
        LiteralArgumentBuilder<CommandSourceStack> handicapNoPreviewChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("handicapP1")
                                .then(Commands.argument("setP1HandicapTo", IntegerArgumentType.integer(-99,99))
                                        .then(Commands.literal("handicapP2")
                                                .then(Commands.argument("setP2HandicapTo", IntegerArgumentType.integer(-99,99))
                                                        .then(Commands.literal("nopreview")
                                                                .executes(c -> challengePlayer(c,  DEFAULT_LEVEL, DEFAULT_LEVEL, IntegerArgumentType.getInteger(c, "setP1HandicapTo"), IntegerArgumentType.getInteger(c, "setP2HandicapTo"), false))
                                                        )
                                                )
                                        )
                                )
                        )
                );

        // level
        LiteralArgumentBuilder<CommandSourceStack> levelChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("level")
                                .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                        .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"), IntegerArgumentType.getInteger(c, "setLevelTo"), DEFAULT_HANDICAP, DEFAULT_HANDICAP, true))

                                )
                        )
                );

        // level + handicap
        LiteralArgumentBuilder<CommandSourceStack> levelHandicapChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("level")
                                .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("handicapP1")
                                                .then(Commands.argument("setP1HandicapTo", IntegerArgumentType.integer(-99,99))
                                                        .then(Commands.literal("handicapP2")
                                                                .then(Commands.argument("setP2HandicapTo", IntegerArgumentType.integer(-99,99))
                                                                        .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"), IntegerArgumentType.getInteger(c, "setLevelTo"), IntegerArgumentType.getInteger(c, "setP1HandicapTo"), IntegerArgumentType.getInteger(c, "setP2HandicapTo"), true))
                                                                )
                                                        )
                                                )
                                        )

                                )
                        )
                );

        // level + no preview
        LiteralArgumentBuilder<CommandSourceStack> levelNoPreviewChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("level")
                                .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("nopreview")
                                                .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"), IntegerArgumentType.getInteger(c, "setLevelTo"), DEFAULT_HANDICAP, DEFAULT_HANDICAP, false))
                                        )

                                )
                        )
                );

        // level + handicap + no preview
        LiteralArgumentBuilder<CommandSourceStack> levelHandicapNoPreviewChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("level")
                                .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("handicapP1")
                                                .then(Commands.argument("setP1HandicapTo", IntegerArgumentType.integer(-99,99))
                                                        .then(Commands.literal("handicapP2")
                                                                .then(Commands.argument("setP2HandicapTo", IntegerArgumentType.integer(-99,99))
                                                                        .then(Commands.literal("nopreview")
                                                                                .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"), IntegerArgumentType.getInteger(c, "setLevelTo"), IntegerArgumentType.getInteger(c, "setP1HandicapTo"), IntegerArgumentType.getInteger(c, "setP2HandicapTo"), false))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )

                                )
                        )
                );

        // min/max
        LiteralArgumentBuilder<CommandSourceStack> minMaxLevelChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("minLevel")
                                .then(Commands.argument("setMinLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("maxLevel")
                                                .then(Commands.argument("setMaxLevelTo", IntegerArgumentType.integer(1,100))
                                                        .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setMinLevelTo"), IntegerArgumentType.getInteger(c, "setMaxLevelTo"), DEFAULT_HANDICAP, DEFAULT_HANDICAP, true))
                                                )
                                        )

                                )
                        )
                );

        // min/max + handicap
        LiteralArgumentBuilder<CommandSourceStack> minMaxLevelHandicapChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("minLevel")
                                .then(Commands.argument("setMinLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("maxLevel")
                                                .then(Commands.argument("setMaxLevelTo", IntegerArgumentType.integer(1,100))
                                                        .then(Commands.literal("handicapP1")
                                                                .then(Commands.argument("setP1HandicapTo", IntegerArgumentType.integer(-99,99))
                                                                        .then(Commands.literal("handicapP2")
                                                                                .then(Commands.argument("setP2HandicapTo", IntegerArgumentType.integer(-99,99))
                                                                                        .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setMinLevelTo"), IntegerArgumentType.getInteger(c, "setMaxLevelTo"), IntegerArgumentType.getInteger(c, "setP1HandicapTo"), IntegerArgumentType.getInteger(c, "setP2HandicapTo"), true))
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )

                                )
                        )
                );

        // min/max + no preview
        LiteralArgumentBuilder<CommandSourceStack> minMaxLevelNoPreviewChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("minLevel")
                                .then(Commands.argument("setMinLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("maxLevel")
                                                .then(Commands.argument("setMaxLevelTo", IntegerArgumentType.integer(1,100))
                                                        .then(Commands.literal("nopreview")
                                                                .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setMinLevelTo"), IntegerArgumentType.getInteger(c, "setMaxLevelTo"), DEFAULT_HANDICAP, DEFAULT_HANDICAP, false))
                                                        )
                                                )
                                        )

                                )
                        )
                );

        // min/max + handicap + no preview
        LiteralArgumentBuilder<CommandSourceStack> minMaxLevelHandicapNoPreviewChallengeProperties = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("minLevel")
                                .then(Commands.argument("setMinLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("maxLevel")
                                                .then(Commands.argument("setMaxLevelTo", IntegerArgumentType.integer(1,100))
                                                        .then(Commands.literal("handicapP1")
                                                                .then(Commands.argument("setP1HandicapTo", IntegerArgumentType.integer(-99,99))
                                                                        .then(Commands.literal("handicapP2")
                                                                                .then(Commands.argument("setP2HandicapTo", IntegerArgumentType.integer(-99,99))
                                                                                        .then(Commands.literal("nopreview")
                                                                                                .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setMinLevelTo"), IntegerArgumentType.getInteger(c, "setMaxLevelTo"), IntegerArgumentType.getInteger(c, "setP1HandicapTo"), IntegerArgumentType.getInteger(c, "setP2HandicapTo"), false))
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )

                                )
                        )
                );

        // Command called to accept challenges
        LiteralArgumentBuilder<CommandSourceStack> acceptChallengeAndProperties = Commands.literal("acceptchallenge")
                .then(Commands.argument("id", StringArgumentType.string()).executes(c -> acceptChallenge(c, StringArgumentType.getString(c, "id"))));
        // Command called to deny challenges
        LiteralArgumentBuilder<CommandSourceStack> rejectChallengeAndProperties = Commands.literal("rejectchallenge")
                .then(Commands.argument("id", StringArgumentType.string()).executes(c -> rejectChallenge(c, StringArgumentType.getString(c, "id"))));


        dispatcher.register(acceptChallengeAndProperties);
        dispatcher.register(rejectChallengeAndProperties);

        // 12 possible Challenge Properties Commands
        dispatcher.register(defaultChallengeProperties);
        dispatcher.register(handicapChallengeProperties);
        dispatcher.register(noPreviewChallengeProperties);
        dispatcher.register(handicapNoPreviewChallengeProperties);

        dispatcher.register(levelChallengeProperties);
        dispatcher.register(levelHandicapChallengeProperties);
        dispatcher.register(levelNoPreviewChallengeProperties);
        dispatcher.register(levelHandicapNoPreviewChallengeProperties);

        dispatcher.register(minMaxLevelChallengeProperties);
        dispatcher.register(minMaxLevelHandicapChallengeProperties);
        dispatcher.register(minMaxLevelNoPreviewChallengeProperties);
        dispatcher.register(minMaxLevelHandicapNoPreviewChallengeProperties);

    }

    public static int challengePlayer(CommandContext<CommandSourceStack> c, int minLevel, int maxLevel, int handicapP1, int handicapP2, boolean preview) {
        try {
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

            // make sure the min max range contains at least one functional value
            // > defaults to maxLevel
            if (minLevel > maxLevel){
                minLevel = maxLevel;
            }

            ChallengeRequest request = ChallengeUtil.createChallengeRequest(challengerPlayer, challengedPlayer, minLevel, maxLevel, handicapP1, handicapP2, preview);
            CHALLENGE_REQUESTS.put(request.id, request);

            String levelComponent = (minLevel == maxLevel) ? ChatFormatting.YELLOW + String.format("You have been challenged to a " + ChatFormatting.BOLD + "level %d Pokemon battle", maxLevel) : ChatFormatting.YELLOW + String.format("You have been challenged to a " + ChatFormatting.BOLD + "level %d - %d Pokemon battle", minLevel, maxLevel);
            String challengerComponent = ChatFormatting.YELLOW + " by " + challengerPlayer.getDisplayName().getString() + "!";
            String optionsComponent = request.preview() ? "" : ChatFormatting.RED + " [NoTeamPreview]";
            String handicapComponent = (handicapP1 == 0 && handicapP2 == 0) ? "" : ChatFormatting.BLUE + " [" + challengerPlayer.getDisplayName().getString() + " handicap of " + handicapP1 + "] [" + challengedPlayer.getDisplayName().getString() + " handicap of " + handicapP2 + "]";
            MutableComponent notificationComponent = Component.literal(levelComponent + challengerComponent + optionsComponent + handicapComponent);

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


}