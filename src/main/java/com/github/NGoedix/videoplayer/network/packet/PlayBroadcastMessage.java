package com.github.NGoedix.videoplayer.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

public record PlayBroadcastMessage(String body) implements CustomPacketPayload {

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
