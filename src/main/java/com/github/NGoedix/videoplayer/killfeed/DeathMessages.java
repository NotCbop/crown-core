package com.github.NGoedix.videoplayer.killfeed;

import java.util.regex.Pattern;

/**
 * Death-message patterns for the server's killfeed skript. Every message is prefixed with
 * {@code [💀] } (a skull in brackets) followed by {@code <victim> <verb> [by <attacker>].}.
 * Group 1 is the victim, group 2 (when present) is the attacker. Attacker patterns are listed first
 * so the more specific phrasing wins.
 */
public enum DeathMessages {
    // --- With an attacker ---
    ATTACK("(.+?) was slain by (.+?)\\.$", KillMethod.MELEE),
    SHOT_BY("(.+?) was shot by (.+?)\\.$", KillMethod.RANGE),
    IMPALED("(.+?) was impaled by (.+?)\\.$", KillMethod.RANGE),
    BLOWN_AWAY("(.+?) was blown away by (.+?)\\.$", KillMethod.RANGE),
    BLOWN_UP("(.+?) was blown up by (.+?)\\.$", KillMethod.EXPLOSION),
    FALL_ESCAPE("(.+?) fell from a high place while trying to escape (.+?)\\.$", KillMethod.GENERIC),
    DROWN_ESCAPE("(.+?) drowned while trying to escape (.+?)\\.$", KillMethod.GENERIC),
    VOID_RIVAL("(.+?) didn't want to live in the same world as (.+?)\\.$", KillMethod.MELEE),
    SUFFOCATE_ESCAPE("(.+?) suffocated in a wall while trying to escape (.+?)\\.$", KillMethod.MELEE),
    BURNT_FIGHT("(.+?) was burnt to a crisp while fighting (.+?)\\.$", KillMethod.FIRE),
    LAVA_ESCAPE("(.+?) tried to swim in lava while trying to escape (.+?)\\.$", KillMethod.LAVA),
    DISCONNECT_ESCAPE("(.+?) disconnected while trying to escape (.+?)\\.$", KillMethod.DISCONNECT),
    MAGIC_BY("(.+?) was eliminated using magic by (.+?)\\.$", KillMethod.MAGIC),
    PRICKED_ESCAPE("(.+?) was pricked to death while trying to escape (.+?)\\.$", KillMethod.MELEE),
    SPLEEFED_BY("(.+?) was spleefed by (.+?)\\.$", KillMethod.MELEE),
    DIED_ESCAPE("(.+?) died while trying to escape (.+?)\\.$", KillMethod.GENERIC),

    // --- Self / environmental ---
    FALL("(.+?) fell from a high place\\.$", KillMethod.GENERIC),
    DROWN("(.+?) drowned\\.$", KillMethod.GENERIC),
    VOID("(.+?) fell out of the world\\.$", KillMethod.VOID),
    SUFFOCATE("(.+?) suffocated in a wall\\.$", KillMethod.GENERIC),
    BURNT("(.+?) was burnt to a crisp\\.$", KillMethod.FIRE),
    LAVA("(.+?) tried to swim in lava\\.$", KillMethod.LAVA),
    DISCONNECT("(.+?) disconnected\\.$", KillMethod.DISCONNECT),
    MAGIC("(.+?) was eliminated using magic\\.$", KillMethod.MAGIC),
    OFFLINE("(.+?) did not rejoin in time\\.$", KillMethod.DISCONNECT),
    PRICKED("(.+?) was pricked to death\\.$", KillMethod.GENERIC),
    SPLEEFED("(.+?) was spleefed\\.$", KillMethod.MELEE),
    SHOT("(.+?) was shot\\.$", KillMethod.RANGE),
    DIED("(.+?) died\\.$", KillMethod.GENERIC);

    /**
     * Shared chat prefix: {@code [skull] }. Optional, because the same patterns also match the plain
     * vanilla death text the CrownChampionshipUtils plugin broadcasts to other players (which carries
     * neither the skript's skull prefix nor its trailing period). Compile-time constant.
     */
    private static final String PREFIX = "^(?:\\[\\x{1F480}\\] )?";

    public final Pattern pattern;
    public final KillMethod method;

    DeathMessages(String body, KillMethod method) {
        // Relax the trailing period to optional (vanilla messages have none, the skript adds one) so a
        // single pattern matches both the victim's chat line and the plugin's broadcast of the same kill.
        String relaxed = body.endsWith("\\.$") ? body.substring(0, body.length() - 3) + "\\.?$" : body;
        this.pattern = Pattern.compile(PREFIX + relaxed);
        this.method = method;
    }
}
