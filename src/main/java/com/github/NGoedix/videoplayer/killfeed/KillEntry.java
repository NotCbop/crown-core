package com.github.NGoedix.videoplayer.killfeed;

import org.jetbrains.annotations.Nullable;

/** A single line in the kill feed: attacker (optional) -> method -> victim. */
public class KillEntry {

    public final PlayerRef victim;
    @Nullable
    public final PlayerRef attacker;
    public final KillMethod method;
    public final int streak;
    public final boolean hasAssist;
    public final long timestamp;
    /** 0 while active; set to the start time when the entry begins sliding back out before removal. */
    public long removingAt = 0L;

    public KillEntry(PlayerRef victim, @Nullable PlayerRef attacker, KillMethod method, int streak, boolean hasAssist) {
        this.victim = victim;
        this.attacker = attacker;
        this.method = method;
        this.streak = streak;
        this.hasAssist = hasAssist;
        this.timestamp = System.currentTimeMillis();
    }

    public KillEntry withAssist() {
        return new KillEntry(victim, attacker, method, streak, true);
    }
}
