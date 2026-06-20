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

public class PlayerRef {

    public static final int DEFAULT_COLOR = 0xAAAAAA;

    public final String name;
    public final int color;

    public PlayerRef(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public PlayerRef(String name) {
        this(name, lookupColor(name));
    }

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
