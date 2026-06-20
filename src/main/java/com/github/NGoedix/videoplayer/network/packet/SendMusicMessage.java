package com.github.NGoedix.videoplayer.network.packet;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SendMusicMessage(MusicMessageType action, String url, int volume) implements CustomPacketPayload {

    public static final Type<SendMusicMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "send_music"));

    public static final StreamCodec<FriendlyByteBuf, SendMusicMessage> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeEnum(msg.action);
                buf.writeUtf(msg.url == null ? "" : msg.url);
                buf.writeInt(msg.volume);
            },
            buf -> new SendMusicMessage(buf.readEnum(MusicMessageType.class), buf.readUtf(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum MusicMessageType { START, STOP }
}
