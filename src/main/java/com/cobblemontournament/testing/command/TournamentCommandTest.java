package com.cobblemontournament.testing.command;

import com.cobblemontournament.testing.TournamentBuilderTest;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

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

import java.util.UUID;

public class TournamentCommandTest
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> buildTournamentDebugCommand = Commands.literal("tournament")
                .then(Commands.literal("testBuild")
                        .then(Commands.argument("maxPlayers", IntegerArgumentType.integer(0,64))
                                .then(Commands.literal("print")
                                        .then(Commands.argument("doPrint", BoolArgumentType.bool())
                                                .executes(c -> buildTournamentDebug(IntegerArgumentType.getInteger(c,"maxPlayers"),BoolArgumentType.getBool(c,"doPrint"))
                                        )
                                )
                        )
                )
        );

        dispatcher.register(buildTournamentDebugCommand);
    }

    public static int buildTournamentDebug(int players,boolean doPrint)
    {
        TournamentBuilderTest.buildTournamentDebug(players,doPrint);
        return Command.SINGLE_SUCCESS;
    }
}
