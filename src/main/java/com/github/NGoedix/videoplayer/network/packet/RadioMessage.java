package com.github.NGoedix.videoplayer.network.packet;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RadioMessage(String url, BlockPos pos, boolean playing) implements CustomPacketPayload {

    public static final Type<RadioMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "radio"));

    public static final StreamCodec<FriendlyByteBuf, RadioMessage> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeUtf(msg.url);
                buf.writeBlockPos(msg.pos);
                buf.writeBoolean(msg.playing);
            },
            buf -> new RadioMessage(buf.readUtf(), buf.readBlockPos(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
