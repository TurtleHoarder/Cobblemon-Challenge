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

    public record ChallengeRequest(String id, ServerPlayer challengerPlayer, ServerPlayer challengedPlayer, int level, long createdTime) {}
    public record LeadPokemonSelection(LeadPokemonSelectionSession selectionWrapper, long createdTime) {}

    private static final float MAX_DISTANCE = ChallengeConfig.MAX_CHALLENGE_DISTANCE.get();
    private static final boolean USE_DISTANCE_RESTRICTION = ChallengeConfig.CHALLENGE_DISTANCE_RESTRICTION.get();
    private static final int DEFAULT_LEVEL = ChallengeConfig.DEFAULT_CHALLENGE_LEVEL.get();
    private static final int CHALLENGE_COOLDOWN = ChallengeConfig.CHALLENGE_COOLDOWN_MILLIS.get();
    public static HashMap<String, ChallengeRequest> CHALLENGE_REQUESTS = new HashMap<>();
    public static final HashMap<UUID, LeadPokemonSelection> ACTIVE_SELECTIONS = new HashMap<>();
    private static final HashMap<UUID, Long> LAST_SENT_CHALLENGE = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // Basic challenge command that initiates a challenge with the default challenge level
        LiteralArgumentBuilder<CommandSourceStack> baseCommandBuilder = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> challengePlayer(c, DEFAULT_LEVEL)));

        // Challenge command that initiates a challenge with a given level
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderWithLevelOption = Commands.literal("challenge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("level")
                            .then(Commands.argument("setLevelTo", IntegerArgumentType.integer(1,100))
                                 .executes(c -> challengePlayer(c, IntegerArgumentType.getInteger(c, "setLevelTo"))
                             )
                        )
                    )
                );

        // Command called to accept challenges
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderAcceptChallenge = Commands.literal("acceptchallenge")
                        .then(Commands.argument("id", StringArgumentType.string()).executes(c -> acceptChallenge(c, StringArgumentType.getString(c, "id"))));
        // Command called to deny challenges
        LiteralArgumentBuilder<CommandSourceStack> commandBuilderRejectChallenge = Commands.literal("rejectchallenge")
                .then(Commands.argument("id", StringArgumentType.string()).executes(c -> rejectChallenge(c, StringArgumentType.getString(c, "id"))));


        dispatcher.register(commandBuilderAcceptChallenge);
        dispatcher.register(commandBuilderRejectChallenge);
        dispatcher.register(baseCommandBuilder);
        dispatcher.register(commandBuilderWithLevelOption);
    }

    public static int challengePlayer(CommandContext<CommandSourceStack> c, int level) {
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
            if (USE_DISTANCE_RESTRICTION && (distance > MAX_DISTANCE || challengedPlayer.getLevel() != challengerPlayer.getLevel())) {
                c.getSource().sendFailure(Component.literal(String.format("Target must be less than %d blocks away to initiate a challenge", (int)MAX_DISTANCE)));
                return 0;
            }

            if (challengerPlayer == challengedPlayer) {
                c.getSource().sendFailure(Component.literal("You may not challenge yourself"));
                return 0;
            }

            ChallengeRequest request = ChallengeUtil.createChallengeRequest(challengerPlayer, challengedPlayer, level);
            CHALLENGE_REQUESTS.put(request.id, request);

            MutableComponent notificationComponent = Component.literal(ChatFormatting.YELLOW + String.format("You have been challenged to a " + ChatFormatting.BOLD + "level %d Pokemon battle" + ChatFormatting.RESET + ChatFormatting.YELLOW + " by %s!", level, challengerPlayer.getDisplayName().getString()));
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
            c.getSource().sendFailure(Component.literal("An unexpected error has occurred when sending challenge: " + e.getMessage()));
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
            if (USE_DISTANCE_RESTRICTION && (distance > MAX_DISTANCE || request.challengerPlayer.getLevel() != request.challengedPlayer.getLevel())) {
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