package com.github.NGoedix.videoplayer.killfeed;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.scores.PlayerTeam;

/**
 * A player shown on one side of a kill entry. Holds the name and a colour; the head is an inline
 * player-sprite component (the same "nox head" look Trident uses via {@code SkullSprite}, but using
 * vanilla 1.21.11's native {@link PlayerSprite}).
 */
public class PlayerRef {

    /** Colour used when a player has no team / no name colour. */
    public static final int DEFAULT_COLOR = 0xAAAAAA;

    public final String name;
    public final int color; // 0xRRGGBB

    public PlayerRef(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public PlayerRef(String name) {
        this(name, lookupColor(name));
    }

    /** Inline player-head component (with hat), resolved from the tab list when possible. */
    public MutableComponent head() {
        ResolvableProfile profile;
        PlayerInfo info = playerInfo(name);
        if (info != null) {
            profile = ResolvableProfile.createResolved(info.getProfile());
        } else {
            profile = ResolvableProfile.createUnresolved(name);
        }
        return Component.object(new PlayerSprite(profile, true));
    }

    private static PlayerInfo playerInfo(String name) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null ? connection.getPlayerInfo(name) : null;
    }

    /** Tries to use the player's team colour, matching the coloured bars in the reference image. */
    private static int lookupColor(String name) {
        PlayerInfo info = playerInfo(name);
        if (info != null) {
            PlayerTeam team = info.getTeam();
            if (team != null) {
                ChatFormatting formatting = team.getColor();
                if (formatting != null && formatting.getColor() != null) {
                    return formatting.getColor();
                }
            }
        }
        return DEFAULT_COLOR;
    }
}
