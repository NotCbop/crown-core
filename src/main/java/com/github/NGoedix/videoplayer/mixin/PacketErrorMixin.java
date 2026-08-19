package com.github.NGoedix.videoplayer.mixin;

import com.github.NGoedix.videoplayer.Reference;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the "Network Protocol Error" disconnect. Since 1.20.5, any exception while handling a
 * server packet makes {@code ClientCommonPacketListenerImpl#onPacketError} write a disconnect
 * report and drop the connection. Cancelling it here logs the bad packet and keeps playing
 * instead — same behaviour as Mobilelize's Network-Protocol-Disconnect mod, reimplemented so it
 * ships inside Crown Core.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class PacketErrorMixin {

    @Inject(method = "onPacketError", at = @At("HEAD"), cancellable = true)
    private void crown$ignorePacketErrors(Packet<?> packet, Exception exception, CallbackInfo ci) {
        Reference.LOGGER.error("Failed to handle packet {} — disconnect prevented", packet, exception);
        ci.cancel();
    }
}
