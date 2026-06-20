package com.github.NGoedix.videoplayer.killfeed;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum KillMethod {
    GENERIC,
    MELEE,
    RANGE,
    ORB,
    POTION,
    MAGIC,
    VOID,
    DISCONNECT,
    EXPLOSION,
    LAVA,
    FIRE,
    REVIVE;

    public MutableComponent icon() {
        return orEmpty(glyph());
    }

    private MutableComponent glyph() {
        switch (this) {
            case RANGE:
            case ORB:
            case POTION:
            case MAGIC:
            case DISCONNECT:
            case EXPLOSION:
            case LAVA:
            case FIRE:
            case MELEE:
                return KillFeedAssets.mccIconByTexture("_fonts/kills.png");
            case GENERIC:
            case VOID:
            case REVIVE:
            default:
                return KillFeedAssets.mccIconByTexture("_fonts/skull.png");
        }
    }

    private static MutableComponent orEmpty(MutableComponent component) {
        return component != null ? component : Component.empty();
    }
}
