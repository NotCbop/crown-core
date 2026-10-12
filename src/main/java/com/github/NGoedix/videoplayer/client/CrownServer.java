package com.github.NGoedix.videoplayer.client;

import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.network.packet.PlayBroadcastMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

/**
 * Tracks whether the current server runs the CrownChampionshipUtils plugin, so Crown-specific
 * features (the kill feed, most importantly its vanilla-chat death parsing) stay off on every
 * other server.
 *
 * <p>Detection: Paper announces each plugin's registered channels to the client in a
 * {@code minecraft:register} payload right after join, which Fabric surfaces as
 * {@link C2SPlayChannelEvents#REGISTER} — the plugin registers {@code crown:play} as an incoming
 * channel, so its presence there means the plugin is installed. The integrated (singleplayer)
 * server is excluded: this mod's own server side also announces {@code crown:play}, but there is
 * no plugin there. As a belt-and-braces fallback, actually receiving any {@code crown:play}
 * payload ({@link #markPresent()}) counts too.</p>
 */
@Environment(EnvType.CLIENT)
public final class CrownServer {

    private static volatile boolean present = false;

    private CrownServer() {}

    public static boolean isPresent() {
        return present;
    }

    /** Called from the {@code crown:play} S2C receiver — a payload arriving proves the plugin is there. */
    public static void markPresent() {
        present = true;
    }

    public static void register() {
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (!present && channels.contains(PlayBroadcastMessage.TYPE.id()) && !client.isLocalServer()) {
                present = true;
                Reference.LOGGER.info("Crown plugin detected on this server (crown:play channel registered)");
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> present = false);
    }
}
