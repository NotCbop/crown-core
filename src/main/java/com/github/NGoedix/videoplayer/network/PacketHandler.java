package com.github.NGoedix.videoplayer.network;

import com.github.NGoedix.videoplayer.client.CrownServer;
import com.github.NGoedix.videoplayer.client.HideUiBlocker;
import com.github.NGoedix.videoplayer.killfeed.KillFeed;
import com.github.NGoedix.videoplayer.killfeed.KillFeedChatListener;
import com.github.NGoedix.videoplayer.loading.LoadingScreen;
import com.github.NGoedix.videoplayer.network.packet.PlayBroadcastMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * The {@code crown:play} plugin channel shared with the CrownChampionshipUtils Paper plugin: plain
 * UTF-8 bodies (see {@link PlayBroadcastMessage}). S2C: {@code killfeed on|off}, {@code killmsg <text>},
 * {@code blockf1 on|off}, {@code loadscreen;...}. C2S: {@code version;<mod version>}.
 */
public class PacketHandler {

    /** Registers the crown:play payload type in both directions. Must run on both sides. */
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(PlayBroadcastMessage.TYPE, PlayBroadcastMessage.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayBroadcastMessage.TYPE, PlayBroadcastMessage.CODEC);

        // No-op on a Fabric server: the client's bodies are meant for the CrownChampionshipUtils Paper plugin.
        ServerPlayNetworking.registerGlobalReceiver(PlayBroadcastMessage.TYPE, (payload, context) -> {});
    }

    /** S2C receiver (client only). */
    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(PlayBroadcastMessage.TYPE, (payload, context) -> {
            // Anything arriving on crown:play proves the Crown plugin is on this server.
            CrownServer.markPresent();

            String body = payload.body().trim();
            if (body.isEmpty()) return;

            // "killfeed on|off" — server-side control of the kill feed (e.g. hide it for non-PvP games).
            if (body.regionMatches(true, 0, "killfeed", 0, "killfeed".length())) {
                String[] kf = body.split("\\s+");
                boolean on = kf.length < 2 || parseOnOff(kf[1]);
                context.client().execute(() -> KillFeed.setEnabled(on));
                return;
            }

            // "blockf1 on|off" — server-side control of the F1 hide-GUI key, so events can keep
            // titles (loading screens etc.) visible.
            if (body.regionMatches(true, 0, "blockf1", 0, "blockf1".length())) {
                String[] bf = body.split("\\s+");
                boolean on = bf.length < 2 || parseOnOff(bf[1]);
                context.client().execute(() -> HideUiBlocker.setBlocked(on));
                return;
            }

            // "killmsg <death message>" — the plugin relays each kill to every modded player so the feed
            // shows for everyone, not just the victim whose chat the server messaged.
            if (body.regionMatches(true, 0, "killmsg ", 0, "killmsg ".length())) {
                if (!KillFeed.enabled) return;
                String deathText = body.substring("killmsg ".length());
                context.client().execute(() -> KillFeedChatListener.handleText(deathText));
                return;
            }

            // "loadscreen;<seconds>;<title>;<subtitle>;<game>" — show the animated game loading overlay
            // for that long (the bar fills across it); <game> picks the icon (skb, sg, sky_battle...)
            // and a blank title falls back to the game's name. "loadscreen off" (or just
            // "loadscreen") fades it out early. Fields must not contain ';' (the plugin strips them).
            if (body.regionMatches(true, 0, "loadscreen", 0, "loadscreen".length())) {
                String[] p = body.split(";", 6);
                if (p.length < 2 || p[1].trim().isEmpty() || p[1].trim().equalsIgnoreCase("off")) {
                    context.client().execute(LoadingScreen::hide);
                } else {
                    int seconds = Math.max(1, Math.min(120, parseIntPlain(p[1].trim(), 10)));
                    String title = p.length > 2 ? p[2].trim() : "";
                    String subtitle = p.length > 3 ? p[3].trim() : "";
                    String game = p.length > 4 ? p[4].trim() : "";
                    context.client().execute(() -> LoadingScreen.show(title, subtitle, game, seconds * 1000));
                }
            }
        });
    }

    /** Reads an on/off-style token; anything other than an explicit "off"/"false"/"disable"/"0" is on. */
    private static boolean parseOnOff(String value) {
        switch (value.toLowerCase()) {
            case "off":
            case "false":
            case "disable":
            case "disabled":
            case "0":
            case "no":
                return false;
            default:
                return true;
        }
    }

    private static int parseIntPlain(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Sends a plain-text body to the server on crown:play (silently dropped when not in a world). */
    public static void sendC2SPlayBody(String body) {
        try {
            ClientPlayNetworking.send(new PlayBroadcastMessage(body));
        } catch (Exception ignored) {
            // Not connected / play phase over — the body just doesn't get delivered.
        }
    }
}
