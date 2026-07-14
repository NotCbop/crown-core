package com.github.NGoedix.videoplayer.client;

import com.github.NGoedix.videoplayer.Reference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Environment(EnvType.CLIENT)
public final class VideoToggle {

    private static final String KEY = "showVideos";

    private static boolean enabled = true;
    private static Path file;

    private VideoToggle() {}

    public static void load() {
        file = FabricLoader.getInstance().getConfigDir().resolve("crown-video.properties");
        if (!Files.exists(file)) return;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
            enabled = !"false".equalsIgnoreCase(props.getProperty(KEY, "true"));
        } catch (IOException e) {
            Reference.LOGGER.warn("Could not read " + file + ": " + e);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            ClientHandler.stopVideoIfExists(Minecraft.getInstance());
        }
        save();
    }

    private static void save() {
        if (file == null) return;

        Properties props = new Properties();
        props.setProperty(KEY, Boolean.toString(enabled));
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "Crown Championship Utilities");
        } catch (IOException e) {
            Reference.LOGGER.warn("Could not write " + file + ": " + e);
        }
    }
}
