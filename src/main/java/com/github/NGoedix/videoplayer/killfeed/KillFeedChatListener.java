package com.github.NGoedix.videoplayer.killfeed;

import com.github.NGoedix.videoplayer.Reference;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to incoming game messages, recognises the server's death messages and turns them into
 * kill feed entries. The server's formatted names carry rank/status glyphs around the username, so
 * {@link #resolveName} pulls the bare username back out for the head / team-colour lookup.
 */
public final class KillFeedChatListener {

    private static final Map<String, Integer> STREAKS = new HashMap<>();
    /** When each attacker last got a kill; their streak resets after a gap of {@link #STREAK_TIMEOUT_MS}. */
    private static final Map<String, Long> STREAK_LAST = new HashMap<>();
    /** A player's kill streak resets after this long without a kill. */
    private static final long STREAK_TIMEOUT_MS = 15_000L;

    /** De-dup window: the same kill arrives via chat and the plugin broadcast within a tick or two. */
    private static final long KILL_DEDUP_MS = 2500L;
    private static String lastKillVictim = null;
    private static long lastKillTime = 0L;

    private static final Pattern ASSIST = Pattern.compile("^(?:\\[.\\] )?You assisted in eliminating (.+)!$");
    private static final Pattern REVIVE = Pattern.compile("^(?:\\[.\\] )?(.+?) is being revived by the Hero!$");

    /** Minecraft usernames: 1-16 of [A-Za-z0-9_]. Used to strip rank/status glyphs. */
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private KillFeedChatListener() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!KillFeed.enabled || overlay) return;
            handleText(message.getString());
        });
    }

    public static void resetStreaks() {
        STREAKS.clear();
        STREAK_LAST.clear();
        lastKillVictim = null;
        lastKillTime = 0L;
    }

    /**
     * Parses one death/assist/revive line and updates the feed. Shared by the local chat listener
     * (the victim's own chat) and the {@code killmsg} body the CrownChampionshipUtils plugin broadcasts
     * to everyone else (see {@code PacketHandler}). Must run on the client thread.
     */
    public static void handleText(String raw) {
        try {
            handle(raw == null ? "" : raw.trim());
        } catch (Exception e) {
            Reference.LOGGER.error("Kill feed failed to parse message '{}': {}", raw, e.getMessage());
        }
    }

    private static void handle(String text) {
        if (text.isEmpty()) return;

        if (ASSIST.matcher(text).matches()) {
            KillFeed.applyAssist();
            return;
        }

        Matcher revive = REVIVE.matcher(text);
        if (revive.matches()) {
            String revived = resolveName(revive.group(1));
            if (!revived.isEmpty()) {
                KillFeed.add(new KillEntry(new PlayerRef(revived), null, KillMethod.REVIVE, 0, false));
            }
            return;
        }

        for (DeathMessages death : DeathMessages.values()) {
            Matcher matcher = death.pattern.matcher(text);
            if (!matcher.matches()) continue;

            String victimName = resolveName(matcher.group(1));
            String attackerName = matcher.groupCount() >= 2 && matcher.group(2) != null ? resolveName(matcher.group(2)) : null;
            if (victimName.isEmpty()) return;

            // The same kill reaches us twice: once from chat and once from the plugin's broadcast to all
            // players. A player can't die twice within a couple of seconds, so drop a repeat of the same
            // victim inside that window. Done before the streak bump below so streaks don't double-count.
            long now = System.currentTimeMillis();
            if (victimName.equalsIgnoreCase(lastKillVictim) && now - lastKillTime < KILL_DEDUP_MS) {
                return;
            }
            lastKillVictim = victimName;
            lastKillTime = now;

            // Some self-deaths (e.g. self-explosion) repeat the victim where an attacker would go.
            if (attackerName != null && attackerName.equalsIgnoreCase(victimName)) {
                attackerName = null;
            }

            if (attackerName != null && !attackerName.isEmpty()) {
                Long lastKill = STREAK_LAST.get(attackerName);
                int streak;
                if (lastKill != null && now - lastKill <= STREAK_TIMEOUT_MS) {
                    streak = STREAKS.merge(attackerName, 1, Integer::sum);
                } else {
                    streak = 1; // first kill, or the streak timed out
                    STREAKS.put(attackerName, 1);
                }
                STREAK_LAST.put(attackerName, now);
                KillFeed.add(new KillEntry(
                        new PlayerRef(victimName),
                        new PlayerRef(attackerName),
                        death.method,
                        streak,
                        false
                ));
            } else {
                KillFeed.add(new KillEntry(new PlayerRef(victimName), null, death.method, 0, false));
            }
            return;
        }
    }

    /**
     * Extracts the bare username from a server-formatted name (which may include rank / status
     * glyphs). Prefers a token that matches a player currently in the tab list, otherwise falls back
     * to the longest word-like token.
     */
    static String resolveName(String raw) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        Matcher matcher = USERNAME.matcher(raw);
        String longest = null;
        while (matcher.find()) {
            String token = matcher.group();
            if (connection != null && connection.getPlayerInfo(token) != null) {
                return token;
            }
            if (longest == null || token.length() > longest.length()) {
                longest = token;
            }
        }
        return longest != null ? longest : raw.trim();
    }
}
