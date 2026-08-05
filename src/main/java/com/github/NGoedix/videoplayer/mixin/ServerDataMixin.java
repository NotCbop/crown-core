package com.github.NGoedix.videoplayer.mixin;

import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces "Server Resource Packs: Enabled" for every server, so server-sent packs are always
 * accepted and downloaded automatically — the yes/no prompt never appears and a required pack can
 * never be declined (declining one disconnects the player). The Crown server depends on its pack
 * for the kill feed fonts/icons, so an accidental "No" would silently break the HUD.
 *
 * <p>The vanilla prompt/decline flow reads this getter in
 * {@code ClientCommonPacketListenerImpl#handleResourcePackPush}; returning {@code ENABLED}
 * short-circuits straight to the download path. Pack failures are still caught and reported by
 * {@link com.github.NGoedix.videoplayer.packlog.PackFailureReporter}.</p>
 */
@Mixin(ServerData.class)
public class ServerDataMixin {

    @Inject(method = "getResourcePackStatus", at = @At("HEAD"), cancellable = true)
    private void crown$alwaysAcceptServerPacks(CallbackInfoReturnable<ServerData.ServerPackStatus> cir) {
        cir.setReturnValue(ServerData.ServerPackStatus.ENABLED);
    }
}
