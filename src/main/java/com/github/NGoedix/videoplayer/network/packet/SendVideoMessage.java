package com.github.NGoedix.videoplayer.network.packet;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SendVideoMessage(VideoMessageType action, String url, int volume, boolean controlBlocked, boolean canSkip) implements CustomPacketPayload {

    public static final Type<SendVideoMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "send_video"));

    public static final StreamCodec<FriendlyByteBuf, SendVideoMessage> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeEnum(msg.action);
                buf.writeUtf(msg.url == null ? "" : msg.url);
                buf.writeInt(msg.volume);
                buf.writeBoolean(msg.controlBlocked);
                buf.writeBoolean(msg.canSkip);
            },
            buf -> new SendVideoMessage(buf.readEnum(VideoMessageType.class), buf.readUtf(), buf.readInt(), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum VideoMessageType { START, STOP }
}
