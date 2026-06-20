package com.github.NGoedix.videoplayer.network.packet;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenRadioManagerScreenMessage(BlockPos pos, String url, int volume, boolean isPlaying) implements CustomPacketPayload {

    public static final Type<OpenRadioManagerScreenMessage> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "open_radio_manager"));

    public static final StreamCodec<FriendlyByteBuf, OpenRadioManagerScreenMessage> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeBlockPos(msg.pos);
                buf.writeUtf(msg.url);
                buf.writeInt(msg.volume);
                buf.writeBoolean(msg.isPlaying);
            },
            buf -> new OpenRadioManagerScreenMessage(buf.readBlockPos(), buf.readUtf(), buf.readInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
