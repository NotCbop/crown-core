package com.github.NGoedix.videoplayer.mixin;

import com.github.NGoedix.videoplayer.packlog.PackFailureReporter;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

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
