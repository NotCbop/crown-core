package com.github.NGoedix.videoplayer.killfeed;

import com.github.NGoedix.videoplayer.Reference;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KillFeedChatListener {

    private static final Map<String, Integer> STREAKS = new HashMap<>();
    private static final Map<String, Long> STREAK_LAST = new HashMap<>();
    private static final long STREAK_TIMEOUT_MS = 15_000L;

    private static final long KILL_DEDUP_MS = 2500L;
    private static String lastKillVictim = null;
    private static long lastKillTime = 0L;

    private static final Pattern ASSIST = Pattern.compile("^(?:\\[.\\] )?You assisted in eliminating (.+)!$");
    private static final Pattern REVIVE = Pattern.compile("^(?:\\[.\\] )?(.+?) is being revived by the Hero!$");

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

            long now = System.currentTimeMillis();
            if (victimName.equalsIgnoreCase(lastKillVictim) && now - lastKillTime < KILL_DEDUP_MS) {
                return;
            }
            lastKillVictim = victimName;
            lastKillTime = now;

            if (attackerName != null && attackerName.equalsIgnoreCase(victimName)) {
                attackerName = null;
            }

            if (attackerName != null && !attackerName.isEmpty()) {
                Long lastKill = STREAK_LAST.get(attackerName);
                int streak;
                if (lastKill != null && now - lastKill <= STREAK_TIMEOUT_MS) {
                    streak = STREAKS.merge(attackerName, 1, Integer::sum);
                } else {
                    streak = 1;
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
