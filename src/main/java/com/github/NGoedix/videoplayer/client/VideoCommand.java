package com.github.NGoedix.videoplayer.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class VideoCommand {

    private VideoCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("video")
                        .then(ClientCommandManager.literal("on").executes(ctx -> set(ctx.getSource(), true)))
                        .then(ClientCommandManager.literal("off").executes(ctx -> set(ctx.getSource(), false)))
                        .executes(ctx -> {
                            feedback(ctx.getSource(), "Videos and images are currently "
                                    + (VideoToggle.isEnabled() ? "shown" : "hidden") + ". Use /video on|off to change it.");
                            return 1;
                        })));
    }

    private static int set(FabricClientCommandSource source, boolean value) {
        VideoToggle.setEnabled(value);
        feedback(source, value
                ? "Videos and images will now be shown."
                : "Videos and images will now be hidden.");
        return 1;
    }

    private static void feedback(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[Video] " + text));
    }
}
