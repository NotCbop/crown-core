package com.github.NGoedix.videoplayer.killfeed;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * Client-side {@code /killfeed} command: {@code on|off} is the player's own show/hide preference
 * (persisted via {@link KillFeedToggle}), {@code side} flips which screen edge the feed sits on.
 * The server can still hide the feed for everyone via the {@code crown:play} channel — see
 * {@code PacketHandler} and the CrownChampionshipUtils plugin.
 */
public final class KillFeedCommand {

    private KillFeedCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("killfeed")
                        .then(ClientCommandManager.literal("on").executes(ctx -> {
                            KillFeed.setUserEnabled(true);
                            feedback(ctx.getSource(), KillFeed.enabled
                                    ? "Kill feed shown."
                                    : "Kill feed shown (the server has it hidden right now, so it will appear when a game turns it on).");
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("off").executes(ctx -> {
                            KillFeed.setUserEnabled(false);
                            feedback(ctx.getSource(), "Kill feed hidden. Use /killfeed on to show it again.");
                            return 1;
                        }))
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
