package com.github.NGoedix.videoplayer.commands;

import com.github.NGoedix.videoplayer.commands.arguments.SymbolStringArgumentType;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class PlayMusicCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("playmusic")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.argument("volume", IntegerArgumentType.integer(0, 100))
                                .then(Commands.argument("url", SymbolStringArgumentType.symbolString())
                                        .executes(PlayMusicCommand::execute)))));
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
            PacketHandler.sendS2CSendMusicStart(player,
                            StringArgumentType.getString(command, "url"),
                            IntegerArgumentType.getInteger(command, "volume")
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}
