package com.github.NGoedix.videoplayer.killfeed;

import org.jetbrains.annotations.Nullable;

public class KillEntry {

    public final PlayerRef victim;
    @Nullable
    public final PlayerRef attacker;
    public final KillMethod method;
    public final int streak;
    public final boolean hasAssist;
    public final long timestamp;
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
