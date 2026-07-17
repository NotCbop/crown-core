package com.github.NGoedix.videoplayer.killfeed;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * Client-side {@code /killfeed} command. Players may only flip which side of the screen the feed
 * sits on; everything else (whether the feed shows at all) is controlled by the server via the
 * {@code crown:play} channel — see {@code PacketHandler} and the CrownChampionshipUtils plugin.
 */
public final class KillFeedCommand {

    private KillFeedCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("killfeed")
                        .then(ClientCommandManager.literal("side").executes(ctx -> {
                            KillFeed.rightSide = !KillFeed.rightSide;
                            feedback(ctx.getSource(), "Kill feed on the " + (KillFeed.rightSide ? "right" : "left"));
                            return 1;
                        }))));
    }

    private static void feedback(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[Kill Feed] " + text));
    }
}
