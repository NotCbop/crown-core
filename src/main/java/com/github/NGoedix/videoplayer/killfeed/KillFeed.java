package com.github.NGoedix.videoplayer.killfeed;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the current kill feed state and the (very small) set of user-tweakable options.
 * Entries are added by {@link KillFeedChatListener}, expire on a timer, and are drawn by
 * {@link KillFeedRenderer}.
 */
public final class KillFeed {

    // --- Options ---
    /**
     * Whether the feed is shown. Server-controlled: the CrownChampionshipUtils plugin flips this via
     * the {@code crown:play} channel (a {@code killfeed on|off} body) so admins can hide it for
     * non-PvP games. Players cannot toggle it. Defaults on.
     */
    public static boolean enabled = true;
    public static int maxKills = 5;
    /** Seconds before a kill fades out. 0 = keep until cleared. */
    public static int removeSeconds = 10;
    public static boolean rightSide = true;
    public static int positionY = 20;
    /** Horizontal gap kept between the feed and the screen edge, in pixels. */
    public static int marginX = 6;
    public static boolean reverseOrder = false;
    public static boolean showYouInKill = true;

    private static final List<KillEntry> ENTRIES = new ArrayList<>();

    private KillFeed() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> KillFeedRenderer.render(guiGraphics));
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

    /** Marks the most recent kill as having an assist. */
    public static void applyAssist() {
        if (ENTRIES.isEmpty()) return;
        int last = ENTRIES.size() - 1;
        ENTRIES.set(last, ENTRIES.get(last).withAssist());
    }

    public static void clear() {
        ENTRIES.clear();
    }

    /**
     * Server-driven enable/disable (from the CrownChampionshipUtils plugin). Disabling also drops any
     * current entries and streak counts so the feed vanishes immediately. Must be called on the client
     * thread.
     */
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
                // Not yet expiring: start the slide-out once it has lived past removeSeconds.
                if (removeSeconds > 0 && now - entry.timestamp >= removeSeconds * 1000L) {
                    entry.removingAt = now;
                }
            } else if (now - entry.removingAt >= KillFeedRenderer.SLIDE_MS) {
                // Slide-out finished: actually drop it.
                it.remove();
            }
        }
    }
}
