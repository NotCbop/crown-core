package com.github.NGoedix.videoplayer;

import com.github.NGoedix.videoplayer.network.PacketHandler;
import net.fabricmc.api.ModInitializer;

public class VideoPlayer implements ModInitializer {

    @Override
    public void onInitialize() {
        Reference.LOGGER.info("Initializing CrownCore");

        // crown:play payload types must be registered on both sides.
        PacketHandler.registerPayloads();
    }
}
