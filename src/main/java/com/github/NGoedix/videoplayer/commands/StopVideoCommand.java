package com.github.NGoedix.videoplayer.commands;

import com.github.NGoedix.videoplayer.network.PacketHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class StopVideoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("stopvideo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes(StopVideoCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> command) {
        Collection<ServerPlayer> players;

        try {
            players = EntityArgument.getPlayers(command, "target");
        } catch (CommandSyntaxException e) {
            command.getSource().sendFailure(Component.literal("Error with target parameter."));
            return Command.SINGLE_SUCCESS;
        }

        for (ServerPlayer player : players) {
            PacketHandler.sendS2CSendVideoStop(player);
        }

        return Command.SINGLE_SUCCESS;
    }
}
