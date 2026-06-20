package com.github.NGoedix.videoplayer.commands;

import com.github.NGoedix.videoplayer.commands.arguments.SymbolStringArgumentType;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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

public class PlayCustomVideoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("playgovideo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.argument("volume", IntegerArgumentType.integer(0, 100))
                                .then(Commands.argument("url", SymbolStringArgumentType.symbolString())
                                        .then(Commands.argument("mode", IntegerArgumentType.integer(0, 1))
                                                .then(Commands.argument("control_blocked", BoolArgumentType.bool())
                                                        .then(Commands.argument("can_skip", BoolArgumentType.bool())
                                                                .executes(e -> PlayCustomVideoCommand.execute(e, true, true, false, false))
                                                                .then(Commands.argument("option_mode_in", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("option_secs_in", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("option_mode_out", IntegerArgumentType.integer())
                                                                                        .then(Commands.argument("option_secs_out", IntegerArgumentType.integer())
                                                                                                .executes(e -> PlayCustomVideoCommand.execute(e, true, true, false, true))))))))
                                                                .then(Commands.argument("position", IntegerArgumentType.integer())
                                                                        .executes(e -> PlayCustomVideoCommand.execute(e, false, false, true, false))
                                                                        .then(Commands.argument("option_mode_in", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("option_secs_in", IntegerArgumentType.integer())
                                                                                        .then(Commands.argument("option_mode_out", IntegerArgumentType.integer())
                                                                                                .then(Commands.argument("option_secs_out", IntegerArgumentType.integer())
                                                                                                        .executes(e -> PlayCustomVideoCommand.execute(e, false, false, true, true))))))))))));
    }

    public static int execute(CommandContext<CommandSourceStack> context, boolean hasControlBlocked, boolean hasCanSkip, boolean hasPartialMode, boolean hasOptions) {
        Collection<ServerPlayer> target;
        try {
            target = EntityArgument.getPlayers(context, "target");
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Invalid target"));
            return 0;
        }

        int volume = IntegerArgumentType.getInteger(context, "volume");
        String url = StringArgumentType.getString(context, "url");
        int mode = IntegerArgumentType.getInteger(context, "mode");

        boolean controlBlocked = hasControlBlocked && BoolArgumentType.getBool(context, "control_blocked");
        boolean canSkip = !hasCanSkip || BoolArgumentType.getBool(context, "can_skip");

        int position = hasPartialMode ? IntegerArgumentType.getInteger(context, "position") : 0;

        int optionModeIn = hasOptions ? IntegerArgumentType.getInteger(context, "option_mode_in") : -1;
        int optionSecsIn = hasOptions ? IntegerArgumentType.getInteger(context, "option_secs_in") : -1;
        int optionModeOut = hasOptions ? IntegerArgumentType.getInteger(context, "option_mode_out") : -1;
        int optionSecsOut = hasOptions ? IntegerArgumentType.getInteger(context, "option_secs_out") : -1;

        for (ServerPlayer player : target) {
            PacketHandler.sendS2CSendVideoStart(player,
                    url,
                    volume,
                    controlBlocked,
                    canSkip,
                    mode,
                    position,
                    optionModeIn,
                    optionSecsIn,
                    optionModeOut,
                    optionSecsOut
            );
        }

        return 1;
    }
}
