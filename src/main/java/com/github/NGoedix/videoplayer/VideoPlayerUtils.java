
package com.github.NGoedix.videoplayer;

import net.fabricmc.loader.api.FabricLoader;

public class VideoPlayerUtils {

    private static Boolean waterMedia;

    /**
     * Whether WaterMedia is installed. It's an optional dependency: without it the video/radio
     * features are disabled, but the kill feed and pack reporter still work. Cached after first call.
     */
    public static boolean hasWaterMedia() {
        if (waterMedia == null) {
            waterMedia = FabricLoader.getInstance().isModLoaded("watermedia");
        }
        return waterMedia;
    }

    public static boolean isInstalled(String... mods) {
        for (String id: mods) {
            if (!FabricLoader.getInstance().isModLoaded(id)) {
                return false;
            }
        }
        return true;
    }

    public static class UnsupportedModException extends UnsupportedOperationException {
        private static final String MSG = "§fMod §6'%s' §fis not compatible with §e'%s'§f. please remove it";
        private static final String MSG_REASON = "§fMod §6'%s' §fis not compatible with §e'%s' §fbecause §c%s §fplease remove it";
        private static final String MSG_REASON_ALT = "§fMod §6'%s' §fis not compatible with §e'%s' §fbecause §c%s §fuse §a'%s' §finstead";

        public UnsupportedModException(String modid) {
            super(String.format(MSG, modid, Reference.MOD_ID));
        }

        public UnsupportedModException(String modid, String reason) {
            super(String.format(MSG_REASON, modid, Reference.MOD_ID, reason));
        }

        public UnsupportedModException(String modid, String reason, String alternatives) {
            super(String.format(MSG_REASON_ALT, modid, Reference.MOD_ID, reason, alternatives));
        }
    }
}
