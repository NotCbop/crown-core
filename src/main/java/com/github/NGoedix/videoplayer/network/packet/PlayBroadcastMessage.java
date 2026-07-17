package com.github.NGoedix.videoplayer.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

/**
 * A deliberately simple S2C channel meant to be driven by servers that do NOT have this mod
 * installed (e.g. a Paper/Spigot server using a plugin or Skript).
 * <p>
 * The whole packet body is just a UTF-8 string with no length prefix, so an external sender only
 * needs to write the raw bytes of a string onto the {@code crown:play} plugin channel.
 * <p>
 * Body grammar (fields separated by {@code ;}):
 * <pre>
 *   &lt;url&gt;
 *   &lt;url&gt;;&lt;volume 0-100&gt;
 *   &lt;url&gt;;&lt;volume&gt;;&lt;controlBlocked true|false&gt;
 *   &lt;url&gt;;&lt;volume&gt;;&lt;controlBlocked&gt;;&lt;canSkip true|false&gt;
 *   stop
 * </pre>
 */
public record PlayBroadcastMessage(String body) implements CustomPacketPayload {

    // Channel id is the player-facing "crown" namespace (not the internal videoplayer mod id), so the
    // CrownChampionshipUtils plugin and the mod share a brand-consistent control channel: crown:play.
    public static final Type<PlayBroadcastMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath("crown", "play"));

    public static final StreamCodec<FriendlyByteBuf, PlayBroadcastMessage> CODEC = StreamCodec.of(
            (buf, msg) -> buf.writeBytes(msg.body.getBytes(StandardCharsets.UTF_8)),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new PlayBroadcastMessage(new String(bytes, StandardCharsets.UTF_8));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
