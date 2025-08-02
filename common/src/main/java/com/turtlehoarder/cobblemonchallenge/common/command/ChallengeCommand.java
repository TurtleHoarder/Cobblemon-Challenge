package com.turtlehoarder.cobblemonchallenge.common.command;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.battle.ChallengeFormat;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
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
import java.util.UUID;

public class ChallengeCommand {

    public record ChallengeRequest(String id, ServerPlayer challengerPlayer, ServerPlayer challengedPlayer, int level, boolean preview, long createdTime, ChallengeFormat format) {}
    public record LeadPokemonSelection(LeadPokemonSelectionSession selectionWrapper, long createdTime) {}

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
    }

    private static void registerAcceptDenyCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Command called to accept challenges
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderAcceptChallenge = Commands.literal("acceptchallenge")
                .then(Commands.argument("id", StringArgumentType.string()).executes(c -> acceptChallenge(c, StringArgumentType.getString(c, "id"))));
        // Command called to deny challenges
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderRejectChallenge = Commands.literal("rejectchallenge")
                .then(Commands.argument("id", StringArgumentType.string()).executes(c -> rejectChallenge(c, StringArgumentType.getString(c, "id"))));
        dispatcher.register(commandBuilderAcceptChallenge);
        dispatcher.register(commandBuilderRejectChallenge);
    }

    private static void registerChallengeFormatCommand(CommandDispatcher<CommandSourceStack> dispatcher, String baseCommand, ChallengeFormat resultingFormat) {
        // Basic challenge command that initiates a challenge with the default challenge level
        LiteralArgumentBuilder<CommandSourceStack> baseCommandBuilder = Commands.literal(baseCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> challengePlayer(c, DEFAULT_LEVEL, true, resultingFormat)));

        // Basic challenge command that initiates a challenge with the default challenge level
        LiteralArgumentBuilder<CommandSourceStack> baseCommandBuilderNoPreview = Commands.literal(baseCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("nopreview")
                                .executes(c -> challengePlayer(c, DEFAULT_LEVEL, false, resultingFormat))));


        // Challenge command that initiates a challenge with a given level
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderWithLevelOption = Commands.literal(baseCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("level")
                                .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                        .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"), true, resultingFormat)
                                        )
                                )
                        )
                );
        // Challenge command that initiates a challenge with a given level
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderWithLevelOptionNoPreview = Commands.literal(baseCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("level")
                                .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                        .then(Commands.literal("nopreview")
                                                .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"), false, resultingFormat)
                                                )
                                        )
                                )
                        )
                );

        // Challenge command that initiates a challenge with a given level
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderWithLevelOptionNoPreviewBefore = Commands.literal(baseCommand)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("nopreview")
                                .then(Commands.literal("level")
                                        .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                                .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"), false, resultingFormat)
                                                )
                                        )
                                )
                        )
                );
        dispatcher.register(commandBuilderWithLevelOption);
        // Register nopreview section
        dispatcher.register(commandBuilderWithLevelOptionNoPreview);
        dispatcher.register(baseCommandBuilderNoPreview);
        dispatcher.register(commandBuilderWithLevelOptionNoPreviewBefore);
        dispatcher.register(baseCommandBuilder);
    }

    public static int challengePlayer(CommandContext<CommandSourceStack> c, int level, boolean preview, ChallengeFormat format) {
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

            String options = "";
            if (!request.preview()) {
                options = ChatFormatting.GOLD + " [NoTeamPreview]";
            }
            MutableComponent notificationComponent = Component.literal(ChatFormatting.YELLOW + String.format("You have been challenged to a " + ChatFormatting.BOLD + "level %d %s Pokemon battle" + ChatFormatting.RESET + ChatFormatting.YELLOW + " by %s!" + options, level, request.format.getTitle(), challengerPlayer.getDisplayName().getString()));            MutableComponent interactiveComponent = Component.literal("Click to accept or deny: ");
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


}