package com.github.NGoedix.videoplayer.network.packet;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SendCustomVideoMessage(VideoMessageType action, String url, int volume, boolean controlBlocked, boolean canSkip,
                                     int mode, int position, int optionInMode, int optionInSecs, int optionOutMode, int optionOutSecs) implements CustomPacketPayload {

    public static final Type<SendCustomVideoMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "send_custom_video"));

    public static final StreamCodec<FriendlyByteBuf, SendCustomVideoMessage> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeEnum(msg.action);
                buf.writeUtf(msg.url == null ? "" : msg.url);
                buf.writeInt(msg.volume);
                buf.writeBoolean(msg.controlBlocked);
                buf.writeBoolean(msg.canSkip);
                buf.writeInt(msg.mode);
                buf.writeInt(msg.position);
                buf.writeInt(msg.optionInMode);
                buf.writeInt(msg.optionInSecs);
                buf.writeInt(msg.optionOutMode);
                buf.writeInt(msg.optionOutSecs);
            },
            buf -> new SendCustomVideoMessage(buf.readEnum(VideoMessageType.class), buf.readUtf(), buf.readInt(), buf.readBoolean(), buf.readBoolean(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum VideoMessageType { START, STOP }
}
