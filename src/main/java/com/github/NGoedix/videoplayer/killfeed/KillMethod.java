package com.github.NGoedix.videoplayer.killfeed;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * How a kill happened. The icon for each method is the exact same resource-pack glyph Trident uses:
 * the {@code trident:icon} font (private-use codepoints 0xE00E..0xE015) for most methods and the
 * {@code mcc:icon} skull / kills glyphs for melee / generic / void deaths.
 */
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

    /** Icon component, matching Trident's {@code KillMethod.icon}. Never {@code null}. */
    public MutableComponent icon() {
        return orEmpty(glyph());
    }

    /**
     * Raw glyph for this method, or {@code null} if the server resource pack does not provide it
     * (e.g. in a vanilla single-player test). Callers must go through {@link #icon()}.
     */
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
