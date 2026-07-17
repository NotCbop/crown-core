package com.github.NGoedix.videoplayer.mixin;

import com.github.NGoedix.videoplayer.packlog.PackFailureReporter;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Hooks the packet the client sends to report a resource-pack status. The canonical
 * {@code (UUID, Action)} constructor is built only when the client reports a status to the server,
 * so it is a clean place to catch download / load failures without touching the async download
 * internals. Decoding incoming packets uses the other constructor, so this never fires on receive.
 */
@Mixin(ServerboundResourcePackPacket.class)
public class ServerboundResourcePackPacketMixin {

    @Inject(
            method = "<init>(Ljava/util/UUID;Lnet/minecraft/network/protocol/common/ServerboundResourcePackPacket$Action;)V",
            at = @At("TAIL")
    )
    private void crown$onResourcePackStatus(UUID id, ServerboundResourcePackPacket.Action action, CallbackInfo ci) {
        PackFailureReporter.onStatus(id, action);
    }
}
