package com.github.NGoedix.videoplayer.network.packet;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FrameVideoMessage(String url, BlockPos pos, boolean playing, int tick) implements CustomPacketPayload {

    public static final Type<FrameVideoMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "frame_video"));

    public static final StreamCodec<FriendlyByteBuf, FrameVideoMessage> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeUtf(msg.url);
                buf.writeBlockPos(msg.pos);
                buf.writeBoolean(msg.playing);
                buf.writeInt(msg.tick);
            },
            buf -> new FrameVideoMessage(buf.readUtf(), buf.readBlockPos(), buf.readBoolean(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
