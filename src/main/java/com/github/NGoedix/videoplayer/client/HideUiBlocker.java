package com.github.NGoedix.videoplayer.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Server-controlled block of the F1 hide-GUI toggle, so events can keep titles
 * (e.g. loading screens) visible. The CrownChampionshipUtils plugin flips this
 * via the {@code crown:play} channel (a {@code blockf1 on|off} body).
 *
 * Rather than intercepting the keypress (whose handling moves between
 * versions), the toggle is reverted at the end of the same client tick it was
 * pressed on — before anything renders without a UI — with a short action bar
 * notice. Defaults off, and resets on disconnect so other servers are
 * unaffected.
 */
public final class HideUiBlocker {

    private static boolean blocked = false;

    private HideUiBlocker() {}

    public static void register() {
        // Per-tick revert: an F1 press can hide the UI for up to one tick
        // (~50ms) before it snaps back. Acceptable; a per-frame hook could
        // remove even that if it ever matters.
        ClientTickEvents.END_CLIENT_TICK.register(HideUiBlocker::enforce);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> blocked = false);
    }

    private static void enforce(Minecraft client) {
        if (!blocked || !client.options.hideGui) return;
        client.options.hideGui = false;
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("Hiding the UI is disabled on this server.")
                            .withStyle(ChatFormatting.RED),
                    true);
        }
    }

    public static void setBlocked(boolean value) {
        blocked = value;
    }
}
