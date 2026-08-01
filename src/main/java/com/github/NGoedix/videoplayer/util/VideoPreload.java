package com.github.NGoedix.videoplayer.util;

import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Client-side cache of videos downloaded ahead of time so playback starts instantly.
 *
 * <p>The server sends {@code preload;<url>} on the {@code crown:play} channel (the plugin's
 * {@code /preloadvideo} command); each client then downloads the video and audio streams via
 * {@link YtDlp} into {@code <gamedir>/videoplayer/preload/}. When a play command later arrives
 * with the <b>same url</b>, {@link YoutubeResolver} finds the entry here and plays the local
 * files instead of resolving + streaming, so everyone's video starts at the same moment.</p>
 *
 * <p>Entries are keyed by the exact (trimmed) url string. The cache directory is wiped on client
 * start; entries live for the session. A play command that arrives while the download is still
 * running simply streams as usual.</p>
 */
public final class VideoPreload {

    /** A fully downloaded video: {@code audio} is null when {@code video} already contains sound. */
    public record Entry(Path video, Path audio) {}

    // Video-only stream (audio is played from the separate file); H.264 preferred for VLC.
    private static final String VIDEO_ONLY_FORMAT = "bv*[vcodec^=avc1][height<=1080]/bv*[height<=1080]/b";
    // Best muxed stream — used when no separate audio stream exists, so the video is never silent.
    private static final String MUXED_FORMAT = "b";
    private static final String AUDIO_ONLY_FORMAT = "ba[ext=m4a]/ba";

    private static final Map<String, Entry> READY = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VideoPlayer-Preloader");
        t.setDaemon(true);
        return t;
    });

    private VideoPreload() {}

    /** Clears leftovers from previous sessions (preloaded files are only valid for one session). */
    public static void init() {
        EXECUTOR.execute(() -> {
            Path root = root();
            if (!Files.exists(root)) return;
            try (Stream<Path> files = Files.walk(root)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            } catch (IOException e) {
                Reference.LOGGER.warn("Could not clear video preload cache: {}", e.toString());
            }
        });
    }

    /** Starts downloading {@code url} in the background; no-op if already preloaded or in progress. */
    public static void preload(String url) {
        if (url == null || url.isBlank()) return;
        String key = url.trim();
        if (READY.containsKey(key) || !IN_FLIGHT.add(key)) return;

        EXECUTOR.execute(() -> {
            try {
                Reference.LOGGER.info("Preloading video: {}", key);
                Path dir = root().resolve(hash(key));

                Path audio = null;
                try {
                    audio = YtDlp.download(key, AUDIO_ONLY_FORMAT, dir, "audio");
                } catch (Exception e) {
                    Reference.LOGGER.info("No separate audio stream for '{}', using a muxed video", key);
                }
                Path video = YtDlp.download(key, audio != null ? VIDEO_ONLY_FORMAT : MUXED_FORMAT, dir, "video");

                READY.put(key, new Entry(video, audio));
                Reference.LOGGER.info("Preloaded video ready: {}", key);
                PacketHandler.sendC2SPlayBody("preloaded;" + key);
            } catch (Exception e) {
                Reference.LOGGER.error("Failed to preload video '{}'", key, e);
                PacketHandler.sendC2SPlayBody("preloadfail;" + key);
            } finally {
                IN_FLIGHT.remove(key);
            }
        });
    }

    /** Returns the completed preload for {@code url}, or {@code null} if none (still downloading counts as none). */
    public static Entry get(String url) {
        return url == null ? null : READY.get(url.trim());
    }

    private static Path root() {
        return FabricLoader.getInstance().getGameDir().resolve("videoplayer").resolve("preload");
    }

    private static String hash(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(url.hashCode());
        }
    }
}
