package com.github.NGoedix.videoplayer.client;

import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.killfeed.KillFeed;
import com.github.NGoedix.videoplayer.killfeed.KillFeedAssets;
import com.github.NGoedix.videoplayer.killfeed.KillFeedChatListener;
import com.github.NGoedix.videoplayer.killfeed.KillFeedCommand;
import com.github.NGoedix.videoplayer.killfeed.KillFeedToggle;
import com.github.NGoedix.videoplayer.loading.LoadingScreen;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import com.github.NGoedix.videoplayer.packlog.PackFailureReporter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class ClientHandler implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Reference.LOGGER.info("Initializing Client");

        PacketHandler.registerClientReceiver();

        // Detects whether the server runs the CrownChampionshipUtils plugin; Crown features
        // (kill feed etc.) stay dormant everywhere else.
        CrownServer.register();

        // Kill feed
        KillFeedAssets.register();
        KillFeed.register();
        KillFeedChatListener.register();
        KillFeedCommand.register();
        KillFeedToggle.load();

        // Server-controlled F1 hide-GUI block
        HideUiBlocker.register();

        // Animated game loading overlay (plugin-driven; registered AFTER the kill feed so it draws on top)
        LoadingScreen.register();

        // Resource-pack failure -> mclo.gs link in chat
        PackFailureReporter.register();

        // Version handshake: tell the server which mod build this is, so the CrownChampionshipUtils
        // plugin can warn/kick outdated clients (it treats modded players that never send one as
        // pre-handshake builds).
        String modVersion = FabricLoader.getInstance().getModContainer(Reference.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                PacketHandler.sendC2SPlayBody("version;" + modVersion));
    }
}
