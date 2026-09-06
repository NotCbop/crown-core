package com.github.NGoedix.videoplayer.killfeed;

import com.github.NGoedix.videoplayer.Reference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persists the player's own kill feed preference ({@code /killfeed on|off}) across restarts,
 * same pattern as {@link com.github.NGoedix.videoplayer.client.VideoToggle}.
 */
@Environment(EnvType.CLIENT)
public final class KillFeedToggle {

    private static final String KEY = "showKillFeed";

    private static Path file;

    private KillFeedToggle() {}

    public static void load() {
        file = FabricLoader.getInstance().getConfigDir().resolve("crown-killfeed.properties");
        if (!Files.exists(file)) return;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            KillFeed.userEnabled = !"false".equalsIgnoreCase(props.getProperty(KEY, "true"));
        } catch (IOException e) {
            Reference.LOGGER.warn("Could not read " + file + ": " + e);
        }
    }

    static void save() {
        if (file == null) return;

        Properties props = new Properties();
        props.setProperty(KEY, Boolean.toString(KillFeed.userEnabled));
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "CrownCore");
        } catch (IOException e) {
            Reference.LOGGER.warn("Could not write " + file + ": " + e);
        }
    }
}
