package com.github.NGoedix.videoplayer.network.packet;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RadioUpdateMessage(BlockPos pos, String url, int volume, int tick, boolean isPlaying, boolean exit) implements CustomPacketPayload {

    public static final Type<RadioUpdateMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "update_radio"));

    public static final StreamCodec<FriendlyByteBuf, RadioUpdateMessage> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.pos);
                buf.writeUtf(msg.url == null ? "" : msg.url);
                buf.writeInt(msg.volume);
                buf.writeInt(msg.tick);
                buf.writeBoolean(msg.isPlaying);
                buf.writeBoolean(msg.exit);
            },
            buf -> new RadioUpdateMessage(buf.readBlockPos(), buf.readUtf(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
