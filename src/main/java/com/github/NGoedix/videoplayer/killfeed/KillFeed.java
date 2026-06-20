package com.github.NGoedix.videoplayer.killfeed;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.ArrayList;
import java.util.List;

public final class KillFeed {

    public static boolean enabled = false;
    public static int maxKills = 5;
    public static int removeSeconds = 10;
    public static boolean rightSide = true;
    public static int positionY = 20;
    public static int marginX = 6;
    public static boolean reverseOrder = false;
    public static boolean showYouInKill = true;

    private static final List<KillEntry> ENTRIES = new ArrayList<>();

    private KillFeed() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> KillFeedRenderer.render(guiGraphics));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> setEnabled(false));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> setEnabled(false));
    }

    public static List<KillEntry> entries() {
        return ENTRIES;
    }

    public static void add(KillEntry entry) {
        ENTRIES.add(entry);
        while (ENTRIES.size() > maxKills) {
            ENTRIES.remove(0);
        }
    }

    public static void applyAssist() {
        if (ENTRIES.isEmpty()) return;
        int last = ENTRIES.size() - 1;
        ENTRIES.set(last, ENTRIES.get(last).withAssist());
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            clear();
            KillFeedChatListener.resetStreaks();
        }
    }

    private static void tick() {
        if (!enabled || ENTRIES.isEmpty()) return;
        long now = System.currentTimeMillis();
        java.util.Iterator<KillEntry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            KillEntry entry = it.next();
            if (entry.removingAt == 0L) {
                if (removeSeconds > 0 && now - entry.timestamp >= removeSeconds * 1000L) {
                    entry.removingAt = now;
                }
            } else if (now - entry.removingAt >= KillFeedRenderer.SLIDE_MS) {
                it.remove();
            }
        }
    }
}
